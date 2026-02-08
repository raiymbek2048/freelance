package kg.freelance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kg.freelance.dto.request.PasswordChangeRequest;
import kg.freelance.dto.request.UserUpdateRequest;
import kg.freelance.dto.response.UserResponse;
import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();

        userPrincipal = UserPrincipal.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .active(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    private UserResponse buildUserResponse() {
        return UserResponse.builder()
                .id(1L)
                .email("test@example.com")
                .fullName("Test User")
                .role(UserRole.USER)
                .emailVerified(true)
                .build();
    }

    @Nested
    @DisplayName("GET /api/v1/users/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current user")
        void shouldReturnCurrentUser() throws Exception {
            when(userService.getCurrentUser(1L)).thenReturn(buildUserResponse());

            mockMvc.perform(get("/api/v1/users/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.fullName").value("Test User"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me")
    class UpdateCurrentUserTests {

        @Test
        @DisplayName("Should update current user")
        void shouldUpdateCurrentUser() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setFullName("Updated Name");

            UserResponse updated = UserResponse.builder()
                    .id(1L)
                    .email("test@example.com")
                    .fullName("Updated Name")
                    .build();

            when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(updated);

            mockMvc.perform(put("/api/v1/users/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.fullName").value("Updated Name"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/password")
    class ChangePasswordTests {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest();
            request.setCurrentPassword("oldPass123");
            request.setNewPassword("newPass456");

            doNothing().when(userService).updatePassword(1L, "oldPass123", "newPass456");

            mockMvc.perform(put("/api/v1/users/me/password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(userService).updatePassword(1L, "oldPass123", "newPass456");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user by ID")
        void shouldReturnUserById() throws Exception {
            when(userService.getCurrentUser(5L)).thenReturn(
                    UserResponse.builder().id(5L).email("other@example.com").fullName("Other User").build());

            mockMvc.perform(get("/api/v1/users/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5))
                    .andExpect(jsonPath("$.fullName").value("Other User"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void shouldReturn404WhenUserNotFound() throws Exception {
            when(userService.getCurrentUser(999L))
                    .thenThrow(new ResourceNotFoundException("User", "id", 999L));

            mockMvc.perform(get("/api/v1/users/999"))
                    .andExpect(status().isNotFound());
        }
    }
}
