package in.project.computers.controller.pageController;

import in.project.computers.DTO.builds.CompatibilityCheckRequest;
import in.project.computers.DTO.builds.ComputerBuildDetailResponse;
import in.project.computers.DTO.builds.ComputerBuildRequest;
import in.project.computers.DTO.builds.CompatibilityResult;
import in.project.computers.service.componentCompatibility.ComponentCompatibilityService;
import in.project.computers.service.computerBuildService.UserBuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Controller สำหรับจัดการชุดคอมพิวเตอร์ ของผู้ใช้
 * <p>
 * ให้ผู้ใช้สามารถสร้าง, บันทึก, ดู, ลบ และตรวจสอบความเข้ากันได้ของชุดคอมพิวเตอร์ที่จัดสเปคเอง
 */
@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
public class ComputerBuildController {

    private final UserBuildService userBuildService;
    private final ComponentCompatibilityService compatibilityService;

    /**
     * บันทึกการจัดสเปคคอมพิวเตอร์ใหม่ของผู้ใช้
     * <p>
     * Endpoint นี้ต้องมีการยืนยันตัวตน เพื่อระบุว่าเป็นบิลด์ของผู้ใช้คนใด
     * @param request ข้อมูลการจัดสเปค (ชื่อและ ID ชิ้นส่วน)
     * @return ข้อมูลรายละเอียดของบิลด์ที่สร้างสำเร็จ (HttpStatus 201 CREATED)
     */
    @PostMapping
    public ResponseEntity<ComputerBuildDetailResponse> saveBuild(@Valid @RequestBody ComputerBuildRequest request) {
        ComputerBuildDetailResponse savedBuild = userBuildService.saveBuild(request);
        return new ResponseEntity<>(savedBuild, HttpStatus.CREATED);
    }

    /**
     * ดึงรายการบิลด์ทั้งหมดของผู้ใช้ปัจจุบัน
     * <p>
     * Endpoint นี้ต้องมีการยืนยันตัวตน (Authentication) เพื่อดึงข้อมูลเฉพาะของผู้ใช้ที่ล็อกอิน
     * @return List ของบิลด์ทั้งหมดของผู้ใช้
     */
    @GetMapping
    public ResponseEntity<List<ComputerBuildDetailResponse>> getUserBuilds() {
        List<ComputerBuildDetailResponse> builds = userBuildService.getBuildsForCurrentUser();
        return ResponseEntity.ok(builds);
    }

    @PostMapping("/check-compatibility")
    public ResponseEntity<CompatibilityResult> checkTransientBuildCompatibility(@RequestBody CompatibilityCheckRequest request) {
        CompatibilityResult result = compatibilityService.checkCompatibility(request);
        return ResponseEntity.ok(result);
    }

    /**
     * ดึงข้อมูลบิลด์เฉพาะเจาะจงตาม ID
     * @param buildId ID ของบิลด์ที่ต้องการดูข้อมูล
     * @return ข้อมูลโดยละเอียดของบิลด์ที่ร้องขอ
     */
    @GetMapping("/{buildId}")
    public ResponseEntity<ComputerBuildDetailResponse> getBuildDetails(@PathVariable String buildId) {
        ComputerBuildDetailResponse build = userBuildService.getBuildDetails(buildId);
        return ResponseEntity.ok(build);
    }

    /**
     * ตรวจสอบความเข้ากันได้ของฮาร์ดแวร์ในบิลด์ที่บันทึกไว้
     * @param buildId ID ของบิลด์ที่ต้องการตรวจสอบ
     * @return ผลลัพธ์การตรวจสอบความเข้ากันได้ (CompatibilityResult)
     */
    @GetMapping("/check/{buildId}")
    public ResponseEntity<CompatibilityResult> checkBuildCompatibility(@PathVariable String buildId) {
        CompatibilityResult result = compatibilityService.checkCompatibility(buildId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{buildId}")
    public ResponseEntity<ComputerBuildDetailResponse> updateBuild(
            @PathVariable String buildId,
            @RequestBody ComputerBuildRequest request
    ) {
        ComputerBuildDetailResponse updatedBuild = userBuildService.updateBuild(buildId, request);
        return ResponseEntity.ok(updatedBuild);
    }
    /**
     * ลบบิลด์ที่บันทึกไว้
     * <p>
     * Endpoint นี้ต้องมีการยืนยันตัวตน และผู้ใช้ต้องเป็นเจ้าของบิลด์เท่านั้นจึงจะลบได้
     * @param buildId ID ของบิลด์ที่ต้องการลบ
     * return HttpStatus 204 NO_CONTENT หากลบสำเร็จ
     */
    @DeleteMapping("/{buildId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuild(@PathVariable String buildId) {
        userBuildService.deleteBuild(buildId);
    }
}