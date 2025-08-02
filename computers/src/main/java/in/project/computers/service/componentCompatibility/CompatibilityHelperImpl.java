package in.project.computers.service.componentCompatibility;

import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;
import in.project.computers.entity.lookup.FormFactor;
import in.project.computers.entity.lookup.Socket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// คลาส Helper ที่รวบรวมตรรกะการตรวจสอบความเข้ากันได้ของชิ้นส่วนแต่ละคู่
@Component
@RequiredArgsConstructor
@Slf4j
public class CompatibilityHelperImpl implements CompatibilityHelper {

    @Override
    public void checkCpuAndMotherboard(Cpu cpu, Motherboard motherboard, List<String> errors) {
        // === [CHECK-5.1] ตรวจสอบ Socket ของ CPU และ Motherboard ว่าตรงกันหรือไม่ ===
        if (!Objects.equals(cpu.getSocket().getId(), motherboard.getSocket().getId())) {
            errors.add(String.format("CPU เข้ากันไม่ได้: '%s' (Socket %s) ไม่สามารถติดตั้งบนเมนบอร์ด '%s' (Socket %s) ได้",
                    cpu.getName(), cpu.getSocket().getName(), motherboard.getName(), motherboard.getSocket().getName()));
        }
    }

    @Override
    public void checkRamCompatibility(List<BuildPart<RamKit>> ramKitParts, Motherboard motherboard, List<String> errors) {
        // === [CHECK-5.2] ตรวจสอบ RAM: จำนวนแถว, ขนาดรวม, และประเภท ===
        // 5.2.1: ตรวจสอบจำนวนแถว RAM ทั้งหมดเทียบกับช่องบนเมนบอร์ด
        int totalSticksRequired = ramKitParts.stream()
                .mapToInt(part -> part.getQuantity() * part.getComponent().getModuleCount())
                .sum();
        if (totalSticksRequired > motherboard.getRam_slot_count()) {
            errors.add(String.format("ช่อง RAM ไม่พอ: เมนบอร์ดมีช่อง RAM %d ช่อง แต่คุณเลือก RAM ทั้งหมด %d แถว",
                    motherboard.getRam_slot_count(), totalSticksRequired));
        }
        // 5.2.2: ตรวจสอบขนาด RAM รวม (GB) เทียบกับขนาดสูงสุดที่เมนบอร์ดรองรับ
        int totalRamGb = ramKitParts.stream()
                .mapToInt(part -> part.getQuantity() * part.getComponent().getRam_size_gb())
                .sum();
        if (totalRamGb > motherboard.getMax_ram_gb()) {
            errors.add(String.format("ขนาด RAM เกิน: เมนบอร์ดรองรับ RAM สูงสุด %dGB, แต่คุณเลือกทั้งหมด %dGB",
                    motherboard.getMax_ram_gb(), totalRamGb));
        }
        // 5.2.3: ตรวจสอบประเภทของ RAM (เช่น DDR4, DDR5) ว่าตรงกับที่เมนบอร์ดรองรับหรือไม่
        for (BuildPart<RamKit> part : ramKitParts) {
            RamKit ram = part.getComponent();
            if (!Objects.equals(ram.getRamType().getId(), motherboard.getRamType().getId())) {
                errors.add(String.format("RAM เข้ากันไม่ได้: RAM '%s' (ประเภท %s) ไม่ใช่ประเภทเดียวกับที่เมนบอร์ด '%s' (ประเภท %s) รองรับ",
                        ram.getName(), ram.getRamType().getName(), motherboard.getName(), motherboard.getRamType().getName()));
            }
        }
    }

    @Override
    public void checkFormFactorCompatibility(Motherboard motherboard, Case computerCase, List<String> errors) {
        // === [CHECK-5.3] ตรวจสอบขนาด Motherboard (Form Factor) ว่าเข้ากับเคสได้หรือไม่ ===
        List<String> supportedIds = computerCase.getSupportedFormFactors().stream()
                .map(FormFactor::getId)
                .toList();

        if (!supportedIds.contains(motherboard.getFormFactor().getId())) {
            String mbFormFactorName = motherboard.getFormFactor().getName();
            errors.add(String.format("ขนาดไม่พอดี: เมนบอร์ด '%s' (ขนาด %s) ไม่สามารถติดตั้งในเคส '%s' ได้",
                    motherboard.getName(), mbFormFactorName, computerCase.getName()));
        }
    }

