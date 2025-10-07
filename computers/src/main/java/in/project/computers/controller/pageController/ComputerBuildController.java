package in.project.computers.controller.pageController;

import in.project.computers.DTO.builds.CompatibleComponentsRequest;
import in.project.computers.DTO.builds.CompatibleComponentsResponse;
import in.project.computers.DTO.builds.CompatibilityCheckRequest;
import in.project.computers.DTO.builds.ComputerBuildDetailResponse;
import in.project.computers.DTO.builds.ComputerBuildRequest;
import in.project.computers.DTO.builds.CompatibilityResult;
import in.project.computers.service.componentCompatibility.ComponentCompatibilityService;
import in.project.computers.service.componentCompatibility.ComponentFilterService;
import in.project.computers.service.computerBuildService.UserBuildService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/builds")
@RequiredArgsConstructor
public class ComputerBuildController {

    private final UserBuildService userBuildService;
    private final ComponentCompatibilityService compatibilityService;
    private final ComponentFilterService componentFilterService;

    @PostMapping
    public ResponseEntity<ComputerBuildDetailResponse> saveBuild(@Valid @RequestBody ComputerBuildRequest request) {
        ComputerBuildDetailResponse savedBuild = userBuildService.saveBuild(request);
        return new ResponseEntity<>(savedBuild, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ComputerBuildDetailResponse>> getUserBuilds() {
        List<ComputerBuildDetailResponse> builds = userBuildService.getBuildsForCurrentUser();
        return ResponseEntity.ok(builds);
    }

    @PostMapping("/check-compatibility")
    public ResponseEntity<CompatibilityResult> checkTransientBuildCompatibility(@RequestBody CompatibilityCheckRequest request) {
        CompatibilityResult result = compatibilityService.checkCompatibility(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/filter-compatible-parts")
    public ResponseEntity<CompatibleComponentsResponse> getCompatibleComponents(
            @RequestBody CompatibleComponentsRequest request) {
        CompatibleComponentsResponse response = componentFilterService.findCompatibleComponents(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{buildId}")
    public ResponseEntity<ComputerBuildDetailResponse> getBuildDetails(@PathVariable String buildId) {
        ComputerBuildDetailResponse build = userBuildService.getBuildDetails(buildId);
        return ResponseEntity.ok(build);
    }

    @GetMapping("/check/{buildId}")
    public ResponseEntity<CompatibilityResult> checkBuildCompatibility(@PathVariable String buildId) {
        CompatibilityResult result = compatibilityService.checkCompatibility(buildId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{buildId}")
    public ResponseEntity<ComputerBuildDetailResponse> updateBuild(
            @PathVariable String buildId,
            @RequestBody ComputerBuildRequest request
    ) {
        ComputerBuildDetailResponse updatedBuild = userBuildService.updateBuild(buildId, request);
        return ResponseEntity.ok(updatedBuild);
    }

    @DeleteMapping("/{buildId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBuild(@PathVariable String buildId) {
        userBuildService.deleteBuild(buildId);
    }
}