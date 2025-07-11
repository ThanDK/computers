package in.project.computers.controller;

import in.project.computers.dto.builds.ComputerBuildDetailResponse;
import in.project.computers.dto.builds.ComputerBuildRequest;
import in.project.computers.dto.builds.CompatibilityResult;
import in.project.computers.service.componentCompatibility.ComponentCompatibilityService;
import in.project.computers.service.ComputerBuildService.UserBuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller สำหรับจัดการ API ที่เกี่ยวกับชุดคอมพิวเตอร์ (PC Build) ของผู้ใช้
 * รวมถึงการบันทึก, เรียกดูข้อมูลฉบับเต็ม, ลบ, และตรวจสอบความเข้ากันได้ของฮาร์ดแวร์
 * ทุก Endpoint ใน Controller นี้ต้องการการยืนยันตัวตน (Authentication)
 */
@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
@CrossOrigin("*") // อนุญาตการเข้าถึงจากทุก Origin (ควรปรับแก้ใน Production)
public class ComputerBuildController {

    private final UserBuildService userBuildService;
    private final ComponentCompatibilityService compatibilityService;

    /**
     * [POST] /api/builds
     * Endpoint สำหรับบันทึกการจัดสเปคคอมพิวเตอร์ใหม่ของผู้ใช้
     * หลังจากบันทึกสำเร็จ จะคืนข้อมูลรายละเอียดทั้งหมดของ Build ที่เพิ่งสร้าง
     *
     * @param request DTO ที่มีชื่อบิลด์และรายการ ID ของส่วนประกอบต่างๆ
     * @return ResponseEntity ที่มีข้อมูลรายละเอียดทั้งหมดของบิลด์ (ComputerBuildDetailResponse) พร้อมสถานะ 201 CREATED
     */
    @PostMapping
    public ResponseEntity<ComputerBuildDetailResponse> saveBuild(@Valid @RequestBody ComputerBuildRequest request) {
        ComputerBuildDetailResponse savedBuild = userBuildService.saveBuild(request);
        return new ResponseEntity<>(savedBuild, HttpStatus.CREATED);
    }

    /**
     * [GET] /api/builds
     * Endpoint สำหรับดึงรายการบิลด์ทั้งหมดที่ผู้ใช้ปัจจุบันได้บันทึกไว้
     *
     * @return รายการบิลด์ทั้งหมดของผู้ใช้ในรูปแบบข้อมูลฉบับเต็ม (List of ComputerBuildDetailResponse)
     */
    @GetMapping
    public ResponseEntity<List<ComputerBuildDetailResponse>> getUserBuilds() {
        List<ComputerBuildDetailResponse> builds = userBuildService.getBuildsForCurrentUser();
        return ResponseEntity.ok(builds);
    }

    /**
     * [GET] /api/builds/{buildId}
     * Endpoint สำหรับดึงข้อมูลบิลด์เฉพาะเจาะจงตาม ID พร้อมรายละเอียดชิ้นส่วนทั้งหมด
     * Service Layer จะทำการตรวจสอบเพื่อให้แน่ใจว่าผู้ใช้ที่ร้องขอเป็นเจ้าของบิลด์นั้น
     *
     * @param buildId ID ของบิลด์ที่ต้องการดูข้อมูล
     * @return ข้อมูลโดยละเอียดทั้งหมดของบิลด์ที่ร้องขอ (ComputerBuildDetailResponse)
     */
    @GetMapping("/{buildId}")
    public ResponseEntity<ComputerBuildDetailResponse> getBuildDetails(@PathVariable String buildId) {
        ComputerBuildDetailResponse build = userBuildService.getBuildDetails(buildId);
        return ResponseEntity.ok(build);
    }

    /**
     * [GET] /api/builds/check/{buildId}
     * Endpoint สำหรับตรวจสอบความเข้ากันได้ของฮาร์ดแวร์ในบิลด์ที่บันทึกไว้แล้ว
     *
     * @param buildId ID ของบิลด์ที่ต้องการตรวจสอบ
     * @return ผลลัพธ์การตรวจสอบโดยละเอียด, รวมถึงข้อผิดพลาดและคำเตือนต่างๆ
     */
    @GetMapping("/check/{buildId}")
    public ResponseEntity<CompatibilityResult> checkBuildCompatibility(@PathVariable String buildId) {
        CompatibilityResult result = compatibilityService.checkCompatibility(buildId);
        return ResponseEntity.ok(result);
    }

    /**
     * [DELETE] /api/builds/{buildId}
     * Endpoint สำหรับลบบิลด์ที่บันทึกไว้
     * Service Layer จะตรวจสอบสิทธิ์ความเป็นเจ้าของก่อนทำการลบ
     *
     * @param buildId ID ของบิลด์ที่ต้องการลบ
     * return สถานะ 204 NO CONTENT เมื่อลบสำเร็จ
     */
    @DeleteMapping("/{buildId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuild(@PathVariable String buildId) {
        userBuildService.deleteBuild(buildId);
    }
}