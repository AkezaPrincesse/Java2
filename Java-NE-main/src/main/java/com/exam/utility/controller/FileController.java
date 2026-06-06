package com.exam.utility.controller;

import com.exam.utility.dto.response.ApiResponse;
import com.exam.utility.entity.FileUpload;
import com.exam.utility.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Manages file uploads and downloads for the billing system.
 *
 * Security rules:
 * - Upload and delete require ADMIN or OPERATOR role.
 * - Download is available to all authenticated users.
 * - Allowed types: PDF, JPG, JPEG, PNG, DOC, DOCX. Max size: 10 MB.
 * - Dangerous extensions (.exe, .bat, .cmd, .sh, .msi) are always rejected.
 */
@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "File Management", description = "Upload, download, and manage documents")
public class FileController {

    private final FileService fileService;

    /**
     * Uploads a file and associates it with a specific entity (e.g., a customer or meter).
     * entityType examples: "CUSTOMER", "METER", "BILL". entityId is the entity's database ID.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_OPERATOR')")
    @Operation(summary = "Upload a file (PDF, JPG, PNG, DOC, DOCX — max 10 MB)")
    public ResponseEntity<ApiResponse<FileUpload>> upload(
        @RequestParam("file") MultipartFile file,
        @RequestParam(value = "entityType", required = false, defaultValue = "GENERAL") String entityType,
        @RequestParam(value = "entityId", required = false, defaultValue = "0") String entityId
    ) {
        FileUpload saved = fileService.upload(file, entityType, entityId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("File uploaded successfully", saved));
    }

    /**
     * Downloads a file by its database ID.
     * The Content-Disposition header prompts the browser to download rather than render the file.
     */
    @GetMapping("/{id}/download")
    @Operation(summary = "Download a file by ID")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = fileService.download(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + resource.getFilename() + "\"")
            .body(resource);
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @Operation(summary = "List all files linked to an entity")
    public ResponseEntity<ApiResponse<List<FileUpload>>> getByEntity(
        @PathVariable String entityType,
        @PathVariable String entityId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Files retrieved",
            fileService.getByEntity(entityType, entityId)));
    }

    /**
     * Soft-deletes a file. The physical file is retained for audit purposes;
     * only the database record is flagged as deleted.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @Operation(summary = "Soft-delete a file by ID")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        fileService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("File deleted successfully"));
    }
}
