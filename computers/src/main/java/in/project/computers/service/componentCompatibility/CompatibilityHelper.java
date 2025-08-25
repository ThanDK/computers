package in.project.computers.service.componentCompatibility;

import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;

import java.util.List;

public interface CompatibilityHelper {

    /**
     * ตรวจสอบความเข้ากันได้ของ CPU และ Motherboard
     * @param cpu CPU
     * @param motherboard Motherboard
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkCpuAndMotherboard(Cpu cpu, Motherboard motherboard, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ RAM และ Motherboard
     * @param ramKitParts รายการ RamKit
     * @param motherboard Motherboard
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkRamCompatibility(List<BuildPart<RamKit>> ramKitParts, Motherboard motherboard, List<String> errors);

    /**
     * ตรวจสอบ Form Factor ของ Motherboard และ Case
     * @param motherboard Motherboard
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkFormFactorCompatibility(Motherboard motherboard, Case computerCase, List<String> errors);

    /**
     * ตรวจสอบ Form Factor ของ PSU และ Case
     * @param psu PSU
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkPsuFormFactor(Psu psu, Case computerCase, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ GPU กับ Motherboard และ Case
     * @param gpuParts รายการ GPU
     * @param motherboard Motherboard
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkGpuCompatibility(List<BuildPart<Gpu>> gpuParts, Motherboard motherboard, Case computerCase, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ CPU Cooler กับ Motherboard และ Case
     * @param cooler CPU Cooler
     * @param motherboard Motherboard
     * @param computerCase Case
     * @param warnings List สำหรับเก็บคำเตือน
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkCoolerCompatibility(Cooler cooler, Motherboard motherboard, Case computerCase, List<String> warnings, List<String> errors);

    /**
     * ตรวจสอบความเข้ากันได้ของ Storage กับ Motherboard
     * @param storageDriveParts รายการ Storage Drive
     * @param motherboard Motherboard
     * @param nvmeInterfaceId ID ของ interface แบบ NVMe
     * @param sataInterfaceIds List ID ของ interface แบบ SATA
     * @param warnings List สำหรับเก็บคำเตือน
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkStorageCompatibility(List<BuildPart<StorageDrive>> storageDriveParts, Motherboard motherboard, String nvmeInterfaceId, List<String> sataInterfaceIds, List<String> warnings, List<String> errors);

    /**
     * คำนวณ Wattage รวมโดยประมาณ
     * @param cpu CPU
     * @param motherboard Motherboard
     * @param ramKitParts รายการ RamKit
     * @param gpuParts รายการ GPU
     * @param cooler CPU Cooler
     * @return Wattage รวมโดยประมาณ
     */
    int calculateTotalWattage(Cpu cpu, Motherboard motherboard, List<BuildPart<RamKit>> ramKitParts, List<BuildPart<Gpu>> gpuParts, Cooler cooler);

    /**
     * ตรวจสอบว่า PSU มีกำลังไฟเพียงพอหรือไม่
     * @param psu PSU
     * @param totalWattage Wattage รวมของระบบ
     * @param errors List สำหรับเก็บข้อผิดพลาด
     * @param warnings List สำหรับเก็บคำเตือน
     */
    void checkPsuWattage(Psu psu, int totalWattage, List<String> errors, List<String> warnings);

    /**
     * ตรวจสอบช่อง Bay ใน Case สำหรับ Storage Drive
     * @param storageDriveParts รายการ Storage Drive
     * @param computerCase Case
     * @param errors List สำหรับเก็บข้อผิดพลาด
     */
    void checkStorageAndCaseBays(List<BuildPart<StorageDrive>> storageDriveParts, Case computerCase, List<String> errors);
}