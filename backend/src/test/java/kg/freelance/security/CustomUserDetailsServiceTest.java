package kg.freelance.security;

import kg.freelance.entity.User;
import kg.freelance.entity.enums.AuthProvider;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User buildUser() {
        return User.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("hashed-password")
                .fullName("Test User")
                .role(UserRole.USER)
                .active(true)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    @Test
    @DisplayName("Should load user by email")
    void shouldLoadUserByEmail() {
        User user = buildUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("test@example.com");
        assertThat(details.getPassword()).isEqualTo("hashed-password");
        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Should convert email to lowercase")
    void shouldConvertEmailToLowercase() {
        User user = buildUser();
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("Test@Example.COM");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("unknown@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown@example.com");
    }

    @Test
    @DisplayName("Should return UserPrincipal with correct role")
    void shouldReturnPrincipalWithCorrectRole() {
        User admin = buildUser();
        admin.setRole(UserRole.ADMIN);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(admin));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertThat(details.getAuthorities()).hasSize(1);
        assertThat(details.getAuthorities().iterator().next().getAuthority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should return disabled UserPrincipal for blocked user")
    void shouldReturnDisabledPrincipalForBlockedUser() {
        User blocked = buildUser();
        blocked.setActive(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(blocked));

        UserDetails details = service.loadUserByUsername("test@example.com");

        assertThat(details.isEnabled()).isFalse();
        assertThat(details.isAccountNonLocked()).isFalse();
    }
}
