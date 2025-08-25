package in.project.computers.service.dashboard;

import in.project.computers.DTO.dashboard.DashboardResponse;
import java.time.Instant;
import java.util.List;

public interface DashboardService {
    /**
     * ดึงข้อมูลสำหรับแดชบอร์ด
     * @param startDate วันเริ่มต้น
     * @param endDate วันสิ้นสุด
     * @return ข้อมูลแดชบอร์ด
     */
    DashboardResponse getDashboardData(Instant startDate, Instant endDate);

    /**
     * ดึงข้อมูลคำสั่งซื้อสำหรับส่งออกเป็นไฟล์
     * @param startDate วันเริ่มต้น
     * @param endDate วันสิ้นสุด
     * @return รายการคำสั่งซื้อ
     */
    List<DashboardResponse.RecentOrder> getOrdersForExport(Instant startDate, Instant endDate);
}