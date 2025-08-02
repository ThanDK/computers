package in.project.computers.DTO.dashboard;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal; // Import BigDecimal
import java.util.List;

@Data
@Builder
public class DashboardResponse {
    private Stats stats;
    private List<ChartData> revenueChartData;
    private List<ChartData> topSellingData;
    private List<RecentOrder> recentOrders;
    private List<LowStockProduct> lowStockProducts;

    @Data
    @Builder
    public static class Stats {
        private double totalRevenue;
        private double revenueChange;
        private long totalSales;
        private double salesChange;
        private long pendingOrders;
        private long alerts;
        private long products;
    }

    @Data
    @Builder
    public static class ChartData {
        private String name;
        private double value;
    }

    @Data
    @Builder
    public static class RecentOrder {
        private String id;
        private String customerName;
        private String orderStatus;
        private BigDecimal totalAmount;
    }
    @Data
    @Builder
    public static class LowStockProduct {
        private String id;
        private String name;
        private String mpn;
        private int stock;
    }
}