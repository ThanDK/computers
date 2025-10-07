package in.project.computers.service.componentCompatibility;

import in.project.computers.DTO.builds.CompatibleComponentsRequest;
import in.project.computers.DTO.builds.CompatibleComponentsResponse;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;
import in.project.computers.entity.component.Component;
import in.project.computers.service.componentService.ComponentConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComponentFilterServiceImpl implements ComponentFilterService {

    private final CompatibilityContextService contextService;
    private final ComponentQueryService queryService;
    private final ComponentConverter componentConverter;

    @Override
    public CompatibleComponentsResponse findCompatibleComponents(CompatibleComponentsRequest request) {
        // 1. Build the context from the incoming request.
        CompatibilityContext context = contextService.buildContext(request);

        // 2. Prepare the map for available components.
        Map<String, List<ComponentResponse>> availableComponents = new HashMap<>();

        // 3. For each empty slot, query for compatible components and add them to the map.
        if (request.getCpuId() == null) {
            availableComponents.put("cpus", convertToResponseList(queryService.findCompatibleCpus(context)));
        }
        if (request.getMotherboardId() == null) {
            availableComponents.put("motherboards", convertToResponseList(queryService.findCompatibleMotherboards(context)));
        }
        if (CollectionUtils.isEmpty(request.getRamKits())) {
            availableComponents.put("ramKits", convertToResponseList(queryService.findCompatibleRamKits(context)));
        }
        if (CollectionUtils.isEmpty(request.getGpus())) {
            availableComponents.put("gpus", convertToResponseList(queryService.findCompatibleGpus(context)));
        }
        if (request.getCaseId() == null) {
            availableComponents.put("cases", convertToResponseList(queryService.findCompatibleCases(context)));
        }
        if (request.getPsuId() == null) {
            availableComponents.put("psus", convertToResponseList(queryService.findCompatiblePsus(context)));
        }
        if (request.getCoolerId() == null) {
            availableComponents.put("coolers", convertToResponseList(queryService.findCompatibleCoolers(context)));
        }
        if (CollectionUtils.isEmpty(request.getStorageDrives())) {
            availableComponents.put("storageDrives", convertToResponseList(queryService.findCompatibleStorageDrives(context)));
        }

        // 4. Build and return the final response.
        return CompatibleComponentsResponse.builder()
                .availableComponents(availableComponents)
                .totalWattage(context.getRequiredWattage())
                .build();
    }

    private <T extends Component> List<ComponentResponse> convertToResponseList(List<T> components) {
        return components.stream()
                .map(componentConverter::convertEntityToResponse)
                .collect(Collectors.toList());
    }
}