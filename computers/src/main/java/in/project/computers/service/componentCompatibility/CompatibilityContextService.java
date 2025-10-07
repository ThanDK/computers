package in.project.computers.service.componentCompatibility;

import in.project.computers.DTO.builds.CompatibleComponentsRequest;

/**
 * A service responsible for analyzing a user's partial build request and
 * constructing a {@link CompatibilityContext} object. This context contains all
 * known compatibility constraints derived from the selected components.
 */
public interface CompatibilityContextService {

    /**
     * Builds a compatibility context from the provided request.
     *
     * @param request The user's current build selection.
     * @return A {@link CompatibilityContext} object populated with all relevant
     *         constraints for filtering unselected components.
     */
    CompatibilityContext buildContext(CompatibleComponentsRequest request);
}