package in.project.computers.service.componentCompatibility;


import in.project.computers.dto.builds.CompatibilityResult;

public interface ComponentCompatibilityService {

    CompatibilityResult checkCompatibility(String buildId);
}