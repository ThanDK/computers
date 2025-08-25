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

@Component
@RequiredArgsConstructor
@Slf4j
public class CompatibilityHelperImpl implements CompatibilityHelper {

    @Override
    public void checkCpuAndMotherboard(Cpu cpu, Motherboard motherboard, List<String> errors) {
        // === [CHECK-4.1] ตรวจสอบ Socket ของ CPU และ Motherboard ว่าตรงกันหรือไม่ ===
        if (!Objects.equals(cpu.getSocket().getId(), motherboard.getSocket().getId())) {
            errors.add(String.format("CPU และ Motherboard ไม่เข้ากัน: CPU '%s' (Socket %s) ไม่สามารถใช้กับ Motherboard '%s' (Socket %s) ได้",
                    cpu.getName(), cpu.getSocket().getName(), motherboard.getName(), motherboard.getSocket().getName()));
        }
    }

    @Override
    public void checkRamCompatibility(List<BuildPart<RamKit>> ramKitParts, Motherboard motherboard, List<String> errors) {
        // === [CHECK-4.2.1] ตรวจสอบจำนวนแถว RAM ทั้งหมดเทียบกับช่องบนเมนบอร์ด ===
        int totalSticksRequired = ramKitParts.stream()
                .mapToInt(part -> part.getQuantity() * part.getComponent().getModuleCount())
                .sum();
        if (totalSticksRequired > motherboard.getRam_slot_count()) {
            errors.add(String.format("ช่อง RAM ไม่เพียงพอ: Motherboard มี %d ช่อง แต่ต้องการติดตั้ง RAM ทั้งหมด %d แถว",
                    motherboard.getRam_slot_count(), totalSticksRequired));
        }
        // === [CHECK-4.2.2] ตรวจสอบขนาด RAM รวม (GB) เทียบกับขนาดสูงสุดที่เมนบอร์ดรองรับ ===
        int totalRamGb = ramKitParts.stream()
                .mapToInt(part -> part.getQuantity() * part.getComponent().getRam_size_gb())
                .sum();
        if (totalRamGb > motherboard.getMax_ram_gb()) {
            errors.add(String.format("ความจุ RAM เกินกำหนด: Motherboard รองรับสูงสุด %dGB แต่เลือกติดตั้งทั้งหมด %dGB",
                    motherboard.getMax_ram_gb(), totalRamGb));
        }
        // === [CHECK-4.2.3] ตรวจสอบประเภทของ RAM (เช่น DDR4, DDR5) ว่าตรงกับที่เมนบอร์ดรองรับหรือไม่ ===
        ramKitParts.stream()
                .map(BuildPart::getComponent)
                .filter(ram -> !Objects.equals(ram.getRamType().getId(), motherboard.getRamType().getId()))
                .map(ram -> String.format("ประเภท RAM ไม่ตรงกัน: RAM '%s' (%s) ไม่สามารถใช้กับ Motherboard '%s' (รองรับ %s) ได้",
                        ram.getName(), ram.getRamType().getName(), motherboard.getName(), motherboard.getRamType().getName()))
                .distinct()
                .forEach(errors::add);
    }

    @Override
    public void checkFormFactorCompatibility(Motherboard motherboard, Case computerCase, List<String> errors) {
        // === [CHECK-4.3] ตรวจสอบขนาด Motherboard (Form Factor) ว่าเข้ากับเคสได้หรือไม่ ===
        List<String> supportedIds = computerCase.getSupportedFormFactors().stream()
                .map(FormFactor::getId)
                .toList();

        if (!supportedIds.contains(motherboard.getFormFactor().getId())) {
            errors.add(String.format("ขนาด Motherboard ไม่พอดี: Motherboard ขนาด %s ไม่สามารถติดตั้งในเคสที่เลือกได้",
                    motherboard.getFormFactor().getName()));
        }
    }

