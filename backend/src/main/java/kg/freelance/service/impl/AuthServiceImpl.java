package kg.freelance.service.impl;

import kg.freelance.dto.request.LoginRequest;
import kg.freelance.dto.request.RefreshTokenRequest;
import kg.freelance.dto.request.RegisterRequest;
import kg.freelance.dto.response.AuthResponse;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.entity.ExecutorProfile;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.UnauthorizedException;
import kg.freelance.repository.ExecutorProfileRepository;
import kg.freelance.repository.UserRepository;
import kg.freelance.security.UserPrincipal;
import kg.freelance.security.jwt.JwtTokenProvider;
import kg.freelance.service.AuthService;
import kg.freelance.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final ExecutorProfileRepository executorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number is already registered");
        }

        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .role(UserRole.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .hideFromExecutorList(false)
                .emailVerified(false)
                .phoneVerified(false)
                .active(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        // Create executor profile automatically for all users
        ExecutorProfile executorProfile = new ExecutorProfile();
        executorProfile.setUser(user);
        executorProfile.setTotalOrders(0);
        executorProfile.setCompletedOrders(0);
        executorProfile.setDisputedOrders(0);
        executorProfile.setAvgCompletionDays(0.0);
        executorProfile.setRating(java.math.BigDecimal.ZERO);
        executorProfile.setReviewCount(0);
        executorProfile.setAvailableForWork(true);
        executorProfileRepository.save(executorProfile);

        // Set bidirectional relationship so hasExecutorProfile returns true
        user.setExecutorProfile(executorProfile);

        // Send welcome email
        emailService.sendWelcomeEmail(user);

        return generateAuthResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            User user = userRepository.findById(userPrincipal.getId())
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            log.info("User logged in: {}", user.getEmail());
            return generateAuthResponse(user);

        } catch (AuthenticationException e) {
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!user.getActive()) {
            throw new UnauthorizedException("User account is disabled");
        }

        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        UserPrincipal userPrincipal = UserPrincipal.fromUser(user);

        String accessToken = jwtTokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .whatsappLink(user.getWhatsappLink())
                .profileVisibility(user.getProfileVisibility())
                .hideFromExecutorList(user.getHideFromExecutorList())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .executorVerified(user.getExecutorVerified())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .hasExecutorProfile(user.getExecutorProfile() != null)
                .build();

        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationMs(),
                userResponse
        );
    }
}
