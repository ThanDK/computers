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
        if (!Objects.equals(cpu.getSocket().getId(), motherboard.getSocket().getId())) {
            errors.add(String.format("CPU เข้ากันไม่ได้: '%s' (Socket %s) ไม่สามารถติดตั้งบนเมนบอร์ด '%s' (Socket %s) ได้",
                    cpu.getName(), cpu.getSocket().getName(), motherboard.getName(), motherboard.getSocket().getName()));
        }
    }

    @Override
    public void checkRamCompatibility(List<BuildPart<RamKit>> ramKitParts, Motherboard motherboard, List<String> errors) {
        int totalSticksRequired = ramKitParts.stream()
                .mapToInt(part -> part.getQuantity() * part.getComponent().getModuleCount())
                .sum();
        if (totalSticksRequired > motherboard.getRam_slot_count()) {
            errors.add(String.format("ช่อง RAM ไม่พอ: เมนบอร์ดมีช่อง RAM %d ช่อง แต่คุณเลือก RAM ทั้งหมด %d แถว",
                    motherboard.getRam_slot_count(), totalSticksRequired));
        }

        int totalRamGb = ramKitParts.stream()
                .mapToInt(part -> part.getQuantity() * part.getComponent().getRam_size_gb())
                .sum();
        if (totalRamGb > motherboard.getMax_ram_gb()) {
            errors.add(String.format("ขนาด RAM เกิน: เมนบอร์ดรองรับ RAM สูงสุด %dGB, แต่คุณเลือกทั้งหมด %dGB",
                    motherboard.getMax_ram_gb(), totalRamGb));
        }

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
        if (gpuParts == null || gpuParts.isEmpty()) return;

        int totalGpuCount = gpuParts.stream().mapToInt(BuildPart::getQuantity).sum();
        if (totalGpuCount > motherboard.getPcie_x16_slot_count()) {
            errors.add(String.format("ช่อง GPU ไม่พอ: เมนบอร์ดมีช่อง PCIe x16 เพียง %d ช่อง แต่เลือกการ์ดจอ %d ตัว",
                    motherboard.getPcie_x16_slot_count(), totalGpuCount));
        }

        for (BuildPart<Gpu> part : gpuParts) {
            Gpu gpu = part.getComponent();
            if (gpu.getLength_mm() > computerCase.getMax_gpu_length_mm()) {
                errors.add(String.format("ขนาดไม่พอดี: การ์ดจอ '%s' (ยาว %dmm) ยาวเกินไปสำหรับเคส '%s' (รองรับสูงสุด %dmm)",
                        gpu.getName(), gpu.getLength_mm(), computerCase.getName(), computerCase.getMax_gpu_length_mm()));
            }
        }
    }

    // --- MODIFIED METHOD ---
    @Override
    public void checkCoolerCompatibility(Cooler cooler, Motherboard motherboard, Case computerCase, List<String> warnings, List<String> errors) {
        if (cooler != null) {
            // This check applies to all coolers (Air and AIO)
            List<String> supportedSocketIds = cooler.getSupportedSockets().stream().map(Socket::getId).toList();
            if (!supportedSocketIds.contains(motherboard.getSocket().getId())) {
                errors.add(String.format("ชุดระบายความร้อนเข้ากันไม่ได้: '%s' ไม่รองรับ Socket ของเมนบอร์ด (%s)",
                        cooler.getName(), motherboard.getSocket().getName()));
            }

            // Check for AIO (liquid cooler) compatibility
            if (cooler.getRadiatorSize_mm() > 0) {
                List<Integer> supportedSizes = Optional.ofNullable(computerCase.getSupportedRadiatorSizesMm()).orElse(Collections.emptyList());
                if (!supportedSizes.contains(cooler.getRadiatorSize_mm())) {
                    errors.add(String.format("ขนาดไม่พอดี: ชุดระบายความร้อนด้วยน้ำ '%s' (ขนาดหม้อน้ำ %dmm) ไม่สามารถติดตั้งในเคส '%s' ได้",
                            cooler.getName(), cooler.getRadiatorSize_mm(), computerCase.getName()));
                }
                // ADDED: A warning for potential RAM clearance issues with AIOs.
                warnings.add("คำเตือน: การติดตั้งชุดระบายความร้อนด้วยน้ำ อาจมีปัญหากับ RAM ที่มีฮีทซิงค์สูง กรุณาตรวจสอบระยะห่างของเคสและเมนบอร์ด");
            }
            // Check for Air Cooler compatibility
            else {
                if (cooler.getHeight_mm() > computerCase.getMax_cooler_height_mm()) {
                    errors.add(String.format("ขนาดไม่พอดี: ชุดระบายความร้อน '%s' (สูง %dmm) สูงเกินไปสำหรับเคส '%s' (รองรับสูงสุด %dmm)",
                            cooler.getName(), cooler.getHeight_mm(), computerCase.getName(), computerCase.getMax_cooler_height_mm()));
                }
            }
        } else {
            warnings.add("ไม่ได้เลือกชุดระบายความร้อน CPU: กรุณาตรวจสอบว่า CPU ของคุณมีชุดระบายความร้อนแถมมาด้วย หรือเลือกชุดระบายความร้อนที่เข้ากันได้");
        }
    }

    // --- MODIFIED METHOD ---
    @Override
    public void checkStorageCompatibility(List<BuildPart<StorageDrive>> storageDriveParts, Motherboard motherboard, String nvmeInterfaceId, List<String> sataInterfaceIds, List<String> warnings, List<String> errors) {
        if (storageDriveParts == null || storageDriveParts.isEmpty()) {
            warnings.add("ไม่ได้เลือกไดรฟ์เก็บข้อมูล: ระบบปฏิบัติการและโปรแกรมต่างๆ ต้องถูกติดตั้งบนไดรฟ์เก็บข้อมูล");
            return;
        }

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

        if (nvmeCount > motherboard.getM2_slot_count()) {
            errors.add(String.format("ช่อง M.2 ไม่พอ: เมนบอร์ดมีช่อง M.2 %d ช่อง แต่เลือกไดรฟ์ NVMe %d ตัว",
                    motherboard.getM2_slot_count(), nvmeCount));
        }
        if (sataCount > motherboard.getSata_port_count()) {
            errors.add(String.format("ช่อง SATA ไม่พอ: เมนบอร์ดมีช่อง SATA %d ช่อง แต่เลือกไดรฟ์แบบ SATA %d ตัว",
                    motherboard.getSata_port_count(), sataCount));
        }

        // ADDED: A warning about potential M.2 and SATA port conflicts.
        if (nvmeCount > 0 && sataCount > 0) {
            warnings.add("คำเตือน: การใช้งานช่อง M.2 บางครั้งอาจปิดการทำงานของพอร์ต SATA บางพอร์ต กรุณาตรวจสอบคู่มือของเมนบอร์ด");
        }
    }

    @Override
    public int calculateTotalWattage(Cpu cpu, Motherboard motherboard, List<BuildPart<RamKit>> ramKitParts, List<BuildPart<Gpu>> gpuParts, Cooler cooler) {
        int wattage = 0;
        wattage += cpu.getWattage();
        wattage += motherboard.getWattage();
        if (cooler != null) {
            wattage += cooler.getWattage();
        }

        wattage += ramKitParts.stream()
                .mapToInt(part -> part.getQuantity() * part.getComponent().getWattage())
                .sum();

        if (gpuParts != null) {
            wattage += gpuParts.stream()
                    .mapToInt(part -> part.getQuantity() * part.getComponent().getWattage())
                    .sum();
        }

        wattage += 75; // A base value for other peripherals
        return wattage;
    }
    // --- NEW METHOD ---
    @Override
    public void checkStorageAndCaseBays(List<BuildPart<StorageDrive>> storageDriveParts, Case computerCase, List<String> errors) {
        if (storageDriveParts == null || storageDriveParts.isEmpty()) {
            return;
        }

        // Count how many drives of each form factor are needed
        long required_3_5_inch_bays = 0;
        long required_2_5_inch_bays = 0;

        for (BuildPart<StorageDrive> part : storageDriveParts) {
            StorageDrive drive = part.getComponent();
            if (drive.getFormFactor() != null && drive.getFormFactor().getName() != null) {
                String formFactorName = drive.getFormFactor().getName().toUpperCase();
                // We only care about non-M.2 drives for case bays
                if (formFactorName.contains("3.5")) {
                    required_3_5_inch_bays += part.getQuantity();
                } else if (formFactorName.contains("2.5")) {
                    required_2_5_inch_bays += part.getQuantity();
                }
            }
        }

        // Check against case specifications
        if (required_3_5_inch_bays > computerCase.getBays_3_5_inch()) {
            errors.add(String.format(
                    "ช่องใส่ไดรฟ์ 3.5\" ไม่พอ: เคส '%s' มีช่อง 3.5\" เพียง %d ช่อง แต่คุณเลือกไดรฟ์ขนาด 3.5\" ทั้งหมด %d ตัว",
                    computerCase.getName(), computerCase.getBays_3_5_inch(), required_3_5_inch_bays
            ));
        }

        // Note: Many 3.5" bays can also fit 2.5" drives. A more advanced check could have "combo bays".
        // For now, we check the dedicated 2.5" bays, which is a safe and correct approach.
        if (required_2_5_inch_bays > computerCase.getBays_2_5_inch()) {
            errors.add(String.format(
                    "ช่องใส่ไดรฟ์ 2.5\" ไม่พอ: เคส '%s' มีช่อง 2.5\" เพียง %d ช่อง แต่คุณเลือกไดรฟ์ขนาด 2.5\" ทั้งหมด %d ตัว",
                    computerCase.getName(), computerCase.getBays_2_5_inch(), required_2_5_inch_bays
            ));
        }
    }
    @Override
    public void checkPsuWattage(Psu psu, int totalWattage, List<String> errors, List<String> warnings) {
        if (psu.getWattage() < totalWattage) {
            errors.add(String.format("กำลังไฟไม่พอ: Power Supply มีกำลังไฟ %dW ซึ่งไม่เพียงพอต่อการใช้งานของระบบที่ประมาณ %dW", psu.getWattage(), totalWattage));
        } else if (psu.getWattage() < totalWattage * 1.25) {
            warnings.add(String.format("คำเตือนกำลังไฟ: Power Supply (%dW) มีกำลังไฟใกล้เคียงกับที่ระบบต้องการ (%dW) แนะนำให้ใช้ PSU ที่มีกำลังไฟสูงกว่านี้เพื่อความเสถียรและการอัปเกรดในอนาคต", psu.getWattage(), totalWattage));
        }
    }
}