package in.project.computers.service.componentCompatibility;

import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;


@Getter
@Builder
public class CompatibilityContext {

    private final Cpu selectedCpu;
    private final Motherboard selectedMotherboard;
    private final Case selectedCase;
    private final Cooler selectedCooler;
    private final Psu selectedPsu;
    private final List<BuildPart<RamKit>> selectedRamKits;
    private final List<BuildPart<Gpu>> selectedGpus;
    private final List<BuildPart<StorageDrive>> selectedStorageDrives;

    private final String requiredSocketId;
    private final String requiredRamTypeId;
    private final List<String> supportedMoboFormFactorIds; // From Case
    private final List<String> supportedPsuFormFactorIds;  // From Case
    private final Integer maxGpuLengthMm;
    private final Integer maxCoolerHeightMm;
    private final List<Integer> supportedRadiatorSizesMm;
    private final int requiredWattage;

    private final int totalRamModules;
    private final int totalGpuCount;
}