    @Override
    public void checkPsuFormFactor(Psu psu, Case computerCase, List<String> errors) {
        // === [CHECK-5.4] ตรวจสอบขนาด PSU (Form Factor) ว่าเข้ากับเคสได้หรือไม่ ===
        if (psu.getFormFactor() == null) {
            log.warn("PSU '{}' is missing form factor data. Skipping compatibility check.", psu.getName());
            return;
        }

        List<String> supportedPsuFormFactorIds = Optional.ofNullable(computerCase.getSupportedPsuFormFactors())
                .orElse(Collections.emptyList())
                .stream()
                .map(FormFactor::getId)
                .toList();

        if (!supportedPsuFormFactorIds.contains(psu.getFormFactor().getId())) {
            errors.add(String.format("ขนาดไม่พอดี: Power Supply '%s' (ขนาด %s) ไม่สามารถติดตั้งในเคส '%s' ได้",
                    psu.getName(), psu.getFormFactor().getName(), computerCase.getName()));
        }
    }

    @Override
    public void checkGpuCompatibility(List<BuildPart<Gpu>> gpuParts, Motherboard motherboard, Case computerCase, List<String> errors) {
        // === [CHECK-5.5] ตรวจสอบ GPU: จำนวนการ์ดจอ และความยาว ===
        if (gpuParts == null || gpuParts.isEmpty()) return;

        // 5.5.1: ตรวจสอบจำนวนการ์ดจอทั้งหมดเทียบกับช่อง PCIe x16 บนเมนบอร์ด
        int totalGpuCount = gpuParts.stream().mapToInt(BuildPart::getQuantity).sum();
        if (totalGpuCount > motherboard.getPcie_x16_slot_count()) {
            errors.add(String.format("ช่อง GPU ไม่พอ: เมนบอร์ดมีช่อง PCIe x16 เพียง %d ช่อง แต่เลือกการ์ดจอ %d ตัว",
                    motherboard.getPcie_x16_slot_count(), totalGpuCount));
        }
        // 5.5.2: ตรวจสอบความยาวของการ์ดจอแต่ละตัวเทียบกับพื้นที่ในเคส
        for (BuildPart<Gpu> part : gpuParts) {
            Gpu gpu = part.getComponent();
            if (gpu.getLength_mm() > computerCase.getMax_gpu_length_mm()) {
                errors.add(String.format("ขนาดไม่พอดี: การ์ดจอ '%s' (ยาว %dmm) ยาวเกินไปสำหรับเคส '%s' (รองรับสูงสุด %dmm)",
                        gpu.getName(), gpu.getLength_mm(), computerCase.getName(), computerCase.getMax_gpu_length_mm()));
            }
        }
    }

    @Override
    public void checkCoolerCompatibility(Cooler cooler, Motherboard motherboard, Case computerCase, List<String> warnings, List<String> errors) {
        // === [CHECK-5.6] ตรวจสอบชุดระบายความร้อน CPU ===
        if (cooler != null) {
            // 5.6.1: ตรวจสอบ Socket ว่าเข้ากับเมนบอร์ดได้หรือไม่ (สำหรับ Cooler ทุกประเภท)
            List<String> supportedSocketIds = cooler.getSupportedSockets().stream().map(Socket::getId).toList();
            if (!supportedSocketIds.contains(motherboard.getSocket().getId())) {
                errors.add(String.format("ชุดระบายความร้อนเข้ากันไม่ได้: '%s' ไม่รองรับ Socket ของเมนบอร์ด (%s)",
                        cooler.getName(), motherboard.getSocket().getName()));
            }
            // 5.6.2: ตรวจสอบสำหรับชุดระบายความร้อนด้วยน้ำ (AIO)
            if (cooler.getRadiatorSize_mm() > 0) {
                List<Integer> supportedSizes = Optional.ofNullable(computerCase.getSupportedRadiatorSizesMm()).orElse(Collections.emptyList());
                if (!supportedSizes.contains(cooler.getRadiatorSize_mm())) {
                    errors.add(String.format("ขนาดไม่พอดี: ชุดระบายความร้อนด้วยน้ำ '%s' (ขนาดหม้อน้ำ %dmm) ไม่สามารถติดตั้งในเคส '%s' ได้",
                            cooler.getName(), cooler.getRadiatorSize_mm(), computerCase.getName()));
                }
                warnings.add("คำเตือน: การติดตั้งชุดระบายความร้อนด้วยน้ำ อาจมีปัญหากับ RAM ที่มีฮีทซิงค์สูง กรุณาตรวจสอบระยะห่างของเคสและเมนบอร์ด");
            }
            // 5.6.3: ตรวจสอบสำหรับชุดระบายความร้อนด้วยลม (Air Cooler)
            else {
                if (cooler.getHeight_mm() > computerCase.getMax_cooler_height_mm()) {
                    errors.add(String.format("ขนาดไม่พอดี: ชุดระบายความร้อน '%s' (สูง %dmm) สูงเกินไปสำหรับเคส '%s' (รองรับสูงสุด %dmm)",
                            cooler.getName(), cooler.getHeight_mm(), computerCase.getName(), computerCase.getMax_cooler_height_mm()));
                }
            }
        } else {
            // 5.6.4: กรณีที่ผู้ใช้ไม่ได้เลือก Cooler
            warnings.add("ไม่ได้เลือกชุดระบายความร้อน CPU: กรุณาตรวจสอบว่า CPU ของคุณมีชุดระบายความร้อนแถมมาด้วย หรือเลือกชุดระบายความร้อนที่เข้ากันได้");
        }
    }


