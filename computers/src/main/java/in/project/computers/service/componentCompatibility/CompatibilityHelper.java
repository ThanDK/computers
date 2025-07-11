package in.project.computers.service.componentCompatibility;

import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;

import java.util.List;

public interface CompatibilityHelper {

    void checkCpuAndMotherboard(Cpu cpu, Motherboard motherboard, List<String> errors);

    void checkRamCompatibility(List<BuildPart<RamKit>> ramKitParts, Motherboard motherboard, List<String> errors);

    void checkFormFactorCompatibility(Motherboard motherboard, Case computerCase, List<String> errors);

    void checkPsuFormFactor(Psu psu, Case computerCase, List<String> errors); // ADDED

    void checkGpuCompatibility(List<BuildPart<Gpu>> gpuParts, Motherboard motherboard, Case computerCase, List<String> errors);

    void checkCoolerCompatibility(Cooler cooler, Motherboard motherboard, Case computerCase, List<String> warnings, List<String> errors);

    void checkStorageCompatibility(List<BuildPart<StorageDrive>> storageDriveParts, Motherboard motherboard, String nvmeInterfaceId, List<String> sataInterfaceIds, List<String> warnings, List<String> errors);

    int calculateTotalWattage(Cpu cpu, Motherboard motherboard, List<BuildPart<RamKit>> ramKitParts, List<BuildPart<Gpu>> gpuParts, Cooler cooler);

    void checkPsuWattage(Psu psu, int totalWattage, List<String> errors, List<String> warnings);

    void checkStorageAndCaseBays(List<BuildPart<StorageDrive>> storageDriveParts, Case computerCase, List<String> errors);
}