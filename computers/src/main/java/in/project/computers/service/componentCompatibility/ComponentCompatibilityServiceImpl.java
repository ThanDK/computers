package in.project.computers.service.componentCompatibility;

import in.project.computers.dto.builds.CompatibilityResult;
import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.lookup.StorageInterface;
import in.project.computers.repository.lookup.StorageInterfaceRepository;
import in.project.computers.repository.generalRepo.ComputerBuildRepository;
import in.project.computers.service.userAuthenticationService.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComponentCompatibilityServiceImpl implements ComponentCompatibilityService {

    private final ComputerBuildRepository buildRepository;
    private final UserService userService;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final CompatibilityHelper compatibilityHelper; // Use the interface

    private String nvmeInterfaceId;
    private List<String> sataInterfaceIds;

    @PostConstruct
    public void initialize() {
        log.info("Caching IDs for compatibility checker...");
        this.nvmeInterfaceId = storageInterfaceRepository.findByName("NVMe").map(StorageInterface::getId).orElse(null);
        if (this.nvmeInterfaceId == null)
            log.warn("Could not find 'NVMe' in StorageInterface lookup. NVMe checks will be skipped.");

        this.sataInterfaceIds = storageInterfaceRepository.findAll().stream()
                .filter(si -> si.getName() != null && si.getName().toUpperCase().contains("SATA"))
                .map(StorageInterface::getId)
                .collect(Collectors.toList());
        if (this.sataInterfaceIds.isEmpty())
            log.warn("Could not find 'SATA' types in StorageInterface lookup. SATA checks will be skipped.");
    }

    @Override
    public CompatibilityResult checkCompatibility(String buildId) {
        String currentUserId = userService.findByUserId();

        ComputerBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found with ID: " + buildId));

        if (!build.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this build.");
        }

        log.info("Starting compatibility check for build ID: {}", buildId);
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        Cpu cpu = build.getCpu();
        Motherboard motherboard = build.getMotherboard();
        Psu psu = build.getPsu();
        Case computerCase = build.getCaseDetail();
        Cooler cooler = build.getCooler();

        if (cpu == null) errors.add("Critical Error: CPU not selected.");
        if (motherboard == null) errors.add("Critical Error: Motherboard not selected.");
        if (psu == null) errors.add("Critical Error: PSU not selected.");
        if (computerCase == null) errors.add("Critical Error: Case not selected.");
        if (build.getRamKits() == null || build.getRamKits().isEmpty()) {
            errors.add("Critical Error: RAM not selected.");
        }

        if (!errors.isEmpty()) {
            return CompatibilityResult.builder().isCompatible(false).errors(errors).warnings(warnings).totalWattage(0).build();
        }

        compatibilityHelper.checkCpuAndMotherboard(cpu, motherboard, errors);
        compatibilityHelper.checkRamCompatibility(build.getRamKits(), motherboard, errors);
        compatibilityHelper.checkFormFactorCompatibility(motherboard, computerCase, errors);
        compatibilityHelper.checkPsuFormFactor(psu, computerCase, errors);
        compatibilityHelper.checkGpuCompatibility(build.getGpus(), motherboard, computerCase, errors);
        compatibilityHelper.checkCoolerCompatibility(cooler, motherboard, computerCase, warnings, errors);
        compatibilityHelper.checkStorageCompatibility(build.getStorageDrives(), motherboard, this.nvmeInterfaceId, this.sataInterfaceIds, warnings, errors);
        compatibilityHelper.checkStorageAndCaseBays(build.getStorageDrives(), computerCase, errors);

        int totalWattage = compatibilityHelper.calculateTotalWattage(cpu, motherboard, build.getRamKits(), build.getGpus(), cooler);
        compatibilityHelper.checkPsuWattage(psu, totalWattage, errors, warnings);

        return CompatibilityResult.builder()
                .isCompatible(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .totalWattage(totalWattage)
                .build();
    }
}