    @Override
    public void checkStorageCompatibility(List<BuildPart<StorageDrive>> storageDriveParts, Motherboard motherboard, String nvmeInterfaceId, List<String> sataInterfaceIds, List<String> warnings, List<String> errors) {
        // === [CHECK-5.7] ตรวจสอบไดรฟ์เก็บข้อมูล (Storage) กับช่องบนเมนบอร์ด ===
        if (storageDriveParts == null || storageDriveParts.isEmpty()) {
            warnings.add("ไม่ได้เลือกไดรฟ์เก็บข้อมูล: ระบบปฏิบัติการและโปรแกรมต่างๆ ต้องถูกติดตั้งบนไดรฟ์เก็บข้อมูล");
            return;
        }

        // 5.7.1: นับจำนวนไดรฟ์ NVMe และ SATA ที่ต้องการ
        int nvmeCount = 0;
        int sataCount = 0;
        for (BuildPart<StorageDrive> part : storageDriveParts) {
            StorageDrive drive = part.getComponent();
            int quantity = part.getQuantity();
            String interfaceId = drive.getStorageInterface().getId();

            if (nvmeInterfaceId != null && nvmeInterfaceId.equals(interfaceId)) {
                nvmeCount += quantity;
            } else if (sataInterfaceIds != null && sataInterfaceIds.contains(interfaceId)) {
                sataCount += quantity;
            }
        }
        // 5.7.2: ตรวจสอบเทียบกับจำนวนช่องบนเมนบอร์ด
        if (nvmeCount > motherboard.getM2_slot_count()) {
            errors.add(String.format("ช่อง M.2 ไม่พอ: เมนบอร์ดมีช่อง M.2 %d ช่อง แต่เลือกไดรฟ์ NVMe %d ตัว",
                    motherboard.getM2_slot_count(), nvmeCount));
        }
        if (sataCount > motherboard.getSata_port_count()) {
            errors.add(String.format("ช่อง SATA ไม่พอ: เมนบอร์ดมีช่อง SATA %d ช่อง แต่เลือกไดรฟ์แบบ SATA %d ตัว",
                    motherboard.getSata_port_count(), sataCount));
        }
        // 5.7.3: เพิ่มคำเตือนเกี่ยวกับ M.2 และ SATA ที่อาจใช้ช่องทางร่วมกัน
        if (nvmeCount > 0 && sataCount > 0) {
            warnings.add("คำเตือน: การใช้งานช่อง M.2 บางครั้งอาจปิดการทำงานของพอร์ต SATA บางพอร์ต กรุณาตรวจสอบคู่มือของเมนบอร์ด");
        }
    }

