package in.project.computers.service.componentCompatibility;

import in.project.computers.DTO.builds.CompatibilityCheckRequest;
import in.project.computers.DTO.builds.CompatibilityResult;
import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;
import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.entity.lookup.StorageInterface;
import in.project.computers.repository.lookupRepository.StorageInterfaceRepository;
import in.project.computers.repository.generalReposiroty.ComputerBuildRepository;
import in.project.computers.service.userAuthenticationService.UserService;
import in.project.computers.service.util.ComponentFetcher;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

// คลาสหลักสำหรับจัดการ Logic การตรวจสอบความเข้ากันได้ของชิ้นส่วนใน ComputerBuild
@Service
@Slf4j
@RequiredArgsConstructor
public class ComponentCompatibilityServiceImpl implements ComponentCompatibilityService {

    private final ComputerBuildRepository buildRepository;
    private final UserService userService;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final CompatibilityHelper compatibilityHelper;

    private final ComponentFetcher componentFetcher;

    private String nvmeInterfaceId;
    private List<String> sataInterfaceIds;

    @PostConstruct
    public void initialize() {
        log.info("Caching IDs for compatibility checker...");
        // === [INIT-1] ค้นหาและจัดการ NVMe ID ===
        List<StorageInterface> nvmeInterfaces = storageInterfaceRepository.findAllByName("NVMe");
        if (nvmeInterfaces.size() > 1) {
            log.error("CRITICAL DATABASE INCONSISTENCY: Found {} entries for 'NVMe' in storage_interfaces. Using the first one found (ID: {}). Please clean up the duplicates.", nvmeInterfaces.size(), nvmeInterfaces.getFirst().getId());
            this.nvmeInterfaceId = nvmeInterfaces.getFirst().getId();
        } else if (nvmeInterfaces.size() == 1) {
            this.nvmeInterfaceId = nvmeInterfaces.getFirst().getId();
            log.info("Successfully cached 'NVMe' interface ID: {}", this.nvmeInterfaceId);
        } else {
            this.nvmeInterfaceId = null;
            log.warn("Could not find 'NVMe' in StorageInterface lookup. NVMe compatibility checks will be skipped.");
        }

        // === [INIT-2] ค้นหาและแคช SATA ID ทั้งหมด ===
        this.sataInterfaceIds = storageInterfaceRepository.findAll().stream()
                .filter(si -> si.getName() != null && si.getName().toUpperCase().contains("SATA"))
                .map(StorageInterface::getId)
                .collect(Collectors.toList());

        if (this.sataInterfaceIds.isEmpty()) {
            log.warn("Could not find 'SATA' types in StorageInterface lookup. SATA checks will be skipped.");
        } else {
            log.info("Successfully cached {} SATA interface IDs.", this.sataInterfaceIds.size());
        }
    }

    @Override
    public CompatibilityResult checkCompatibility(String buildId) {
        // === [CHECK-1] ดึงข้อมูลผู้ใช้ปัจจุบันและ ComputerBuild จาก ID ===
        String currentUserId = userService.findByUserId();
        ComputerBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found with ID: " + buildId));

        // === [CHECK-2] ตรวจสอบสิทธิ์ความเป็นเจ้าของ Build ===
        if (!build.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this build.");
        }

        log.info("Starting compatibility check for build ID: {}", buildId);
        return performCompatibilityChecks(
                build.getCpu(),
                build.getMotherboard(),
                build.getPsu(),
                build.getCaseDetail(),
                build.getCooler(),
                build.getRamKits(),
                build.getGpus(),
                build.getStorageDrives()
        );
    }

