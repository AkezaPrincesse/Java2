package com.exam.utility.repository;

import com.exam.utility.entity.Notification;
import com.exam.utility.enums.NotificationStatus;
import com.exam.utility.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Page<Notification> findByUserId(Long userId, Pageable pageable);
    Page<Notification> findByUserIdAndStatus(Long userId, NotificationStatus status, Pageable pageable);
    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ' WHERE n.user.id = :userId AND n.status = 'UNREAD'")
    void markAllAsRead(Long userId);

    Page<Notification> findByCustomerId(Long customerId, Pageable pageable);
    Page<Notification> findByType(NotificationType type, Pageable pageable);
}
