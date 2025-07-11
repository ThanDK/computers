package in.project.computers.entity.order;

public enum PaymentStatus {
    PENDING,          // Waiting for user to initiate/complete payment.
    PENDING_APPROVAL, // Bank transfer slip submitted, waiting for admin to verify.
    COMPLETED,        // Payment successful and verified.
    FAILED,           // Payment attempt failed.
    REFUNDED,         // Payment was refunded.
    REJECTED          // Bank transfer slip was rejected by an admin.
}