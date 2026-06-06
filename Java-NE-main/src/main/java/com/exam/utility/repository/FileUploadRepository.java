package com.exam.utility.repository;

import com.exam.utility.entity.FileUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    List<FileUpload> findByEntityTypeAndEntityIdAndDeletedFalse(String entityType, String entityId);
    Page<FileUpload> findByDeletedFalse(Pageable pageable);
}
