package in.project.computers.service.reportService;

import in.project.computers.DTO.report.*;

import java.time.Instant;
import java.util.List;

public interface ReportService {

    OrderSearchApiResponse searchOrders(OrderSearchRequest request);

    TopSellingProductsApiResponse generateTopSellingReport(Instant startDate, Instant endDate);

    LowStockReportApiResponse generateLowStockReport();

    List<String> getPaymentStatuses();

    List<String> getPaymentMethods();

    String generateOrderReportCsv(OrderSearchRequest request);

    String generateTopSellingReportCsv(Instant startDate, Instant endDate);

    String generateLowStockReportCsv();
}