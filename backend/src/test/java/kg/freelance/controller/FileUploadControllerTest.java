package kg.freelance.controller;

import kg.freelance.entity.enums.UserRole;
import kg.freelance.exception.GlobalExceptionHandler;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Path;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileUploadController Tests")
class FileUploadControllerTest {

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private FileUploadController controller;

    private MockMvc mockMvc;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        userPrincipal = UserPrincipal.builder()
                .id(1L).email("test@example.com").fullName("Test User")
                .role(UserRole.USER).active(true).build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities()));
    }

    @Nested
    @DisplayName("POST /api/v1/files/upload")
    class UploadFileTests {

        @Test
        @DisplayName("Should upload file successfully")
        void shouldUploadFileSuccessfully() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "test.jpg", "image/jpeg", "fake-image-data".getBytes());

            when(s3Service.uploadFile(any(), eq("general/1"))).thenReturn("https://s3.example.com/general/1/test.jpg");

            mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://s3.example.com/general/1/test.jpg"));
        }

        @Test
        @DisplayName("Should upload with custom folder")
        void shouldUploadWithCustomFolder() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "avatar.png", "image/png", "fake-image-data".getBytes());

            when(s3Service.uploadFile(any(), eq("avatars/1"))).thenReturn("https://s3.example.com/avatars/1/avatar.png");

            mockMvc.perform(multipart("/api/v1/files/upload").file(file).param("folder", "avatars"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://s3.example.com/avatars/1/avatar.png"));
        }

        @Test
        @DisplayName("Should return 400 for empty file")
        void shouldReturn400ForEmptyFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.jpg", "image/jpeg", new byte[0]);

            mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for file too large")
        void shouldReturn400ForFileTooLarge() throws Exception {
            byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
            MockMultipartFile file = new MockMultipartFile(
                    "file", "large.jpg", "image/jpeg", largeContent);

            mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for invalid content type")
        void shouldReturn400ForInvalidContentType() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", "fake-pdf".getBytes());

            mockMvc.perform(multipart("/api/v1/files/upload").file(file))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/files/upload/verification")
    class UploadVerificationDocumentTests {

        @Test
        @DisplayName("Should upload passport")
        void shouldUploadPassport() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "passport.jpg", "image/jpeg", "fake-image-data".getBytes());

            when(s3Service.uploadFile(any(), eq("verifications/1")))
                    .thenReturn("https://s3.example.com/verifications/1/passport.jpg");

            mockMvc.perform(multipart("/api/v1/files/upload/verification")
                            .file(file).param("type", "passport"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").value("https://s3.example.com/verifications/1/passport.jpg"));
        }

        @Test
        @DisplayName("Should upload selfie")
        void shouldUploadSelfie() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "selfie.png", "image/png", "fake-image-data".getBytes());

            when(s3Service.uploadFile(any(), eq("verifications/1")))
                    .thenReturn("https://s3.example.com/verifications/1/selfie.png");

            mockMvc.perform(multipart("/api/v1/files/upload/verification")
                            .file(file).param("type", "selfie"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.url").exists());
        }

        @Test
        @DisplayName("Should return 400 for invalid type")
        void shouldReturn400ForInvalidType() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "doc.jpg", "image/jpeg", "fake-image-data".getBytes());

            mockMvc.perform(multipart("/api/v1/files/upload/verification")
                            .file(file).param("type", "other"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for empty verification file")
        void shouldReturn400ForEmptyVerificationFile() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "passport.jpg", "image/jpeg", new byte[0]);

            mockMvc.perform(multipart("/api/v1/files/upload/verification")
                            .file(file).param("type", "passport"))
                    .andExpect(status().isBadRequest());
        }
    }
}
