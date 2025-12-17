package com.taskqueue.www.repository;

import com.taskqueue.www.model.GeneratedDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, Long> {

    Optional<GeneratedDocument> findByTaskId(Long taskId);

    List<GeneratedDocument> findAllByTaskId(Long taskId);

    Page<GeneratedDocument> findByDocumentType(String documentType, Pageable pageable);

    List<GeneratedDocument> findByCreatedAtBefore(LocalDateTime cutoffDate);

    long countByDocumentType(String documentType);
}