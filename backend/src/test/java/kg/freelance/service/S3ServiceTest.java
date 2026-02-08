package kg.freelance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3Service Tests")
class S3ServiceTest {

    @Nested
    @DisplayName("Without S3 Client (Local Storage)")
    class LocalStorageTests {

        @Test
        @DisplayName("Should report not configured when S3 client is null")
        void shouldReportNotConfiguredWhenS3ClientNull() {
            // Given
            S3Service service = new S3Service(null, null);

            // When/Then
            assertThat(service.isConfigured()).isFalse();
        }

        @Test
        @DisplayName("Should save file locally when S3 not configured")
        void shouldSaveFileLocallyWhenS3NotConfigured(@TempDir Path tempDir) throws IOException {
            // Given
            S3Service service = new S3Service(null, null);
            ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
            ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");

            MultipartFile file = new MockMultipartFile(
                    "test.jpg",
                    "test.jpg",
                    "image/jpeg",
                    "test content".getBytes()
            );

            // When
            String url = service.uploadFile(file, "avatars");

            // Then
            assertThat(url).startsWith("http://localhost:8080/api/v1/files/avatars/");
            assertThat(url).endsWith(".jpg");

            // Verify file was actually saved
            Path avatarsDir = tempDir.resolve("avatars");
            assertThat(Files.exists(avatarsDir)).isTrue();
            long fileCount = Files.list(avatarsDir).count();
            assertThat(fileCount).isEqualTo(1);
        }

        @Test
        @DisplayName("Should save file locally with no extension")
        void shouldSaveFileLocallyWithNoExtension(@TempDir Path tempDir) throws IOException {
            // Given
            S3Service service = new S3Service(null, null);
            ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
            ReflectionTestUtils.setField(service, "baseUrl", "http://localhost:8080");

            MultipartFile file = new MockMultipartFile(
                    "noext",
                    "noext",
                    "application/octet-stream",
                    "test content".getBytes()
            );

            // When
            String url = service.uploadFile(file, "docs");

            // Then
            assertThat(url).startsWith("http://localhost:8080/api/v1/files/docs/");
            assertThat(url).doesNotContain(".");
        }

        @Test
        @DisplayName("Should return original URL for presigned URL in dev mode")
        void shouldReturnOriginalUrlForPresignedUrlInDevMode() {
            // Given
            S3Service service = new S3Service(null, null);
            String key = "http://localhost:8080/api/v1/files/avatars/test.jpg";

            // When
            String result = service.getPresignedUrl(key, Duration.ofMinutes(15));

            // Then
            assertThat(result).isEqualTo(key);
        }

        @Test
        @DisplayName("Should get local file path")
        void shouldGetLocalFilePath() {
            // Given
            S3Service service = new S3Service(null, null);
            ReflectionTestUtils.setField(service, "uploadDir", "/app/uploads");

            // When
            Path path = service.getLocalFilePath("avatars", "test.jpg");

            // Then
            assertThat(path.toString()).isEqualTo("/app/uploads/avatars/test.jpg");
        }
    }

    @Nested
    @DisplayName("With S3 Client")
    class S3StorageTests {

        @Test
        @DisplayName("Should report configured when S3 client is present")
        void shouldReportConfiguredWhenS3ClientPresent() {
            // Given
            S3Client s3Client = mock(S3Client.class);
            S3Service service = new S3Service(s3Client, null);

            // When/Then
            assertThat(service.isConfigured()).isTrue();
        }

        @Test
        @DisplayName("Should upload file to S3")
        void shouldUploadFileToS3() throws IOException {
            // Given
            S3Client s3Client = mock(S3Client.class);
            S3Service service = new S3Service(s3Client, null);
            ReflectionTestUtils.setField(service, "bucket", "test-bucket");
            ReflectionTestUtils.setField(service, "region", "us-east-1");

            when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                    .thenReturn(PutObjectResponse.builder().build());

            MultipartFile file = new MockMultipartFile(
                    "test.jpg",
                    "test.jpg",
                    "image/jpeg",
                    "test content".getBytes()
            );

            // When
            String url = service.uploadFile(file, "avatars");

            // Then
            assertThat(url).startsWith("https://test-bucket.s3.us-east-1.amazonaws.com/avatars/");
            assertThat(url).endsWith("-test.jpg");
            verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        }

        @Test
        @DisplayName("Should generate presigned URL when presigner is configured")
        void shouldGeneratePresignedUrlWhenPresignerConfigured() throws Exception {
            // Given
            S3Client s3Client = mock(S3Client.class);
            S3Presigner s3Presigner = mock(S3Presigner.class);
            S3Service service = new S3Service(s3Client, s3Presigner);
            ReflectionTestUtils.setField(service, "bucket", "test-bucket");

            PresignedGetObjectRequest presignedRequest = mock(PresignedGetObjectRequest.class);
            when(presignedRequest.url()).thenReturn(new URL("https://test-bucket.s3.amazonaws.com/key?signed=true"));
            when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
                    .thenReturn(presignedRequest);

            // When
            String url = service.getPresignedUrl("avatars/test.jpg", Duration.ofMinutes(15));

            // Then
            assertThat(url).contains("test-bucket");
            verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
        }
    }
}
