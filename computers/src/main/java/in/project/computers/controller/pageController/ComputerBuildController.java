package in.project.computers.controller.pageController;

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
 * Controller สำหรับจัดการชุดคอมพิวเตอร์ (PC Build) ของผู้ใช้
 * <p>
 * <b>คำเตือน:</b> {@code @CrossOrigin("*")} ไม่ปลอดภัยสำหรับ Production ควรระบุ Origin ของ Frontend ให้ชัดเจน
 */
@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
public class ComputerBuildController {

    private final UserBuildService userBuildService;
    private final ComponentCompatibilityService compatibilityService;

    /**
     * บันทึกการจัดสเปคคอมพิวเตอร์ใหม่ของผู้ใช้
     * @param request ข้อมูลการจัดสเปค (ชื่อและ ID ชิ้นส่วน)
     * @return ข้อมูลรายละเอียดของบิลด์ที่สร้างสำเร็จ (HttpStatus 201)
     */
    @PostMapping
    public ResponseEntity<ComputerBuildDetailResponse> saveBuild(@Valid @RequestBody ComputerBuildRequest request) {
        ComputerBuildDetailResponse savedBuild = userBuildService.saveBuild(request);
        return new ResponseEntity<>(savedBuild, HttpStatus.CREATED);
    }

    /**
     * ดึงรายการบิลด์ทั้งหมดของผู้ใช้ปัจจุบัน
     * @return List ของบิลด์ทั้งหมดของผู้ใช้
     */
    @GetMapping
    public ResponseEntity<List<ComputerBuildDetailResponse>> getUserBuilds() {
        List<ComputerBuildDetailResponse> builds = userBuildService.getBuildsForCurrentUser();
        return ResponseEntity.ok(builds);
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
     * @return ผลลัพธ์การตรวจสอบความเข้ากันได้
     */
    @GetMapping("/check/{buildId}")
    public ResponseEntity<CompatibilityResult> checkBuildCompatibility(@PathVariable String buildId) {
        CompatibilityResult result = compatibilityService.checkCompatibility(buildId);
        return ResponseEntity.ok(result);
    }

    /**
     * ลบบิลด์ที่บันทึกไว้ (คืนค่า 204 No Content)
     * @param buildId ID ของบิลด์ที่ต้องการลบ
     */
    @DeleteMapping("/{buildId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuild(@PathVariable String buildId) {
        userBuildService.deleteBuild(buildId);
    }
}