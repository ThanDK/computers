package in.project.computers.service.computerBuildService;

import in.project.computers.DTO.builds.BuildPartDetail;
import in.project.computers.DTO.builds.ComputerBuildDetailResponse;
import in.project.computers.DTO.builds.ComputerBuildRequest;
import in.project.computers.DTO.component.componentResponse.*;
import in.project.computers.entity.component.*;
import in.project.computers.entity.computerBuild.BuildPart;
import in.project.computers.entity.computerBuild.ComputerBuild;
import in.project.computers.repository.generalReposiroty.ComputerBuildRepository;
import in.project.computers.service.componentService.ComponentConverter;
import in.project.computers.service.userAuthenticationService.UserService;
import in.project.computers.service.util.ComponentFetcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBuildServiceImpl implements UserBuildService {

    private final ComputerBuildRepository buildRepository;
    private final UserService userService;
    private final ComponentConverter componentConverter;
    private final ComponentFetcher componentFetcher;

    @Override
    @Transactional
    public ComputerBuildDetailResponse saveBuild(ComputerBuildRequest request) {
        String userId = userService.findByUserId();

        Cpu cpu = componentFetcher.fetchComponentEntity(request.getCpuId(), Cpu.class);
        Motherboard motherboard = componentFetcher.fetchComponentEntity(request.getMotherboardId(), Motherboard.class);
        Psu psu = componentFetcher.fetchComponentEntity(request.getPsuId(), Psu.class);
        Case caseDetail = componentFetcher.fetchComponentEntity(request.getCaseId(), Case.class);
        Cooler cooler = componentFetcher.fetchComponentEntity(request.getCoolerId(), Cooler.class);

        List<BuildPart<RamKit>> ramKits = componentFetcher.fetchBuildParts(request.getRamKits(), RamKit.class);
        List<BuildPart<Gpu>> gpus = componentFetcher.fetchBuildParts(request.getGpus(), Gpu.class);
        List<BuildPart<StorageDrive>> storageDrives = componentFetcher.fetchBuildParts(request.getStorageDrives(), StorageDrive.class);

        ComputerBuild buildEntity = ComputerBuild.builder()
                .userId(userId)
                .buildName(request.getBuildName())
                .cpu(cpu)
                .motherboard(motherboard)
                .psu(psu)
                .caseDetail(caseDetail)
                .cooler(cooler)
                .ramKits(ramKits)
                .gpus(gpus)
                .storageDrives(storageDrives)
                .build();

        ComputerBuild savedBuild = buildRepository.save(buildEntity);
        log.info("Successfully saved new build with ID: {} for user ID: {}", savedBuild.getId(), userId);

        return convertEntityToResponse(savedBuild);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComputerBuildDetailResponse> getBuildsForCurrentUser() {
        String userId = userService.findByUserId();
        List<ComputerBuild> userBuilds = buildRepository.findByUserId(userId);
        return userBuilds.stream()
                .map(this::convertEntityToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ComputerBuildDetailResponse getBuildDetails(String buildId) {
        String currentUserId = userService.findByUserId();
        ComputerBuild build = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found with ID: " + buildId));

        if (!build.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not own this build.");
        }

        return convertEntityToResponse(build);
    }
    @Override
    @Transactional
    public ComputerBuildDetailResponse updateBuild(String buildId, ComputerBuildRequest request) {
        String currentUserId = userService.findByUserId();

        ComputerBuild buildToUpdate = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found with ID: " + buildId));

        if (!buildToUpdate.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to edit this build.");
        }

        Cpu cpu = componentFetcher.fetchComponentEntity(request.getCpuId(), Cpu.class);
        Motherboard motherboard = componentFetcher.fetchComponentEntity(request.getMotherboardId(), Motherboard.class);
        Psu psu = componentFetcher.fetchComponentEntity(request.getPsuId(), Psu.class);
        Case caseDetail = componentFetcher.fetchComponentEntity(request.getCaseId(), Case.class);
        Cooler cooler = componentFetcher.fetchComponentEntity(request.getCoolerId(), Cooler.class);
        List<BuildPart<RamKit>> ramKits = componentFetcher.fetchBuildParts(request.getRamKits(), RamKit.class);
        List<BuildPart<Gpu>> gpus = componentFetcher.fetchBuildParts(request.getGpus(), Gpu.class);
        List<BuildPart<StorageDrive>> storageDrives = componentFetcher.fetchBuildParts(request.getStorageDrives(), StorageDrive.class);

        buildToUpdate.setBuildName(request.getBuildName());
        buildToUpdate.setCpu(cpu);
        buildToUpdate.setMotherboard(motherboard);
        buildToUpdate.setPsu(psu);
        buildToUpdate.setCaseDetail(caseDetail);
        buildToUpdate.setCooler(cooler);
        buildToUpdate.setRamKits(ramKits);
        buildToUpdate.setGpus(gpus);
        buildToUpdate.setStorageDrives(storageDrives);


        ComputerBuild updatedBuild = buildRepository.save(buildToUpdate);
        log.info("Successfully updated build with ID: {} for user ID: {}", updatedBuild.getId(), currentUserId);

        return convertEntityToResponse(updatedBuild);
    }

    @Override
    @Transactional
    public void deleteBuild(String buildId) {
        String currentUserId = userService.findByUserId();
        ComputerBuild buildToDelete = buildRepository.findById(buildId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build not found with ID: " + buildId));

        if (!buildToDelete.getUserId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: You do not have permission to delete this build.");
        }
        buildRepository.delete(buildToDelete);
        log.info("Successfully deleted build with ID: {}", buildId);
    }

    private ComputerBuildDetailResponse convertEntityToResponse(ComputerBuild build) {
        CpuResponse cpuResponse = componentConverter.convertEntityToResponse(build.getCpu(), CpuResponse.class);
        MotherboardResponse motherboardResponse = componentConverter.convertEntityToResponse(build.getMotherboard(), MotherboardResponse.class);
        PsuResponse psuResponse = componentConverter.convertEntityToResponse(build.getPsu(), PsuResponse.class);
        CaseResponse caseResponse = componentConverter.convertEntityToResponse(build.getCaseDetail(), CaseResponse.class);
        CoolerResponse coolerResponse = componentConverter.convertEntityToResponse(build.getCooler(), CoolerResponse.class);

        List<BuildPartDetail<RamKitResponse>> ramKitDetails = convertBuildPartsToDetails(build.getRamKits(), RamKitResponse.class);
        List<BuildPartDetail<GpuResponse>> gpuDetails = convertBuildPartsToDetails(build.getGpus(), GpuResponse.class);
        List<BuildPartDetail<StorageDriveResponse>> storageDriveDetails = convertBuildPartsToDetails(build.getStorageDrives(), StorageDriveResponse.class);

        BigDecimal totalPrice = calculateTotalPrice(cpuResponse, motherboardResponse, psuResponse, caseResponse, coolerResponse, ramKitDetails, gpuDetails, storageDriveDetails);

        return ComputerBuildDetailResponse.builder()
                .id(build.getId())
                .userId(build.getUserId())
                .buildName(build.getBuildName())
                .cpu(cpuResponse)
                .motherboard(motherboardResponse)
                .psu(psuResponse)
                .caseDetail(caseResponse)
                .cooler(coolerResponse)
                .ramKits(ramKitDetails)
                .gpus(gpuDetails)
                .storageDrives(storageDriveDetails)
                .totalPrice(totalPrice)
                .build();
    }

    private <T extends Component, R extends ComponentResponse> List<BuildPartDetail<R>> convertBuildPartsToDetails(
            List<BuildPart<T>> parts, Class<R> responseClass) {
        if (parts == null || parts.isEmpty()) {
            return Collections.emptyList();
        }
        return parts.stream()
                .map(part -> {
                    R response = componentConverter.convertEntityToResponse(part.getComponent(), responseClass);
                    return new BuildPartDetail<>(response, part.getQuantity());
                })
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotalPrice(
            CpuResponse cpu, MotherboardResponse motherboard, PsuResponse psu, CaseResponse caseDetail, CoolerResponse cooler,
            List<BuildPartDetail<RamKitResponse>> ramKits,
            List<BuildPartDetail<GpuResponse>> gpus,
            List<BuildPartDetail<StorageDriveResponse>> storageDrives
    ) {
        BigDecimal singlePartsTotal = Stream.of(cpu, motherboard, psu, caseDetail, cooler)
                .filter(part -> part != null && part.getPrice() != null)
                .map(ComponentResponse::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ramKitsTotal = ramKits.stream()
                .filter(item -> item != null && item.getPartDetails() != null && item.getPartDetails().getPrice() != null)
                .map(item -> item.getPartDetails().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal gpusTotal = gpus.stream()
                .filter(item -> item != null && item.getPartDetails() != null && item.getPartDetails().getPrice() != null)
                .map(item -> item.getPartDetails().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal storageDrivesTotal = storageDrives.stream()
                .filter(item -> item != null && item.getPartDetails() != null && item.getPartDetails().getPrice() != null)
                .map(item -> item.getPartDetails().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return singlePartsTotal.add(ramKitsTotal).add(gpusTotal).add(storageDrivesTotal);
    }
}