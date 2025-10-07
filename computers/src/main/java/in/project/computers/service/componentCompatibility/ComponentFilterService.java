package in.project.computers.service.componentCompatibility;

import in.project.computers.DTO.builds.CompatibleComponentsRequest;
import in.project.computers.DTO.builds.CompatibleComponentsResponse;


public interface ComponentFilterService {


    CompatibleComponentsResponse findCompatibleComponents(CompatibleComponentsRequest request);

}