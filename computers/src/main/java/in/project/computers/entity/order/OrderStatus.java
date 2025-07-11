package in.project.computers.entity.order;

public enum OrderStatus {
    PENDING_PAYMENT,
    PROCESSING,
    SHIPPED, // <-- เพิ่มสถานะนี้เข้าไป
    COMPLETED,
    CANCELLED,
    REFUND_REQUESTED,
    REFUND_APPROVED,
    REFUND_REJECTED,
    REFUNDED
}