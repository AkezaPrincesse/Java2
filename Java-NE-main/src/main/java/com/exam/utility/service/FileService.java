package com.exam.utility.service;

import com.exam.utility.entity.FileUpload;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Handles secure file upload, download, and management.
 *
 * Security rules enforced:
 * - Allowed MIME types: PDF, JPG/JPEG, PNG, DOC, DOCX
 * - Forbidden extensions: .exe, .bat, .cmd, .sh, .msi
 * - Maximum file size: 10 MB (enforced by Spring multipart config and here)
 * - Original file name and content type are stored; served file names are UUID-based to prevent path traversal.
 */
public interface FileService {

    /** Uploads a file, validates type and size, stores it, and returns the saved metadata. */
    FileUpload upload(MultipartFile file, String entityType, String entityId);

    /** Returns the stored file as a downloadable Resource. */
    Resource download(Long fileId);

    /** Lists all non-deleted files linked to a specific entity. */
    List<FileUpload> getByEntity(String entityType, String entityId);

    /** Soft-deletes a file record (sets deleted = true, does NOT remove the physical file immediately). */
    void delete(Long fileId);
}
