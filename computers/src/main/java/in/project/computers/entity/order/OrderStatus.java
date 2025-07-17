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
    REFUND_APPROVED,
    REFUND_REJECTED,
    REFUNDED,
    REJECTED_SLIP
}