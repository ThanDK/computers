package in.project.computers.entity.computerBuild;

import in.project.computers.entity.component.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * เอกสารอธิบาย:
 * นี่คือการปรับปรุงครั้งสำคัญที่สุด!
 * เราเปลี่ยนจากการเก็บแค่ 'Id' ของ Component มาเป็นการเก็บ 'Object' ของ Component ทั้งหมดโดยตรง
 * ประโยชน์:
 * 1. Data Integrity: ข้อมูล Build จะสมบูรณ์ในตัวเองเสมอ
 * 2. Performance: ลดการ query ฐานข้อมูลลงอย่างมหาศาล เวลาดึงข้อมูล Build ไม่ต้องไป lookup หา component ทีละชิ้น
 * 3. Simplicity: ทำให้ Logic ใน Service Layer ง่ายลงมาก
 * ตัวอย่างการเปลี่ยนแปลง:
 * - private String cpuId;  --> private Cpu cpu;
 * - private Map<String, Integer> ramKits; --> private List<BuildPart<RamKit>> ramKits;
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "computer_builds")
public class ComputerBuild {
    @Id
    private String id;
    private String userId;
    private String buildName;

    // --- ส่วนประกอบที่มีชิ้นเดียว ---
    private Cpu cpu;
    private Motherboard motherboard;
    private Psu psu;
    private Case caseDetail; // 'case' is a reserved keyword in Java, so 'caseDetail' is a good name.
    private Cooler cooler;

    // --- ส่วนประกอบที่อาจมีหลายชิ้น (ใช้ BuildPart) ---
    private List<BuildPart<RamKit>> ramKits;
    private List<BuildPart<Gpu>> gpus;
    private List<BuildPart<StorageDrive>> storageDrives;
}