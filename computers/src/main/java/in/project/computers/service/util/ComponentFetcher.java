package in.project.computers.service.util;

import in.project.computers.entity.component.Component;
import in.project.computers.entity.computerBuild.BuildPart;
import in.project.computers.repository.componentRepository.ComponentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
public class ComponentFetcher {

    private final ComponentRepository componentRepository;

    public <T extends Component> T fetchComponentEntity(String componentId, Class<T> componentClass) {
        if (componentId == null || componentId.isBlank()) {
            return null;
        }
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component not found with ID: " + componentId));

        if (!componentClass.isInstance(component)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Component with ID " + componentId + " is not of expected type " + componentClass.getSimpleName());
        }
        return componentClass.cast(component);
    }

    public <T extends Component> List<BuildPart<T>> fetchBuildParts(Map<String, Integer> componentMap, Class<T> componentClass) {
        if (componentMap == null || componentMap.isEmpty()) {
            return Collections.emptyList();
        }
        return componentMap.entrySet().stream()
                .map(entry -> {
                    T component = fetchComponentEntity(entry.getKey(), componentClass);
                    return new BuildPart<>(component, entry.getValue());
                })
                .collect(Collectors.toList());
    }
}