package in.project.computers.service.componentCompatibility;


import in.project.computers.DTO.builds.CompatibilityResult;

public interface ComponentCompatibilityService {

    CompatibilityResult checkCompatibility(String buildId);
}