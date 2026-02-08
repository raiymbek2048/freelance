package kg.freelance.service.impl;

import kg.freelance.dto.request.UserUpdateRequest;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.entity.User;
import kg.freelance.entity.enums.ProfileVisibility;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.repository.UserRepository;
import kg.freelance.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashedPassword")
                .fullName("Test User")
                .phone("+996700123456")
                .role(UserRole.USER)
                .profileVisibility(ProfileVisibility.PUBLIC)
                .hideFromExecutorList(false)
                .emailVerified(false)
                .phoneVerified(false)
                .executorVerified(false)
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Load User Tests")
    class LoadUserTests {

        @Test
        @DisplayName("Should load user by ID successfully")
        void shouldLoadUserByIdSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserPrincipal result = userService.loadUserById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should return null when user not found by ID")
        void shouldReturnNullWhenUserNotFoundById() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When
            UserPrincipal result = userService.loadUserById(999L);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should load user by email successfully")
        void shouldLoadUserByEmailSuccessfully() {
            // Given
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            UserPrincipal result = userService.loadUserByEmail("test@example.com");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should return null when user not found by email")
        void shouldReturnNullWhenUserNotFoundByEmail() {
            // Given
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // When
            UserPrincipal result = userService.loadUserByEmail("notfound@example.com");

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("Find User Tests")
    class FindUserTests {

        @Test
        @DisplayName("Should find user by ID")
        void shouldFindUserById() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            User result = userService.findById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw exception when user not found by ID")
        void shouldThrowExceptionWhenUserNotFoundById() {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.findById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Should find user by email")
        void shouldFindUserByEmail() {
            // Given
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            // When
            User result = userService.findByEmail("test@example.com");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEmail()).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should throw exception when user not found by email")
        void shouldThrowExceptionWhenUserNotFoundByEmail() {
            // Given
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> userService.findByEmail("notfound@example.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Current User Tests")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user response")
        void shouldReturnCurrentUserResponse() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When
            UserResponse result = userService.getCurrentUser(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getEmail()).isEqualTo("test@example.com");
            assertThat(result.getFullName()).isEqualTo("Test User");
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update user full name")
        void shouldUpdateUserFullName() {
            // Given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setFullName("Updated Name");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserResponse result = userService.updateUser(1L, request);

            // Then
            assertThat(result.getFullName()).isEqualTo("Updated Name");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should update user phone")
        void shouldUpdateUserPhone() {
            // Given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setPhone("+996700999888");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByPhone("+996700999888")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserResponse result = userService.updateUser(1L, request);

            // Then
            assertThat(result.getPhoneVerified()).isFalse();
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception when phone already exists")
        void shouldThrowExceptionWhenPhoneAlreadyExists() {
            // Given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setPhone("+996700999888");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.existsByPhone("+996700999888")).thenReturn(true);

            // When/Then
            assertThatThrownBy(() -> userService.updateUser(1L, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Phone number already in use");
        }

        @Test
        @DisplayName("Should update profile visibility")
        void shouldUpdateProfileVisibility() {
            // Given
            UserUpdateRequest request = new UserUpdateRequest();
            request.setProfileVisibility(ProfileVisibility.PRIVATE);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            UserResponse result = userService.updateUser(1L, request);

            // Then
            assertThat(result.getProfileVisibility()).isEqualTo(ProfileVisibility.PRIVATE);
        }
    }

    @Nested
    @DisplayName("Update Password Tests")
    class UpdatePasswordTests {

        @Test
        @DisplayName("Should update password successfully")
        void shouldUpdatePasswordSuccessfully() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("oldPassword", "hashedPassword")).thenReturn(true);
            when(passwordEncoder.encode("newPassword")).thenReturn("newHashedPassword");

            // When
            userService.updatePassword(1L, "oldPassword", "newPassword");

            // Then
            verify(userRepository).save(argThat(user ->
                    user.getPasswordHash().equals("newHashedPassword")
            ));
        }

        @Test
        @DisplayName("Should throw exception when old password is incorrect")
        void shouldThrowExceptionWhenOldPasswordIncorrect() {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

            // When/Then
            assertThatThrownBy(() -> userService.updatePassword(1L, "wrongPassword", "newPassword"))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Current password is incorrect");
        }
    }

    @Nested
    @DisplayName("Existence Check Tests")
    class ExistenceCheckTests {

        @Test
        @DisplayName("Should check email existence")
        void shouldCheckEmailExistence() {
            // Given
            when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            // When/Then
            assertThat(userService.existsByEmail("test@example.com")).isTrue();
            assertThat(userService.existsByEmail("new@example.com")).isFalse();
        }

        @Test
        @DisplayName("Should check phone existence")
        void shouldCheckPhoneExistence() {
            // Given
            when(userRepository.existsByPhone("+996700123456")).thenReturn(true);
            when(userRepository.existsByPhone("+996700999999")).thenReturn(false);

            // When/Then
            assertThat(userService.existsByPhone("+996700123456")).isTrue();
            assertThat(userService.existsByPhone("+996700999999")).isFalse();
        }
    }
}
