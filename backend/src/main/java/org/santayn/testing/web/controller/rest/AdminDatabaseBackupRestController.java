package org.santayn.testing.web.controller.rest;

import org.santayn.testing.service.DatabaseBackupService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/database-backups")
public class AdminDatabaseBackupRestController {

    private final DatabaseBackupService databaseBackupService;

    public AdminDatabaseBackupRestController(DatabaseBackupService databaseBackupService) {
        this.databaseBackupService = databaseBackupService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public ResponseEntity<StreamingResponseBody> createBackup() {
        DatabaseBackupService.DatabaseBackup backup = databaseBackupService.createFullBackup();
        StreamingResponseBody body = outputStream -> {
            try (InputStream inputStream = Files.newInputStream(backup.path())) {
                inputStream.transferTo(outputStream);
            } finally {
                backup.deleteQuietly();
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(backup.fileName(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentType(MediaType.parseMediaType("application/sql"))
                .contentLength(backup.sizeBytes())
                .body(body);
    }

    @PostMapping(value = "/restore", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ADMIN')")
    public DatabaseBackupRestoreResponse restoreBackup(@RequestParam("file") MultipartFile file) {
        databaseBackupService.restoreBackup(file);
        return new DatabaseBackupRestoreResponse(
                "restored",
                file.getOriginalFilename(),
                file.getSize(),
                Instant.now()
        );
    }

    public record DatabaseBackupRestoreResponse(String status,
                                                String fileName,
                                                long sizeBytes,
                                                Instant restoredAtUtc) {
    }
}
