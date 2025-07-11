package in.project.computers.service.ComputerBuildService;

import in.project.computers.dto.builds.ComputerBuildDetailResponse;
import in.project.computers.dto.builds.ComputerBuildRequest;

import java.util.List;

/**
 * Service Layer Interface ที่กำหนดการกระทำทั้งหมดที่เกี่ยวข้องกับการจัดการชุดคอมพิวเตอร์ (PC Build) ของผู้ใช้
 * รับผิดชอบในการสร้าง, ดึงข้อมูล, และลบข้อมูล Build ของผู้ใช้ที่ล็อกอินอยู่
 */
public interface UserBuildService {

    /**
     * บันทึกชุดคอมพิวเตอร์ (Build) ใหม่ที่ผู้ใช้สร้างขึ้น
     * @param request ข้อมูลของ Build ที่จะสร้าง รับมาจากผู้ใช้
     * @return ข้อมูลรายละเอียดทั้งหมดของ Build ที่ถูกสร้างและบันทึกเรียบร้อยแล้ว (ComputerBuildDetailResponse)
     */
    ComputerBuildDetailResponse saveBuild(ComputerBuildRequest request);

    /**
     * ดึงข้อมูลรายละเอียดทั้งหมดของ Build ตาม ID ที่ระบุ
     * @param buildId ID ของ Build ที่ต้องการดึงข้อมูล
     * @return ข้อมูลรายละเอียดทั้งหมดของ Build ที่ระบุ รวมถึงข้อมูลชิ้นส่วนฉบับเต็ม
     */
    ComputerBuildDetailResponse getBuildDetails(String buildId);

    /**
     * ดึงข้อมูล Build ทั้งหมดที่เป็นของผู้ใช้ที่กำลังล็อกอินอยู่
     * @return รายการ (List) ของข้อมูล Build ทั้งหมดที่เป็นของผู้ใช้ พร้อมรายละเอียดฉบับเต็ม
     */
    List<ComputerBuildDetailResponse> getBuildsForCurrentUser();

    /**
     * ลบข้อมูล Build ออกจากระบบตาม ID ที่ระบุ
     * @param buildId ID ของ Build ที่ต้องการลบ
     */
    void deleteBuild(String buildId);
}