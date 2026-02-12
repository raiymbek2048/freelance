package kg.freelance.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class DeepLinkController {

    @Value("${app.android.package:kg.freelance.freelance_kg}")
    private String androidPackage;

    @Value("${app.android.sha256:}")
    private String androidSha256;

    @Value("${app.ios.app-id:}")
    private String iosAppId;

    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<Map<String, Object>>> androidAssetLinks() {
        Map<String, Object> target = Map.of(
                "namespace", "android_app",
                "package_name", androidPackage,
                "sha256_cert_fingerprints", androidSha256.isBlank() ? List.of() : List.of(androidSha256)
        );
        Map<String, Object> entry = Map.of(
                "relation", List.of("delegate_permission/common.handle_all_urls"),
                "target", target
        );
        return ResponseEntity.ok(List.of(entry));
    }

    @GetMapping(value = "/.well-known/apple-app-site-association", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> appleAppSiteAssociation() {
        Map<String, Object> detail = Map.of(
                "appID", iosAppId.isBlank() ? "TEAMID.kg.freelance.freelanceKg" : iosAppId,
                "paths", List.of("/orders/*", "/executors/*", "/chats/*")
        );
        Map<String, Object> applinks = Map.of(
                "apps", List.of(),
                "details", List.of(detail)
        );
        return ResponseEntity.ok(Map.of("applinks", applinks));
    }
}
