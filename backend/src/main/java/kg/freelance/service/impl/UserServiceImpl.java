package kg.freelance.service.impl;

import kg.freelance.dto.request.UserUpdateRequest;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.entity.User;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.UserRepository;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserPrincipal loadUserById(Long id) {
        User user = userRepository.findById(id)
                .orElse(null);
        return user != null ? UserPrincipal.fromUser(user) : null;
    }

    @Override
    public UserPrincipal loadUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElse(null);
        return user != null ? UserPrincipal.fromUser(user) : null;
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = findById(userId);
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = findById(userId);

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            if (!request.getPhone().equals(user.getPhone()) && existsByPhone(request.getPhone())) {
                throw new BadRequestException("Phone number already in use");
            }
            user.setPhone(request.getPhone());
            user.setPhoneVerified(false);
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getWhatsappLink() != null) {
            user.setWhatsappLink(request.getWhatsappLink());
        }
        if (request.getProfileVisibility() != null) {
            user.setProfileVisibility(request.getProfileVisibility());
        }
        if (request.getHideFromExecutorList() != null) {
            user.setHideFromExecutorList(request.getHideFromExecutorList());
        }

        user = userRepository.save(user);
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userRepository.existsByPhone(phone);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
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
    }
}
