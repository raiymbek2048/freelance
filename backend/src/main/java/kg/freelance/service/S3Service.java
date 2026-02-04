package kg.freelance.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket:freelance-kg}")
    private String bucket;

    @Value("${aws.s3.region:eu-central-1}")
    private String region;

    @Value("${app.upload.dir:/app/uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Autowired
    public S3Service(@Autowired(required = false) S3Client s3Client,
                     @Autowired(required = false) S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        if (s3Client == null) {
            log.warn("S3 not configured - file uploads will use local storage");
        }
    }

    public boolean isConfigured() {
        return s3Client != null;
    }

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (s3Client == null) {
            // Development mode without S3 - save locally
            return saveLocally(file, folder);
        }

        String key = folder + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
    }

    private String saveLocally(MultipartFile file, String folder) throws IOException {
        // Create directory if not exists
        Path dirPath = Paths.get(uploadDir, folder);
        Files.createDirectories(dirPath);

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = dirPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File saved locally: {}", filePath);

        // Return URL to access the file
        return baseUrl + "/api/v1/files/" + folder + "/" + filename;
    }

    public Path getLocalFilePath(String folder, String filename) {
        return Paths.get(uploadDir, folder, filename);
    }

    public String getPresignedUrl(String key, Duration duration) {
        if (s3Presigner == null) {
            return key; // Return original URL in dev mode
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(r -> r.bucket(bucket).key(key))
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}
