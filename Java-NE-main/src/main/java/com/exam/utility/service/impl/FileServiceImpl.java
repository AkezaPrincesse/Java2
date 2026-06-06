package com.exam.utility.service.impl;

import com.exam.utility.entity.FileUpload;
import com.exam.utility.exception.FileException;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.FileUploadRepository;
import com.exam.utility.service.FileService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Secure file storage with MIME type validation, extension blocking, and size enforcement.
 *
 * Files are stored on disk under the configured upload directory.
 * Stored file names are UUID-based to prevent directory traversal and name collision.
 * Original names and content types are persisted in the database for display/audit purposes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceImpl implements FileService {

    private final FileUploadRepository fileUploadRepository;

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    // Allowed MIME types — any content type not in this set is rejected
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "image/jpeg",
        "image/jpg",
        "image/png",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    // Forbidden extensions — dangerous executables and scripts must never be stored
    private static final Set<String> FORBIDDEN_EXTENSIONS = Set.of(
        ".exe", ".bat", ".cmd", ".sh", ".msi", ".ps1", ".vbs", ".js", ".jar"
    );

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create upload directory: " + uploadDir, e);
        }
    }

    @Override
    @Transactional
    public FileUpload upload(MultipartFile file, String entityType, String entityId) {
        if (file == null || file.isEmpty()) {
            throw new FileException("File must not be empty");
        }

        validateFileSize(file);
        validateContentType(file);
        validateExtension(file);

        String originalName = file.getOriginalFilename() != null
            ? file.getOriginalFilename() : "unknown";
        String extension = getExtension(originalName);
        String storedName = UUID.randomUUID() + extension;
        Path destination = Paths.get(uploadDir).resolve(storedName);

        try {
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file {}: {}", originalName, e.getMessage());
            throw new FileException("Failed to store file. Please try again.");
        }

        String uploadedBy = SecurityContextHolder.getContext().getAuthentication() != null
            ? SecurityContextHolder.getContext().getAuthentication().getName()
            : "system";

        FileUpload fileUpload = FileUpload.builder()
            .fileName(storedName)
            .originalName(originalName)
            .contentType(file.getContentType())
            .fileSize(file.getSize())
            .filePath(destination.toString())
            .entityType(entityType)
            .entityId(entityId)
            .deleted(false)
            .build();

        fileUpload = fileUploadRepository.save(fileUpload);
        log.info("File uploaded: {} -> {} by {}", originalName, storedName, uploadedBy);
        return fileUpload;
    }

    @Override
    @Transactional(readOnly = true)
    public Resource download(Long fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        if (fileUpload.isDeleted()) {
            throw new ResourceNotFoundException("File", "id", fileId);
        }

        try {
            Path filePath = Paths.get(fileUpload.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new FileException("File not found or not readable: " + fileUpload.getOriginalName());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new FileException("Could not read file: " + fileUpload.getOriginalName());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<FileUpload> getByEntity(String entityType, String entityId) {
        return fileUploadRepository.findByEntityTypeAndEntityIdAndDeletedFalse(entityType, entityId);
    }

    @Override
    @Transactional
    public void delete(Long fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
            .orElseThrow(() -> new ResourceNotFoundException("File", "id", fileId));

        // Soft delete — physical file preserved for audit/recovery; marked deleted in DB
        fileUpload.setDeleted(true);
        fileUploadRepository.save(fileUpload);
        log.info("File soft-deleted: id={}, name={}", fileId, fileUpload.getOriginalName());
    }

    private void validateFileSize(MultipartFile file) {
        if (file.getSize() > maxFileSize) {
            throw new FileException(String.format(
                "File size %.2f MB exceeds the maximum allowed size of %.0f MB",
                file.getSize() / (1024.0 * 1024.0),
                maxFileSize / (1024.0 * 1024.0)
            ));
        }
    }

    private void validateContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new FileException(
                "File type '" + contentType + "' is not allowed. " +
                "Accepted types: PDF, JPG, JPEG, PNG, DOC, DOCX"
            );
        }
    }

    private void validateExtension(MultipartFile file) {
        String name = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        for (String forbidden : FORBIDDEN_EXTENSIONS) {
            if (name.endsWith(forbidden)) {
                throw new FileException("File extension '" + forbidden + "' is not permitted for security reasons.");
            }
        }
    }

    private String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex).toLowerCase() : "";
    }
}
