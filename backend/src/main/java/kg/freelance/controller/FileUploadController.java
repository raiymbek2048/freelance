package kg.freelance.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import kg.freelance.exception.BadRequestException;
import kg.freelance.exception.ResourceNotFoundException;
import kg.freelance.security.UserPrincipal;
import kg.freelance.service.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload endpoints")
public class FileUploadController {

    private final S3Service s3Service;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10MB

    @PostMapping("/upload")
    @Operation(summary = "Upload file", description = "Upload a file to S3")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> uploadFile(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "general") String folder) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed (10MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid file type. Allowed: JPEG, PNG, WebP");
        }

        // Use user ID in folder path for organization
        String uploadFolder = folder + "/" + user.getId();
        String url = s3Service.uploadFile(file, uploadFolder);

        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/upload/verification")
    @Operation(summary = "Upload verification document", description = "Upload passport or selfie for verification")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> uploadVerificationDocument(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file,
            @RequestParam String type) throws IOException {

        if (!type.equals("passport") && !type.equals("selfie")) {
            throw new BadRequestException("Type must be 'passport' or 'selfie'");
        }

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed (10MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException("Invalid file type. Allowed: JPEG, PNG, WebP");
        }

        String uploadFolder = "verifications/" + user.getId();
        String url = s3Service.uploadFile(file, uploadFolder);

        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/upload/evidence")
    @Operation(summary = "Upload dispute evidence", description = "Upload evidence file for a dispute (images + PDF)")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> uploadEvidence(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new BadRequestException("File size exceeds maximum allowed (10MB)");
        }

        Set<String> evidenceTypes = Set.of(
                "image/jpeg", "image/png", "image/webp", "application/pdf"
        );
        String contentType = file.getContentType();
        if (contentType == null || !evidenceTypes.contains(contentType)) {
            throw new BadRequestException("Invalid file type. Allowed: JPEG, PNG, WebP, PDF");
        }

        String uploadFolder = "evidence/" + user.getId();
        String url = s3Service.uploadFile(file, uploadFolder);

        return ResponseEntity.ok(Map.of("url", url));
    }

    @GetMapping("/{folder}/{subfolder}/{filename}")
    @Operation(summary = "Get file", description = "Get a file from local storage")
    public ResponseEntity<Resource> getFile(
            @PathVariable String folder,
            @PathVariable String subfolder,
            @PathVariable String filename) throws IOException {

        Path filePath = s3Service.getLocalFilePath(folder + "/" + subfolder, filename);

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("File", "path", folder + "/" + subfolder + "/" + filename);
        }

        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File", "path", folder + "/" + subfolder + "/" + filename);
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }

    @GetMapping("/{folder}/{filename}")
    @Operation(summary = "Get file (single folder)", description = "Get a file from local storage")
    public ResponseEntity<Resource> getFileSingleFolder(
            @PathVariable String folder,
            @PathVariable String filename) throws IOException {

        Path filePath = s3Service.getLocalFilePath(folder, filename);

        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("File", "path", folder + "/" + filename);
        }

        Resource resource;
        try {
            resource = new UrlResource(filePath.toUri());
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File", "path", folder + "/" + filename);
        }

        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
