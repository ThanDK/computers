package in.project.computers.entity.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PROCESSING,
    SHIPPED,
    COMPLETED,
    CANCELLED,
    DELIVERY_FAILED,
    RETURNED_TO_SENDER,
    REFUND_REQUESTED,
    // REFUND_APPROVED, <-- This status is no longer needed with the new flow
    REFUND_REJECTED,
    REFUNDED,
    REJECTED_SLIP
}