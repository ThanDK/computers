package in.project.computers.service.dashboard;

import in.project.computers.DTO.dashboard.DashboardResponse;
import java.time.Instant;
import java.util.List;

public interface DashboardService {
    DashboardResponse getDashboardData(Instant startDate, Instant endDate);
    List<DashboardResponse.RecentOrder> getOrdersForExport(Instant startDate, Instant endDate);
}