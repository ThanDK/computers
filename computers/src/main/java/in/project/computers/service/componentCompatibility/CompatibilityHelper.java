package in.project.computers.service.componentCompatibility;

import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;

import java.util.List;

/**
 * Interface สำหรับคลาสผู้ช่วย (Helper) ในการตรวจสอบความเข้ากันได้ของชิ้นส่วน
 * <p>
 * กำหนดเมธอดสำหรับการตรวจสอบความสัมพันธ์ระหว่างชิ้นส่วนต่างๆ โดยเฉพาะ
 * เช่น CPU กับ Motherboard, RAM กับ Motherboard เป็นต้น
 * ถูกออกแบบมาเพื่อใช้ภายใน {@link ComponentCompatibilityService}
 */
public interface CompatibilityHelper {

    /**
     * ตรวจสอบความเข้ากันได้ระหว่าง CPU และ Motherboard (โดยหลักคือ Socket)
     * @param cpu CPU ที่จะตรวจสอบ
     * @param motherboard Motherboard ที่จะตรวจสอบ
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด หากไม่เข้ากัน
     */
    void checkCpuAndMotherboard(Cpu cpu, Motherboard motherboard, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ RAM กับ Motherboard
     * <p>
     * เช็คประเภท RAM (เช่น DDR4, DDR5), จำนวนช่อง (slots), และความจุสูงสุด
     * @param ramKitParts รายการ RamKit ที่อยู่ในบิลด์
     * @param motherboard Motherboard
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด
     */
    void checkRamCompatibility(List<BuildPart<RamKit>> ramKitParts, Motherboard motherboard, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ Form Factor ระหว่าง Motherboard และ Case
     * @param motherboard Motherboard
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด
     */
    void checkFormFactorCompatibility(Motherboard motherboard, Case computerCase, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ Form Factor ระหว่าง PSU และ Case
     * @param psu Power Supply Unit
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด
     */
    void checkPsuFormFactor(Psu psu, Case computerCase, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ GPU กับ Motherboard และ Case
     * <p>
     * เช็คความยาวของ GPU ว่าใส่ใน Case ได้หรือไม่ และจำนวนช่อง PCIe ที่ต้องการ
     * @param gpuParts รายการ GPU ที่อยู่ในบิลด์
     * @param motherboard Motherboard
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด
     */
    void checkGpuCompatibility(List<BuildPart<Gpu>> gpuParts, Motherboard motherboard, Case computerCase, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ CPU Cooler กับ Motherboard และ Case
     * <p>
     * เช็ค Socket, ความสูงของ Cooler ว่าเกินขนาด Case หรือไม่
     * @param cooler CPU Cooler
     * @param motherboard Motherboard
     * @param computerCase Case
     * @param warnings List สำหรับเก็บคำเตือน (เช่น อาจบังช่อง RAM)
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด
     */
    void checkCoolerCompatibility(Cooler cooler, Motherboard motherboard, Case computerCase, List<String> warnings, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของหน่วยความจำ (Storage) กับ Motherboard
     * <p>
     * เช็คจำนวนพอร์ต M.2 และ SATA ที่มีอยู่บน Motherboard
     * @param storageDriveParts รายการ Storage Drive ที่อยู่ในบิลด์
     * @param motherboard Motherboard
     * @param nvmeInterfaceId ID ของ Storage Interface ประเภท NVMe
     * @param sataInterfaceIds List ของ ID ของ Storage Interface ประเภท SATA
     * @param warnings List สำหรับเก็บคำเตือน (เช่น การใช้ M.2 อาจปิดการทำงานของพอร์ต SATA บางช่อง)
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด
     */
    void checkStorageCompatibility(List<BuildPart<StorageDrive>> storageDriveParts, Motherboard motherboard, String nvmeInterfaceId, List<String> sataInterfaceIds, List<String> warnings, List<String> errors);

    /**
     * คำนวณปริมาณการใช้พลังงาน (Wattage) โดยประมาณของส่วนประกอบทั้งหมดในบิลด์
     * @param cpu CPU
     * @param motherboard Motherboard
     * @param ramKitParts รายการ RamKit
     * @param gpuParts รายการ GPU
     * @param cooler CPU Cooler
     * @return ค่า Wattage รวมโดยประมาณ
     */
    int calculateTotalWattage(Cpu cpu, Motherboard motherboard, List<BuildPart<RamKit>> ramKitParts, List<BuildPart<Gpu>> gpuParts, Cooler cooler);

    /**
     * ตรวจสอบว่า PSU มีกำลังไฟเพียงพอต่อการใช้งานของระบบหรือไม่
     * @param psu Power Supply Unit
     * @param totalWattage ค่า Wattage รวมที่คำนวณได้
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด (เช่น กำลังไฟไม่พอ)
     * @param warnings List สำหรับเก็บคำเตือน (เช่น กำลังไฟพอดีเกินไป ควรเผื่อ)
     */
    void checkPsuWattage(Psu psu, int totalWattage, List<String> errors, List<String> warnings);

    /**
     * ตรวจสอบว่า Case มีช่อง (Bay) เพียงพอสำหรับติดตั้ง Storage Drive ทั้งหมดหรือไม่
     * @param storageDriveParts รายการ Storage Drive ที่อยู่ในบิลด์
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อความข้อผิดพลาด
     */
    void checkStorageAndCaseBays(List<BuildPart<StorageDrive>> storageDriveParts, Case computerCase, List<String> errors);
}