    @Override
    public void checkStorageAndCaseBays(List<BuildPart<StorageDrive>> storageDriveParts, Case computerCase, List<String> errors) {
        // === [CHECK-5.8] ตรวจสอบไดรฟ์เก็บข้อมูล (Storage) กับช่องในเคส (Bays) ===
        if (storageDriveParts == null || storageDriveParts.isEmpty()) {
            return;
        }

        // 5.8.1: นับจำนวนไดรฟ์ขนาด 3.5" และ 2.5" ที่ต้องการ
        long required_3_5_inch_bays = 0;
        long required_2_5_inch_bays = 0;

        for (BuildPart<StorageDrive> part : storageDriveParts) {
            StorageDrive drive = part.getComponent();
            if (drive.getFormFactor() != null && drive.getFormFactor().getName() != null) {
                String formFactorName = drive.getFormFactor().getName().toUpperCase();
                // เราจะนับเฉพาะไดรฟ์ที่ไม่ใช่ M.2 สำหรับการติดตั้งใน Bay
                if (formFactorName.contains("3.5")) {
                    required_3_5_inch_bays += part.getQuantity();
                } else if (formFactorName.contains("2.5")) {
                    required_2_5_inch_bays += part.getQuantity();
                }
            }
        }

        // 5.8.2: ตรวจสอบเทียบกับจำนวนช่องในเคส
        if (required_3_5_inch_bays > computerCase.getBays_3_5_inch()) {
            errors.add(String.format(
                    "ช่องใส่ไดรฟ์ 3.5\" ไม่พอ: เคส '%s' มีช่อง 3.5\" เพียง %d ช่อง แต่คุณเลือกไดรฟ์ขนาด 3.5\" ทั้งหมด %d ตัว",
                    computerCase.getName(), computerCase.getBays_3_5_inch(), required_3_5_inch_bays
            ));
        }

        if (required_2_5_inch_bays > computerCase.getBays_2_5_inch()) {
            errors.add(String.format(
                    "ช่องใส่ไดรฟ์ 2.5\" ไม่พอ: เคส '%s' มีช่อง 2.5\" เพียง %d ช่อง แต่คุณเลือกไดรฟ์ขนาด 2.5\" ทั้งหมด %d ตัว",
                    computerCase.getName(), computerCase.getBays_2_5_inch(), required_2_5_inch_bays
            ));
        }
    }

    @Override
    public int calculateTotalWattage(Cpu cpu, Motherboard motherboard, List<BuildPart<RamKit>> ramKitParts, List<BuildPart<Gpu>> gpuParts, Cooler cooler) {
        // === [CHECK-6.1] คำนวณการใช้พลังงานรวมของระบบ ===
        int wattage = 0;
        wattage += cpu.getWattage();
        wattage += motherboard.getWattage();
        if (cooler != null) {
            wattage += cooler.getWattage();
        }
        wattage += ramKitParts.stream().mapToInt(part -> part.getQuantity() * part.getComponent().getWattage()).sum();
        if (gpuParts != null) {
            wattage += gpuParts.stream().mapToInt(part -> part.getQuantity() * part.getComponent().getWattage()).sum();
        }
        wattage += 75; // ค่าประมาณสำหรับอุปกรณ์อื่นๆ เช่น พัดลม, ไดรฟ์
        return wattage;
    }

    @Override
    public void checkPsuWattage(Psu psu, int totalWattage, List<String> errors, List<String> warnings) {
        // === [CHECK-6.2] ตรวจสอบกำลังไฟของ PSU เทียบกับที่ระบบต้องการ ===
        if (psu.getWattage() < totalWattage) {
            // กรณีไฟไม่พอ (Error)
            errors.add(String.format("กำลังไฟไม่พอ: Power Supply มีกำลังไฟ %dW ซึ่งไม่เพียงพอต่อการใช้งานของระบบที่ประมาณ %dW", psu.getWattage(), totalWattage));
        } else if (psu.getWattage() < totalWattage * 1.25) {
            // กรณีไฟพอดีเกินไป (Warning) - ควรมี Headroom อย่างน้อย 25%
            warnings.add(String.format("คำเตือนกำลังไฟ: Power Supply (%dW) มีกำลังไฟใกล้เคียงกับที่ระบบต้องการ (%dW) แนะนำให้ใช้ PSU ที่มีกำลังไฟสูงกว่านี้เพื่อความเสถียรและการอัปเกรดในอนาคต", psu.getWattage(), totalWattage));
        }
    }
}