    @Override
    public void checkPsuFormFactor(Psu psu, Case computerCase, List<String> errors) {
        // === [CHECK-4.4] ตรวจสอบขนาด PSU (Form Factor) ว่าเข้ากับเคสได้หรือไม่ ===
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
            errors.add(String.format("ขนาด PSU ไม่พอดี: PSU ขนาด %s ไม่สามารถติดตั้งในเคสที่เลือกได้",
                    psu.getFormFactor().getName()));
        }
    }

    @Override
    public void checkGpuCompatibility(List<BuildPart<Gpu>> gpuParts, Motherboard motherboard, Case computerCase, List<String> errors) {
        if (gpuParts == null || gpuParts.isEmpty()) return;

        // === [CHECK-4.5.1] ตรวจสอบจำนวนการ์ดจอทั้งหมดเทียบกับช่อง PCIe x16 บนเมนบอร์ด ===
        int totalGpuCount = gpuParts.stream().mapToInt(BuildPart::getQuantity).sum();
        if (totalGpuCount > motherboard.getPcie_x16_slot_count()) {
            errors.add(String.format("ช่องติดตั้ง GPU ไม่พอ: Motherboard มีช่อง PCIe x16 เพียง %d ช่อง แต่เลือกติดตั้ง GPU %d ตัว",
                    motherboard.getPcie_x16_slot_count(), totalGpuCount));
        }
        // === [CHECK-4.5.2] ตรวจสอบความยาวของการ์ดจอแต่ละตัวเทียบกับพื้นที่ในเคส ===
        for (BuildPart<Gpu> part : gpuParts) {
            Gpu gpu = part.getComponent();
            if (gpu.getLength_mm() > computerCase.getMax_gpu_length_mm()) {
                errors.add(String.format("GPU ยาวเกินไป: GPU '%s' (ยาว %dmm) ยาวเกินกว่าที่เคสรองรับ (สูงสุด %dmm)",
                        gpu.getName(), gpu.getLength_mm(), computerCase.getMax_gpu_length_mm()));
            }
        }
    }

    @Override
    public void checkCoolerCompatibility(Cooler cooler, Motherboard motherboard, Case computerCase, List<String> warnings, List<String> errors) {
        if (cooler != null) {
            // === [CHECK-4.6.1] ตรวจสอบ Socket ว่าเข้ากับเมนบอร์ดได้หรือไม่ (สำหรับ Cooler ทุกประเภท) ===
            List<String> supportedSocketIds = cooler.getSupportedSockets().stream().map(Socket::getId).toList();
            if (!supportedSocketIds.contains(motherboard.getSocket().getId())) {
                errors.add(String.format("Cooler ไม่รองรับ Socket: Cooler '%s' ไม่สามารถติดตั้งบน Motherboard (Socket %s) ได้",
                        cooler.getName(), motherboard.getSocket().getName()));
            }

            // === [CHECK-4.6.2] ตรวจสอบสำหรับชุดระบายความร้อนด้วยน้ำ (AIO) ===
            boolean isAioCooler = cooler.getRadiatorSize_mm() >= 120;
            if (isAioCooler) {
                List<Integer> supportedSizes = Optional.ofNullable(computerCase.getSupportedRadiatorSizesMm()).orElse(Collections.emptyList());
                if (!supportedSizes.contains(cooler.getRadiatorSize_mm())) {
                    errors.add(String.format("Case ไม่รองรับขนาดหม้อน้ำ: Cooler '%s' (หม้อน้ำ %dmm) ไม่สามารถติดตั้งในเคสได้",
                            cooler.getName(), cooler.getRadiatorSize_mm()));
                }
                warnings.add("ข้อควรระวัง (AIO Cooler): การติดตั้งชุดระบายความร้อนด้วยน้ำอาจมีปัญหากับ RAM ที่มีฮีทซิงค์สูง กรุณาตรวจสอบระยะห่างอีกครั้ง");
            } else {
                // === [CHECK-4.6.3] ตรวจสอบสำหรับชุดระบายความร้อนด้วยลม (Air Cooler) ===
                if (cooler.getHeight_mm() > computerCase.getMax_cooler_height_mm()) {
                    errors.add(String.format("Cooler สูงเกินไป: Cooler '%s' (สูง %dmm) สูงเกินกว่าที่เคสรองรับ (สูงสุด %dmm)",
                            cooler.getName(), cooler.getHeight_mm(), computerCase.getMax_cooler_height_mm()));
                }
            }
        } else {
            // === [CHECK-4.6.4] กรณีที่ผู้ใช้ไม่ได้เลือก Cooler ===
            warnings.add("ไม่ได้เลือก CPU Cooler: กรุณาตรวจสอบว่า CPU ของคุณมีชุดระบายความร้อนแถมมาด้วย หรือเลือกซื้อ Cooler เพิ่มเติม");
        }
    }


    @Override
    public void checkStorageCompatibility(List<BuildPart<StorageDrive>> storageDriveParts, Motherboard motherboard, String nvmeInterfaceId, List<String> sataInterfaceIds, List<String> warnings, List<String> errors) {
        if (storageDriveParts == null || storageDriveParts.isEmpty()) {
            warnings.add("ไม่ได้เลือก Storage Drive: ระบบปฏิบัติการและโปรแกรมต่างๆ ต้องถูกติดตั้งบน Storage Drive");
            return;
        }

        // === [CHECK-4.7.1] นับจำนวนไดรฟ์ NVMe และ SATA ที่ต้องการ ===
        int nvmeCount = 0;
        int sataCount = 0;
        for (BuildPart<StorageDrive> part : storageDriveParts) {
            String interfaceId = part.getComponent().getStorageInterface().getId();
            if (nvmeInterfaceId != null && nvmeInterfaceId.equals(interfaceId)) {
                nvmeCount += part.getQuantity();
            } else if (sataInterfaceIds != null && sataInterfaceIds.contains(interfaceId)) {
                sataCount += part.getQuantity();
            }
        }
        // === [CHECK-4.7.2] ตรวจสอบเทียบกับจำนวนช่องบนเมนบอร์ด ===
        if (nvmeCount > motherboard.getM2_slot_count()) {
            errors.add(String.format("ช่อง M.2 ไม่พอ: Motherboard มี %d ช่อง แต่ต้องการไดรฟ์ NVMe %d ตัว",
                    motherboard.getM2_slot_count(), nvmeCount));
        }
        if (sataCount > motherboard.getSata_port_count()) {
            errors.add(String.format("พอร์ต SATA ไม่พอ: Motherboard มี %d พอร์ต แต่ต้องการไดรฟ์ SATA %d ตัว",
                    motherboard.getSata_port_count(), sataCount));
        }
        // === [CHECK-4.7.3] เพิ่มคำเตือนเกี่ยวกับ M.2 และ SATA ที่อาจใช้ช่องทางร่วมกัน ===
        if (nvmeCount > 0 && motherboard.getSata_port_count() > 0) {
            warnings.add("ข้อควรระวัง (M.2/SATA): การใช้งานช่อง M.2 บางครั้งอาจปิดการทำงานของพอร์ต SATA บางพอร์ต กรุณาตรวจสอบคู่มือของ Motherboard");
        }
    }

    @Override
    public void checkStorageAndCaseBays(List<BuildPart<StorageDrive>> storageDriveParts, Case computerCase, List<String> errors) {
        if (storageDriveParts == null || storageDriveParts.isEmpty()) {
            return;
        }

        // === [CHECK-4.8.1] นับจำนวนไดรฟ์ขนาด 3.5" และ 2.5" ที่ต้องการ ===
        long required_3_5_inch_bays = 0;
        long required_2_5_inch_bays = 0;
        for (BuildPart<StorageDrive> part : storageDriveParts) {
            if (part.getComponent().getFormFactor() != null && part.getComponent().getFormFactor().getName() != null) {
                String formFactorName = part.getComponent().getFormFactor().getName().toUpperCase();
                if (formFactorName.contains("3.5")) {
                    required_3_5_inch_bays += part.getQuantity();
                } else if (formFactorName.contains("2.5")) {
                    required_2_5_inch_bays += part.getQuantity();
                }
            }
        }

        // === [CHECK-4.8.2] ตรวจสอบเทียบกับจำนวนช่องในเคส ===
        if (required_3_5_inch_bays > computerCase.getBays_3_5_inch()) {
            errors.add(String.format("ช่อง 3.5\" ไม่พอ: Case มี %d ช่อง แต่ต้องการติดตั้งไดรฟ์ 3.5\" ทั้งหมด %d ตัว",
                    computerCase.getBays_3_5_inch(), required_3_5_inch_bays));
        }
        if (required_2_5_inch_bays > computerCase.getBays_2_5_inch()) {
            errors.add(String.format("ช่อง 2.5\" ไม่พอ: Case มี %d ช่อง แต่ต้องการติดตั้งไดรฟ์ 2.5\" ทั้งหมด %d ตัว",
                    computerCase.getBays_2_5_inch(), required_2_5_inch_bays));
        }
    }

    @Override
    public int calculateTotalWattage(Cpu cpu, Motherboard motherboard, List<BuildPart<RamKit>> ramKitParts, List<BuildPart<Gpu>> gpuParts, Cooler cooler) {
        // === [CHECK-5.1] คำนวณการใช้พลังงานรวมของระบบ ===
        int wattage = 0;
        wattage += cpu.getWattage();
        wattage += motherboard.getWattage();
        if (cooler != null) {
            wattage += cooler.getWattage();
        }
        wattage += ramKitParts.stream().mapToInt(p -> p.getQuantity() * p.getComponent().getWattage()).sum();
        if (gpuParts != null) {
            wattage += gpuParts.stream().mapToInt(p -> p.getQuantity() * p.getComponent().getWattage()).sum();
        }
        wattage += 75; // ค่าประมาณสำหรับอุปกรณ์อื่นๆ เช่น พัดลม, ไดรฟ์
        return wattage;
    }

    @Override
    public void checkPsuWattage(Psu psu, int totalWattage, List<String> errors, List<String> warnings) {
        // === [CHECK-5.2] ตรวจสอบกำลังไฟของ PSU เทียบกับที่ระบบต้องการ ===
        if (psu.getWattage() < totalWattage) {
            errors.add(String.format("กำลังไฟไม่เพียงพอ: ระบบต้องการไฟประมาณ %dW แต่ PSU '%s' จ่ายไฟได้เพียง %dW",
                    totalWattage, psu.getName(), psu.getWattage()));
        } else if (psu.getWattage() < totalWattage * 1.25) {
            warnings.add(String.format("คำเตือนกำลังไฟ: PSU '%s' (%dW) อาจไม่เพียงพอสำหรับระบบที่ต้องการ %dW เมื่อใช้งานหนักหรือเพื่อการอัปเกรดในอนาคต (แนะนำให้มีกำลังไฟสำรองอย่างน้อย 25%%)",
                    psu.getName(), psu.getWattage(), totalWattage));
        }
    }
}