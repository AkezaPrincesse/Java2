package com.exam.utility.service.impl;

import com.exam.utility.dto.response.PagedResponse;
import com.exam.utility.dto.response.notification.NotificationResponse;
import com.exam.utility.entity.*;
import com.exam.utility.enums.NotificationStatus;
import com.exam.utility.enums.NotificationType;
import com.exam.utility.exception.ResourceNotFoundException;
import com.exam.utility.repository.NotificationRepository;
import com.exam.utility.repository.UserRepository;
import com.exam.utility.service.EmailService;
import com.exam.utility.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Async("notificationExecutor")
    public void sendNotification(User user, Customer customer, NotificationType type,
                                  String title, String message) {
        Notification notification = Notification.builder()
            .user(user)
            .customer(customer)
            .type(type)
            .title(title)
            .message(message)
            .status(NotificationStatus.UNREAD)
            .build();
        notificationRepository.save(notification);
        log.debug("Notification saved: {} for user: {}", type, user != null ? user.getEmail() : "N/A");
    }

    @Override
    @Async("notificationExecutor")
    public void notifyBillGenerated(Bill bill) {
        Customer customer = bill.getCustomer();
        String title = "New Utility Bill – " + bill.getBillNumber();
        String message = String.format(
            "Dear %s, your utility bill %s for %s amounting to %s RWF has been generated. Due: %s.",
            customer.getFullName(), bill.getBillNumber(),
            bill.getBillingCycle().getBillingYear() + "/" + bill.getBillingCycle().getBillingMonth(),
            bill.getTotalAmount(), bill.getDueDate()
        );
        sendNotification(customer.getUser(), customer, NotificationType.BILL_GENERATED, title, message);
        if (customer.getUser() != null) {
            emailService.sendBillGeneratedEmail(
                customer.getEmail(), customer.getFullName(),
                bill.getBillNumber(), bill.getTotalAmount(), bill.getDueDate().toString()
            );
        }
    }

    @Override
    @Async("notificationExecutor")
    public void notifyBillApproved(Bill bill) {
        Customer customer = bill.getCustomer();
        String title = "Bill Approved – " + bill.getBillNumber();
        String message = String.format(
            "Dear %s, your bill %s amounting to %s RWF has been approved and is ready for payment.",
            customer.getFullName(), bill.getBillNumber(), bill.getTotalAmount()
        );
        sendNotification(customer.getUser(), customer, NotificationType.BILL_APPROVED, title, message);
        emailService.sendBillApprovedEmail(
            customer.getEmail(), customer.getFullName(),
            bill.getBillNumber(), bill.getTotalAmount()
        );
    }

    @Override
    @Async("notificationExecutor")
    public void notifyPaymentReceived(Payment payment) {
        Customer customer = payment.getCustomer();
        Bill bill = payment.getBill();
        String title = "Payment Confirmed – Receipt " + payment.getReceiptNumber();
        String message = String.format(
            "Dear %s, your payment of %s RWF for bill %s has been received. Receipt: %s.",
            customer.getFullName(), payment.getAmount(),
            bill.getBillNumber(), payment.getReceiptNumber()
        );
        sendNotification(customer.getUser(), customer, NotificationType.PAYMENT_RECEIVED, title, message);
        emailService.sendPaymentConfirmationEmail(
            customer.getEmail(), customer.getFullName(),
            payment.getReceiptNumber(), payment.getAmount(), bill.getBillNumber()
        );
    }

    @Override
    @Async("notificationExecutor")
    public void notifyOverdueBill(Bill bill) {
        Customer customer = bill.getCustomer();
        String title = "Overdue Bill Notice – " + bill.getBillNumber();
        String message = String.format(
            "Dear %s, bill %s amounting to %s RWF was due on %s and remains unpaid. " +
            "Penalties may apply. Please pay immediately.",
            customer.getFullName(), bill.getBillNumber(), bill.getBalanceAmount(), bill.getDueDate()
        );
        sendNotification(customer.getUser(), customer, NotificationType.OVERDUE_BILL, title, message);
        emailService.sendOverdueBillEmail(
            customer.getEmail(), customer.getFullName(),
            bill.getBillNumber(), bill.getBalanceAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getMyNotifications(Pageable pageable) {
        User user = getCurrentUser();
        Page<NotificationResponse> page = notificationRepository
            .findByUserId(user.getId(), pageable)
            .map(this::toResponse);
        return PagedResponse.of(page);
    }

    @Override
    public long getUnreadCount() {
        User user = getCurrentUser();
        return notificationRepository.countByUserIdAndStatus(user.getId(), NotificationStatus.UNREAD);
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        notificationRepository.markAllAsRead(user.getId());
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));
        notification.setStatus(NotificationStatus.READ);
        notificationRepository.save(notification);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
            .id(n.getId())
            .type(n.getType())
            .title(n.getTitle())
            .message(n.getMessage())
            .status(n.getStatus())
            .emailSent(n.isEmailSent())
            .referenceLink(n.getReferenceLink())
            .createdAt(n.getCreatedAt())
            .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }
}
