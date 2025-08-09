package in.project.computers.controller.pageController;

import in.project.computers.DTO.dashboard.DashboardResponse;
import in.project.computers.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Controller สำหรับดึงข้อมูลสรุปสำหรับหน้า Dashboard ของ Admin
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * ดึงข้อมูลสรุปสำหรับ Dashboard ตามช่วงวันที่กำหนด
     * @param startDate วันที่เริ่มต้น (รูปแบบ YYYY-MM-DD)
     * @param endDate วันที่สิ้นสุด (รูปแบบ YYYY-MM-DD)
     * @return ข้อมูลสรุป DashboardResponse (เช่น ยอดขาย, ผู้ใช้ใหม่)
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboardData(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Admin request for dashboard data from {} to {}", startDate, endDate);
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        DashboardResponse response = dashboardService.getDashboardData(startInstant, endInstant);
        return ResponseEntity.ok(response);
    }

    /**
     * ดึงรายการ Order ทั้งหมดในช่วงวันที่กำหนด สำหรับการ Export
     * @param startDate วันที่เริ่มต้น (รูปแบบ YYYY-MM-DD)
     * @param endDate วันที่สิ้นสุด (รูปแบบ YYYY-MM-DD)
     * @return List ของ Order สำหรับนำไป Export
     */
    @GetMapping("/export")
    public ResponseEntity<List<DashboardResponse.RecentOrder>> exportOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Admin request for order export from {} to {}", startDate, endDate);
        Instant startInstant = startDate.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endInstant = endDate.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        List<DashboardResponse.RecentOrder> ordersToExport = dashboardService.getOrdersForExport(startInstant, endInstant);
        return ResponseEntity.ok(ordersToExport);
    }
}