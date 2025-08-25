package in.project.computers.service.computerBuildService;

import in.project.computers.DTO.builds.ComputerBuildDetailResponse;
import in.project.computers.DTO.builds.ComputerBuildRequest;

import java.util.List;

public interface UserBuildService {

    /**
     * บันทึก Build ใหม่
     * @param request ข้อมูล Build ที่จะสร้าง
     * @return รายละเอียด Build ที่สร้างใหม่
     */
    ComputerBuildDetailResponse saveBuild(ComputerBuildRequest request);

    /**
     * ดึงรายละเอียด Build ตาม ID
     * @param buildId ID ของ Build
     * @return รายละเอียด Build
     */
    ComputerBuildDetailResponse getBuildDetails(String buildId);

    /**
     * ดึง Build ทั้งหมดของผู้ใช้ปัจจุบัน
     * @return รายการ Build ทั้งหมดของผู้ใช้
     */
    List<ComputerBuildDetailResponse> getBuildsForCurrentUser();

    /**
     * ลบ Build ตาม ID
     * @param buildId ID ของ Build
     */
    void deleteBuild(String buildId);

    /**
     * อัปเดต Build
     * @param buildId ID ของ Build ที่จะอัปเดต
     * @param request ข้อมูล Build ใหม่
     * @return รายละเอียด Build ที่อัปเดตแล้ว
     */
    ComputerBuildDetailResponse updateBuild(String buildId, ComputerBuildRequest request);
}