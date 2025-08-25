package in.project.computers.service.componentCompatibility;

import in.project.computers.DTO.builds.CompatibilityCheckRequest;
import in.project.computers.DTO.builds.CompatibilityResult;

public interface ComponentCompatibilityService {

    /**
     * ตรวจสอบความเข้ากันได้ของส่วนประกอบในบิลด์
     * @param buildId ID ของบิลด์
     * @return ผลลัพธ์การตรวจสอบความเข้ากันได้
     */
    CompatibilityResult checkCompatibility(String buildId);

    /**
     * ตรวจสอบความเข้ากันได้ของส่วนประกอบตาม request
     * @param request อ็อบเจ็กต์ที่บรรจุส่วนประกอบที่จะตรวจสอบ
     * @return ผลลัพธ์การตรวจสอบความเข้ากันได้
     */
    CompatibilityResult checkCompatibility(CompatibilityCheckRequest request);
}