    @Override
    public CompatibilityResult checkCompatibility(CompatibilityCheckRequest request) {
        log.info("Starting compatibility check for transient request.");

        Cpu cpu = componentFetcher.fetchComponentEntity(request.getCpuId(), Cpu.class);
        Motherboard motherboard = componentFetcher.fetchComponentEntity(request.getMotherboardId(), Motherboard.class);
        Psu psu = componentFetcher.fetchComponentEntity(request.getPsuId(), Psu.class);
        Case computerCase = componentFetcher.fetchComponentEntity(request.getCaseId(), Case.class);
        Cooler cooler = componentFetcher.fetchComponentEntity(request.getCoolerId(), Cooler.class);

        List<BuildPart<RamKit>> ramKits = componentFetcher.fetchBuildParts(request.getRamKits(), RamKit.class);
        List<BuildPart<Gpu>> gpus = componentFetcher.fetchBuildParts(request.getGpus(), Gpu.class);
        List<BuildPart<StorageDrive>> storageDrives = componentFetcher.fetchBuildParts(request.getStorageDrives(), StorageDrive.class);

        return performCompatibilityChecks(cpu, motherboard, psu, computerCase, cooler, ramKits, gpus, storageDrives);
    }


    private CompatibilityResult performCompatibilityChecks(
            Cpu cpu, Motherboard motherboard, Psu psu, Case computerCase, Cooler cooler,
            List<BuildPart<RamKit>> ramKits, List<BuildPart<Gpu>> gpus, List<BuildPart<StorageDrive>> storageDrives
    ) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // === [CHECK-3] ตรวจสอบชิ้นส่วนที่จำเป็น (Critical Parts) หากยังไม่ได้เลือก ให้จบการทำงานและแจ้งข้อผิดพลาดทันที ===
        if (cpu == null) errors.add("ข้อผิดพลาดร้ายแรง: ยังไม่ได้เลือก CPU");
        if (motherboard == null) errors.add("ข้อผิดพลาดร้ายแรง: ยังไม่ได้เลือกเมนบอร์ด");
        if (psu == null) errors.add("ข้อผิดพลาดร้ายแรง: ยังไม่ได้เลือก Power Supply");
        if (computerCase == null) errors.add("ข้อผิดพลาดร้ายแรง: ยังไม่ได้เลือกเคส");
        if (ramKits == null || ramKits.isEmpty()) {
            errors.add("ข้อผิดพลาดร้ายแรง: ยังไม่ได้เลือก RAM");
        }

        if (!errors.isEmpty()) {
            return CompatibilityResult.builder().isCompatible(false).errors(errors).warnings(warnings).totalWattage(0).build();
        }

        // === [CHECK-4] เรียกใช้ Helper เพื่อตรวจสอบความเข้ากันได้ในแต่ละส่วนอย่างละเอียด ===
        compatibilityHelper.checkCpuAndMotherboard(cpu, motherboard, errors);
        compatibilityHelper.checkRamCompatibility(ramKits, motherboard, errors);
        compatibilityHelper.checkFormFactorCompatibility(motherboard, computerCase, errors);
        compatibilityHelper.checkPsuFormFactor(psu, computerCase, errors);
        compatibilityHelper.checkGpuCompatibility(gpus, motherboard, computerCase, errors);
        compatibilityHelper.checkCoolerCompatibility(cooler, motherboard, computerCase, warnings, errors);
        compatibilityHelper.checkStorageCompatibility(storageDrives, motherboard, this.nvmeInterfaceId, this.sataInterfaceIds, warnings, errors);
        compatibilityHelper.checkStorageAndCaseBays(storageDrives, computerCase, errors);

        // === [CHECK-5] คำนวณและตรวจสอบการใช้พลังงาน (Wattage) ===
        int totalWattage = compatibilityHelper.calculateTotalWattage(cpu, motherboard, ramKits, gpus, cooler);
        compatibilityHelper.checkPsuWattage(psu, totalWattage, errors, warnings);

        // === [CHECK-6] สร้างและส่งคืนผลลัพธ์การตรวจสอบทั้งหมด ===
        return CompatibilityResult.builder()
                .isCompatible(errors.isEmpty())
                .errors(errors)
                .warnings(warnings)
                .totalWattage(totalWattage)
                .build();
    }
}