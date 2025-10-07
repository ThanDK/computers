package in.project.computers.service.componentCompatibility;

import in.project.computers.DTO.builds.CompatibleComponentsRequest;
import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;
import in.project.computers.entity.lookup.FormFactor;
import in.project.computers.service.util.ComponentFetcher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompatibilityContextServiceImpl implements CompatibilityContextService {

    private final ComponentFetcher componentFetcher;

    @Override
    public CompatibilityContext buildContext(CompatibleComponentsRequest request) {
        // 1. Fetch all selected component entities from the database
        Cpu cpu = componentFetcher.fetchComponentEntity(request.getCpuId(), Cpu.class);
        Motherboard motherboard = componentFetcher.fetchComponentEntity(request.getMotherboardId(), Motherboard.class);
        Case computerCase = componentFetcher.fetchComponentEntity(request.getCaseId(), Case.class);
        Cooler cooler = componentFetcher.fetchComponentEntity(request.getCoolerId(), Cooler.class);
        Psu psu = componentFetcher.fetchComponentEntity(request.getPsuId(), Psu.class);

        List<BuildPart<RamKit>> ramKits = componentFetcher.fetchBuildParts(request.getRamKits(), RamKit.class);
        List<BuildPart<Gpu>> gpus = componentFetcher.fetchBuildParts(request.getGpus(), Gpu.class);
        List<BuildPart<StorageDrive>> storageDrives = componentFetcher.fetchBuildParts(request.getStorageDrives(), StorageDrive.class);

        // 2. Calculate total wattage of selected parts
        int currentWattage = calculateWattage(cpu, motherboard, cooler, ramKits, gpus);
        int requiredWattage = currentWattage + 75; // 75W buffer for peripherals

        // 3. Extract and centralize all compatibility constraints
        CompatibilityContext.CompatibilityContextBuilder contextBuilder = CompatibilityContext.builder();

        contextBuilder.selectedCpu(cpu)
                .selectedMotherboard(motherboard)
                .selectedCase(computerCase)
                .selectedCooler(cooler)
                .selectedPsu(psu)
                .selectedRamKits(ramKits)
                .selectedGpus(gpus)
                .selectedStorageDrives(storageDrives)
                .requiredWattage(requiredWattage);

        if (cpu != null && cpu.getSocket() != null) {
            contextBuilder.requiredSocketId(cpu.getSocket().getId());
        }

        if (motherboard != null) {
            if (motherboard.getSocket() != null) contextBuilder.requiredSocketId(motherboard.getSocket().getId());
            if (motherboard.getRamType() != null) contextBuilder.requiredRamTypeId(motherboard.getRamType().getId());
            if (motherboard.getFormFactor() != null) {
                // This is a direct constraint if a motherboard is selected
                contextBuilder.supportedMoboFormFactorIds(Collections.singletonList(motherboard.getFormFactor().getId()));
            }
        }

        if (!CollectionUtils.isEmpty(ramKits)) {
            // Assume all ram kits in a build are of the same type
            ramKits.stream().findFirst().ifPresent(ramKitBuildPart -> {
                RamKit ram = ramKitBuildPart.getComponent();
                if (ram != null && ram.getRamType() != null) {
                    contextBuilder.requiredRamTypeId(ram.getRamType().getId());
                }
            });
            contextBuilder.totalRamModules(ramKits.stream().mapToInt(p -> p.getQuantity() * p.getComponent().getModuleCount()).sum());
        }

        if(!CollectionUtils.isEmpty(gpus)) {
            contextBuilder.totalGpuCount(gpus.stream().mapToInt(BuildPart::getQuantity).sum());
        }

        if (computerCase != null) {
            contextBuilder.maxGpuLengthMm(computerCase.getMax_gpu_length_mm());
            contextBuilder.maxCoolerHeightMm(computerCase.getMax_cooler_height_mm());

            if (!CollectionUtils.isEmpty(computerCase.getSupportedFormFactors())) {
                contextBuilder.supportedMoboFormFactorIds(
                        computerCase.getSupportedFormFactors().stream().map(FormFactor::getId).collect(Collectors.toList())
                );
            }
            if (!CollectionUtils.isEmpty(computerCase.getSupportedPsuFormFactors())) {
                contextBuilder.supportedPsuFormFactorIds(
                        computerCase.getSupportedPsuFormFactors().stream().map(FormFactor::getId).collect(Collectors.toList())
                );
            }
            contextBuilder.supportedRadiatorSizesMm(computerCase.getSupportedRadiatorSizesMm());
        }

        return contextBuilder.build();
    }

    private int calculateWattage(Cpu cpu, Motherboard motherboard, Cooler cooler, List<BuildPart<RamKit>> ramKits, List<BuildPart<Gpu>> gpus) {
        int wattage = 0;
        wattage += Optional.ofNullable(cpu).map(Cpu::getWattage).orElse(0);
        wattage += Optional.ofNullable(motherboard).map(Motherboard::getWattage).orElse(0);
        wattage += Optional.ofNullable(cooler).map(Cooler::getWattage).orElse(0);
        wattage += ramKits.stream().mapToInt(p -> p.getQuantity() * p.getComponent().getWattage()).sum();
        wattage += gpus.stream().mapToInt(p -> p.getQuantity() * p.getComponent().getWattage()).sum();
        return wattage;
    }
}