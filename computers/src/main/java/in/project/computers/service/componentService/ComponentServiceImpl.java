

package in.project.computers.service.componentService;

import in.project.computers.entity.component.Component;
import in.project.computers.entity.component.Inventory;
import in.project.computers.dto.component.componentRequest.ComponentRequest;
import in.project.computers.dto.component.componentRequest.StockAdjustmentRequest;
import in.project.computers.dto.component.componentResponse.ComponentResponse;
import in.project.computers.repository.ComponentRepo.ComponentRepository;
import in.project.computers.repository.ComponentRepo.InventoryRepository;
import in.project.computers.service.AWSS3Bucket.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ComponentServiceImpl implements ComponentService {

    private final ComponentRepository componentRepository;
    private final InventoryRepository inventoryRepository;
    private final ComponentConverter componentConverter;
    private final S3Service s3Service;

    @Override
    @Transactional
    public ComponentResponse createComponent(ComponentRequest request, MultipartFile imageFile) {
        log.info("Attempting to create a new component with MPN: {}", request.getMpn());
        if (componentRepository.findByMpn(request.getMpn()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Component with MPN " + request.getMpn() + " already exists.");
        }
        Component savedComponent = createNewComponentAndInventory(request, imageFile);
        return componentConverter.convertEntityToResponse(savedComponent);
    }


    @Override
    @Transactional
    public ComponentResponse updateComponent(String componentId, ComponentRequest request, MultipartFile imageFile, boolean removeImage) {
        log.info("Updating component ID: {}", componentId);
        Component component = findComponentById(componentId);

        // 1. Handle Image Logic
        handleImageUpdate(component, imageFile, removeImage);

 
        componentConverter.updateEntityFromRequest(component, request);


        Component updatedComponent = componentRepository.save(component);


        Inventory inventory = findInventoryByComponentId(componentId);
        if (request.getPrice() != null && !request.getPrice().equals(inventory.getPrice())) {
            inventory.setPrice(request.getPrice());
            inventoryRepository.save(inventory);
            log.info("... price for component ID {} updated to: {}", componentId, request.getPrice());
        }

        log.info("Successfully saved updates for component ID: {}", componentId);
        return componentConverter.convertEntityToResponse(updatedComponent);
    }


    @Override
    @Transactional
    public ComponentResponse adjustStock(String componentId, StockAdjustmentRequest request) {
        log.info("Adjusting stock for component ID: {} with change: {}", componentId, request.getQuantity());
        Component component = findComponentById(componentId);
        Inventory inventory = findInventoryByComponentId(componentId);
        performStockAdjustment(component, inventory, request.getQuantity());
        inventoryRepository.save(inventory);
        componentRepository.save(component);
        return componentConverter.convertEntityToResponse(findComponentById(componentId));
    }


    @Override
    @Transactional
    public void deleteComponent(String componentId) {
        log.info("Attempting to delete component with ID: {}", componentId);
        Component componentToDelete = findComponentById(componentId);
        Inventory inventoryToDelete = findInventoryByComponentId(componentId);
        String imageUrl = componentToDelete.getImageUrl();


        if (imageUrl != null && !imageUrl.isBlank()) {
            deleteS3File(imageUrl);

        }
        inventoryRepository.delete(inventoryToDelete);
        componentRepository.delete(componentToDelete);
        log.info("... component and inventory with ID: {} deleted successfully from DB.", componentId);

    }

    // ... getComponentDetailsById and getAllComponents are unchanged ...
    @Override
    @Transactional(readOnly = true)
    public ComponentResponse getComponentDetailsById(String componentId) {
        log.debug("Fetching details for component ID: {}", componentId);
        Component component = findComponentById(componentId);
        return componentConverter.convertEntityToResponse(component);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentResponse> getAllComponents() {
        log.debug("Fetching all components from the database.");
        return componentRepository.findAll()
                .stream()
                .map(componentConverter::convertEntityToResponse)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // SECTION: Private Helper Methods
    // =========================================================================

    private void handleImageUpdate(Component component, MultipartFile imageFile, boolean removeImage) {
        String oldImageUrl = component.getImageUrl();


        if (imageFile != null && !imageFile.isEmpty()) {
            log.info("... new image provided. Replacing old image if it exists.");
            if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                deleteS3File(oldImageUrl);
            }
            String newImageUrl = s3Service.uploadFile(imageFile);
            component.setImageUrl(newImageUrl);
            return;
        }


        if (removeImage && oldImageUrl != null && !oldImageUrl.isBlank()) {
            log.info("... removing existing image for component ID: {}", component.getId());
            deleteS3File(oldImageUrl);
            component.setImageUrl(null);
        }

    }

    private void deleteS3File(String imageUrl) {
        try {
            String filename = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            boolean isFileDeleted = s3Service.deleteFile(filename);
            if (isFileDeleted) {
                log.info("... Associated file '{}' was successfully deleted from S3.", filename);
            } else {
                log.warn("... Deleting file '{}' from S3 failed. Please check S3 logs.", filename);
            }
        } catch (Exception e) {
            log.error("... Error while trying to delete S3 file from URL '{}'", imageUrl, e);
        }
    }

    // ... createNewComponentAndInventory, performStockAdjustment, findComponentById, findInventoryByComponentId are unchanged ...
    private Component createNewComponentAndInventory(ComponentRequest request, MultipartFile imageFile) {
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3Service.uploadFile(imageFile);
        }
        Component componentEntity = componentConverter.convertRequestToEntity(request);
        componentEntity.setImageUrl(imageUrl);
        componentEntity.setActive(request.getQuantity() > 0);
        Component savedComponent = componentRepository.save(componentEntity);
        Inventory inventory = Inventory.builder()
                .componentId(savedComponent.getId())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();
        inventoryRepository.save(inventory);
        return savedComponent;
    }

    private void performStockAdjustment(Component component, Inventory inventory, int quantityChange) {
        int currentQuantity = inventory.getQuantity();
        int newQuantity = currentQuantity + quantityChange;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove " + Math.abs(quantityChange) + " items. Only " + currentQuantity + " are in stock.");
        }
        inventory.setQuantity(newQuantity);
        component.setActive(newQuantity > 0);
    }

    private Component findComponentById(String id) {
        return componentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found with ID: " + id));
    }

    private Inventory findInventoryByComponentId(String componentId) {
        return inventoryRepository.findByComponentId(componentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Data inconsistency: Inventory record not found for Component ID: " + componentId));
    }
}