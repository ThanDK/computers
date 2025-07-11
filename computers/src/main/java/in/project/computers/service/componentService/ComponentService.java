package in.project.computers.service.componentService;

import in.project.computers.dto.component.componentRequest.ComponentRequest;
import in.project.computers.dto.component.componentRequest.StockAdjustmentRequest;
import in.project.computers.dto.component.componentResponse.ComponentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ComponentService {

    ComponentResponse createComponent(ComponentRequest request, MultipartFile imageFile);

    ComponentResponse updateComponent(String componentId, ComponentRequest request, MultipartFile imageFile, boolean removeImage);

    ComponentResponse adjustStock(String componentId, StockAdjustmentRequest request);


    void deleteComponent(String componentId);

    ComponentResponse getComponentDetailsById(String componentId);

    List<ComponentResponse> getAllComponents();

}