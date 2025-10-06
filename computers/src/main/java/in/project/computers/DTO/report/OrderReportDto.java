package in.project.computers.DTO.report;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class OrderReportDto {
    private String orderId;
    private Instant orderDate;
    private Instant lastUpdateDate;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private String orderStatus;
    private String paymentStatus;
}