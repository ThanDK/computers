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

@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
public class ComputerBuildController {

    private final UserBuildService userBuildService;
    private final ComponentCompatibilityService compatibilityService;

    /**
     * บันทึกการจัดสเปคคอมพิวเตอร์ใหม่
     * <p>
     * Endpoint นี้ใช้สำหรับให้ผู้ใช้ที่ล็อกอินแล้ว บันทึกชุดประกอบคอมพิวเตอร์ (Build) ที่ตนเองได้จัดสเปคไว้
     * ระบบจะเชื่อมโยง Build นี้กับบัญชีของผู้ใช้โดยอัตโนมัติ
     * </p>
     * @param request อ็อบเจกต์ {@link ComputerBuildRequest} ที่มีชื่อและรายการ ID ของชิ้นส่วน
     * @return ResponseEntity ที่มีข้อมูล {@link ComputerBuildDetailResponse} ของบิลด์ที่สร้างสำเร็จและสถานะ 201 Created
     */
    @PostMapping
    public ResponseEntity<ComputerBuildDetailResponse> saveBuild(@Valid @RequestBody ComputerBuildRequest request) {
        ComputerBuildDetailResponse savedBuild = userBuildService.saveBuild(request);
        return new ResponseEntity<>(savedBuild, HttpStatus.CREATED);
    }

    /**
     * ดึงรายการบิลด์ทั้งหมดของผู้ใช้ปัจจุบัน
     * <p>
     * Endpoint นี้สำหรับให้ผู้ใช้ที่ล็อกอินแล้ว ดึงข้อมูลชุดประกอบคอมพิวเตอร์ทั้งหมดที่เคยบันทึกไว้
     * </p>
     * @return ResponseEntity ที่มี List ของ {@link ComputerBuildDetailResponse} และสถานะ 200 OK
     */
    @GetMapping
    public ResponseEntity<List<ComputerBuildDetailResponse>> getUserBuilds() {
        List<ComputerBuildDetailResponse> builds = userBuildService.getBuildsForCurrentUser();
        return ResponseEntity.ok(builds);
    }

    /**
     * ตรวจสอบความเข้ากันได้ของชุดชิ้นส่วนที่ยังไม่ได้บันทึก
     * <p>
     * Endpoint นี้ใช้สำหรับตรวจสอบความเข้ากันได้ของชุดชิ้นส่วนที่ผู้ใช้เลือกในหน้าจัดสเปคแบบ Real-time
     * โดยไม่ต้องบันทึกเป็น Build ก่อน เหมาะสำหรับผู้ใช้ทั่วไปที่ยังไม่ได้ล็อกอิน
     * </p>
     * @param request อ็อบเจกต์ {@link CompatibilityCheckRequest} ที่มีรายการ ID ของชิ้นส่วนที่ต้องการตรวจสอบ
     * @return ResponseEntity ที่มีผลลัพธ์ {@link CompatibilityResult} และสถานะ 200 OK
     */
    @PostMapping("/check-compatibility")
    public ResponseEntity<CompatibilityResult> checkTransientBuildCompatibility(@RequestBody CompatibilityCheckRequest request) {
        CompatibilityResult result = compatibilityService.checkCompatibility(request);
        return ResponseEntity.ok(result);
    }

    /**
     * ดึงข้อมูลรายละเอียดของบิลด์ตาม ID
     * <p>
     * Endpoint นี้อนุญาตให้ผู้ใช้ดูรายละเอียดของชุดประกอบคอมพิวเตอร์ที่บันทึกไว้ตาม ID ที่ระบุ
     * </p>
     * @param buildId ID ของบิลด์ที่ต้องการดูข้อมูล (จาก Path Variable)
     * @return ResponseEntity ที่มีข้อมูล {@link ComputerBuildDetailResponse} ของบิลด์และสถานะ 200 OK
     */
    @GetMapping("/{buildId}")
    public ResponseEntity<ComputerBuildDetailResponse> getBuildDetails(@PathVariable String buildId) {
        ComputerBuildDetailResponse build = userBuildService.getBuildDetails(buildId);
        return ResponseEntity.ok(build);
    }

    /**
     * ตรวจสอบความเข้ากันได้ของฮาร์ดแวร์ในบิลด์ที่บันทึกไว้
     * <p>
     * Endpoint นี้ใช้สำหรับเรียกการตรวจสอบความเข้ากันได้ของชิ้นส่วนต่างๆ ในชุดประกอบคอมพิวเตอร์ที่ผู้ใช้เคยบันทึกไว้แล้ว
     * </p>
     * @param buildId ID ของบิลด์ที่ต้องการตรวจสอบ (จาก Path Variable)
     * @return ResponseEntity ที่มีผลลัพธ์ {@link CompatibilityResult} และสถานะ 200 OK
     */
    @GetMapping("/check/{buildId}")
    public ResponseEntity<CompatibilityResult> checkBuildCompatibility(@PathVariable String buildId) {
        CompatibilityResult result = compatibilityService.checkCompatibility(buildId);
        return ResponseEntity.ok(result);
    }

    /**
     * อัปเดตข้อมูลการจัดสเปคคอมพิวเตอร์
     * <p>
     * Endpoint นี้ใช้สำหรับให้ผู้ใช้ที่ล็อกอินแล้ว แก้ไขชุดประกอบคอมพิวเตอร์ที่เคยบันทึกไว้ เช่น เปลี่ยนชิ้นส่วน หรือเปลี่ยนชื่อ
     * </p>
     * @param buildId ID ของบิลด์ที่ต้องการอัปเดต (จาก Path Variable)
     * @param request อ็อบเจกต์ {@link ComputerBuildRequest} ที่มีข้อมูลใหม่
     * @return ResponseEntity ที่มีข้อมูล {@link ComputerBuildDetailResponse} ที่อัปเดตแล้วและสถานะ 200 OK
     */
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
     * Endpoint นี้สำหรับให้ผู้ใช้ลบชุดประกอบคอมพิวเตอร์ของตนเองที่ไม่ต้องการแล้ว ระบบจะตรวจสอบความเป็นเจ้าของก่อนทำการลบ
     * เมื่อดำเนินการสำเร็จจะคืนสถานะ 204 No Content
     * </p>
     * @param buildId ID ของบิลด์ที่ต้องการลบ (จาก Path Variable)
     */
    @DeleteMapping("/{buildId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuild(@PathVariable String buildId) {
        userBuildService.deleteBuild(buildId);
    }
}