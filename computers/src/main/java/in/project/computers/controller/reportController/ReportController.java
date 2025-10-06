package in.project.computers.controller.reportController;

import in.project.computers.DTO.report.*;
import in.project.computers.service.reportService.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/orders/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> searchOrderReport(
            @RequestBody OrderSearchRequest request,
            @RequestParam(defaultValue = "json") String format) {

        Instant startDate = request.getStartDate() != null ? request.getStartDate() : Instant.EPOCH;
        Instant originalEndDate = request.getEndDate() != null ? request.getEndDate() : Instant.now();

        Instant adjustedEndDate = originalEndDate.atZone(ZoneOffset.UTC).plusDays(1).toInstant();

        request.setStartDate(startDate);
        request.setEndDate(adjustedEndDate);

        if ("csv".equalsIgnoreCase(format)) {
            String fileName = "order-report-" + LocalDate.ofInstant(startDate, ZoneOffset.UTC) + "-to-" + LocalDate.ofInstant(originalEndDate, ZoneOffset.UTC) + ".csv";
            String csvData = reportService.generateOrderReportCsv(request);
            return createCsvResponse(csvData, fileName);
        }

        OrderSearchApiResponse data = reportService.searchOrders(request);

        data.setStartDate(startDate);
        data.setEndDate(originalEndDate);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/products/top-selling")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTopSellingProductReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "json") String format) {

        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        if ("csv".equalsIgnoreCase(format)) {
            String fileName = "top-selling-report-" + startDate + "-to-" + endDate + ".csv";
            String csvData = reportService.generateTopSellingReportCsv(startInstant, endInstant);
            return createCsvResponse(csvData, fileName);
        }

        TopSellingProductsApiResponse data = reportService.generateTopSellingReport(startInstant, endInstant);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/stock/low")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getLowStockReport(
            @RequestParam(defaultValue = "json") String format) {

        if ("csv".equalsIgnoreCase(format)) {
            String fileName = "low-stock-report-" + LocalDate.now() + ".csv";
            String csvData = reportService.generateLowStockReportCsv();
            return createCsvResponse(csvData, fileName);
        }

        LowStockReportApiResponse data = reportService.generateLowStockReport();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/lookups/payment-statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getPaymentStatuses() {
        log.info("Request received for payment status lookups.");
        return ResponseEntity.ok(reportService.getPaymentStatuses());
    }

    @GetMapping("/lookups/payment-methods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<String>> getPaymentMethods() {
        log.info("Request received for payment method lookups.");
        return ResponseEntity.ok(reportService.getPaymentMethods());
    }

    private ResponseEntity<byte[]> createCsvResponse(String csvData, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDispositionFormData("attachment", fileName);

        byte[] bom = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        byte[] csvBytes = csvData.getBytes(StandardCharsets.UTF_8);
        byte[] responseBody = new byte[bom.length + csvBytes.length];
        System.arraycopy(bom, 0, responseBody, 0, bom.length);
        System.arraycopy(csvBytes, 0, responseBody, bom.length, csvBytes.length);

        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
    }
}