package com.exam.utility.service;

import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.notification.NotificationResponse;
import com.exam.utility.entity.Bill;
import com.exam.utility.entity.Customer;
import com.exam.utility.entity.Payment;
import com.exam.utility.entity.User;
import com.exam.utility.enums.NotificationType;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    void sendNotification(User user, Customer customer, NotificationType type, String title, String message);
    void notifyBillGenerated(Bill bill);
    void notifyBillApproved(Bill bill);
    void notifyPaymentReceived(Payment payment);
    void notifyOverdueBill(Bill bill);
    PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable);
    long getUnreadCount();
    void markAllAsRead();
    void markAsRead(Long notificationId);
}
