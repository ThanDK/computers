// src/main/java/in/project/computers/controller/AdminController/ComponentManagementController.java

package in.project.computers.controller.AdminController;

import in.project.computers.dto.component.componentRequest.ComponentRequest;
// ComponentUpdateRequest and PriceUpdateRequest are no longer needed
import in.project.computers.dto.component.componentRequest.StockAdjustmentRequest;
import in.project.computers.dto.component.componentResponse.ComponentResponse;
import in.project.computers.service.componentService.ComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin("*")
public class ComponentManagementController {

    private final ComponentService componentService;

    // ... getAllComponents and getComponentById methods are unchanged ...
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ComponentResponse>> getAllComponents() {
        log.info("Request to fetch all components");
        List<ComponentResponse> components = componentService.getAllComponents();
        return ResponseEntity.ok(components);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ComponentResponse> getComponentById(@PathVariable String id) {
        log.info("Request to fetch component with ID: {}", id);
        ComponentResponse component = componentService.getComponentDetailsById(id);
        return ResponseEntity.ok(component);
    }

    @PostMapping("/")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentResponse> createComponent(
            @Valid @RequestPart("request") ComponentRequest request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile) {
        log.info("Admin action: Creating new component with MPN: {}", request.getMpn());
        ComponentResponse newComponent = componentService.createComponent(request, imageFile);
        return new ResponseEntity<>(newComponent, HttpStatus.CREATED);
    }

    /**
     * <h4>[PUT] /api/components/{id}</h4>
     * <p>Admin endpoint to fully update a component's details and/or image.</p>
     * @param id The ID of the component to update.
     * @param request The full JSON data for the component.
     * @param imageFile A new image file to replace the old one (optional).
     * @param removeImage A boolean flag (sent as a query parameter) to remove the existing image.
     * @return The updated component.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentResponse> updateComponent(
            @PathVariable String id,
            @Valid @RequestPart("request") ComponentRequest request, // <-- Use ComponentRequest
            @RequestPart(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "removeImage", defaultValue = "false") boolean removeImage) { // <-- Added removeImage flag
        log.info("Admin action: Updating component with ID: {}. Remove image flag: {}", id, removeImage);
        ComponentResponse updatedComponent = componentService.updateComponent(id, request, imageFile, removeImage);
        return ResponseEntity.ok(updatedComponent);
    }

    // ... adjustStock and deleteComponent methods are unchanged ...
    @PatchMapping("/stock/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComponentResponse> adjustStock(
            @PathVariable String id,
            @Valid @RequestBody StockAdjustmentRequest request) {
        log.info("Admin action: Adjusting stock for component ID: {} by {}", id, request.getQuantity());
        ComponentResponse updatedComponent = componentService.adjustStock(id, request);
        return ResponseEntity.ok(updatedComponent);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteComponent(@PathVariable String id) {
        log.info("Admin action: Deleting component with ID: {}", id);
        componentService.deleteComponent(id);
    }
}