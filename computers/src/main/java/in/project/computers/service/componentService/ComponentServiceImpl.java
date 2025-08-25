package in.project.computers.service.componentService;

import in.project.computers.entity.component.Component;
import in.project.computers.entity.component.Inventory;
import in.project.computers.DTO.component.componentRequest.ComponentRequest;
import in.project.computers.DTO.component.componentRequest.StockAdjustmentRequest;
import in.project.computers.DTO.component.componentResponse.ComponentResponse;

import in.project.computers.repository.componentRepository.ComponentRepository;
import in.project.computers.repository.componentRepository.InventoryRepository;
import in.project.computers.service.awsS3Bucket.S3Service;
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
        // === [CREATE-1] เริ่มกระบวนการสร้าง Component ใหม่ ===
        log.info("Attempting to create a new component with MPN: {}", request.getMpn());

        // === [CREATE-2] ตรวจสอบว่า MPN ซ้ำหรือไม่ ===
        if (componentRepository.findByMpn(request.getMpn()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Component with MPN " + request.getMpn() + " already exists.");
        }

        // === [CREATE-3] เรียกใช้เมธอดภายในเพื่อสร้าง Entity ของ Component และ Inventory ===
        Component savedComponent = createNewComponentAndInventory(request, imageFile);

        // === [CREATE-4] แปลง Entity ที่บันทึกแล้วเป็น DTO Response และส่งคืน ===
        return componentConverter.convertEntityToResponse(savedComponent);
    }


    @Override
    @Transactional
    public ComponentResponse updateComponent(String componentId, ComponentRequest request, MultipartFile imageFile, boolean removeImage) {
        // === [UPDATE-1] เริ่มกระบวนการอัปเดต Component ===
        log.info("Updating component ID: {}", componentId);

        // === [UPDATE-2] ค้นหา Component ที่ต้องการอัปเดต ===
        Component component = findComponentById(componentId);

        // === [UPDATE-3] จัดการการอัปเดตรูปภาพ (อัปโหลดใหม่, ลบ, หรือไม่ทำอะไร) ===
        handleImageUpdate(component, imageFile, removeImage);

        // === [UPDATE-4] เรียกใช้ Converter เพื่ออัปเดตข้อมูลใน Entity จาก Request ===
        componentConverter.updateEntityFromRequest(component, request);

        // === [UPDATE-5] บันทึก Entity ของ Component ที่อัปเดตแล้ว ===
        Component updatedComponent = componentRepository.save(component);

        // === [UPDATE-6] ค้นหาและอัปเดตราคาใน Inventory (ถ้ามีการเปลี่ยนแปลง) ===
        Inventory inventory = findInventoryByComponentId(componentId);
        if (request.getPrice() != null && !request.getPrice().equals(inventory.getPrice())) {
            inventory.setPrice(request.getPrice());
            inventoryRepository.save(inventory);
            log.info("... price for component ID {} updated to: {}", componentId, request.getPrice());
        }

        // === [UPDATE-7] แปลง Entity ที่อัปเดตแล้วเป็น DTO Response และส่งคืน ===
        log.info("Successfully saved updates for component ID: {}", componentId);
        return componentConverter.convertEntityToResponse(updatedComponent);
    }


    @Override
    @Transactional
    public ComponentResponse adjustStock(String componentId, StockAdjustmentRequest request) {
        // === [ADJUST-STOCK-1] เริ่มกระบวนการปรับสต็อก ===
        log.info("Adjusting stock for component ID: {} with change: {}", componentId, request.getQuantity());

        // === [ADJUST-STOCK-2] ค้นหา Component และ Inventory ที่เกี่ยวข้อง ===
        Component component = findComponentById(componentId);
        Inventory inventory = findInventoryByComponentId(componentId);

        // === [ADJUST-STOCK-3] เรียกใช้เมธอดภายในเพื่อคำนวณและตั้งค่าสต็อกใหม่ ===
        performStockAdjustment(component, inventory, request.getQuantity());

        // === [ADJUST-STOCK-4] บันทึกข้อมูลที่อัปเดตแล้วลง DB ===
        inventoryRepository.save(inventory);
        componentRepository.save(component);

        // === [ADJUST-STOCK-5] ส่งคืนข้อมูล Component ที่อัปเดตแล้ว ===
        return componentConverter.convertEntityToResponse(findComponentById(componentId));
    }


    @Override
    @Transactional
    public void deleteComponent(String componentId) {
        // === [DELETE-1] เริ่มกระบวนการลบ Component ===
        log.info("Attempting to delete component with ID: {}", componentId);

        // === [DELETE-2] ค้นหา Component และ Inventory ที่ต้องการลบ ===
        Component componentToDelete = findComponentById(componentId);
        Inventory inventoryToDelete = findInventoryByComponentId(componentId);
        String imageUrl = componentToDelete.getImageUrl();

        // === [DELETE-3] ลบรูปภาพที่เกี่ยวข้องออกจาก S3 (ถ้ามี) ===
        if (imageUrl != null && !imageUrl.isBlank()) {
            deleteS3File(imageUrl);
        }

        // === [DELETE-4] ลบข้อมูล Inventory และ Component ออกจาก DB ===
        inventoryRepository.delete(inventoryToDelete);
        componentRepository.delete(componentToDelete);
        log.info("... component and inventory with ID: {} deleted successfully from DB.", componentId);
    }


    @Override
    @Transactional(readOnly = true)
    public ComponentResponse getComponentDetailsById(String componentId) {
        // === [GET-BY-ID-1] เริ่มกระบวนการดึงข้อมูล Component ===
        log.debug("Fetching details for component ID: {}", componentId);

        // === [GET-BY-ID-2] ค้นหา Component จาก ID ===
        Component component = findComponentById(componentId);

        // === [GET-BY-ID-3] แปลง Entity เป็น DTO Response และส่งคืน ===
        return componentConverter.convertEntityToResponse(component);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentResponse> getAllComponents() {
        // === [GET-ALL-1] เริ่มกระบวนการดึงข้อมูล Component ทั้งหมด ===
        log.debug("Fetching all components from the database.");

        // === [GET-ALL-2] ค้นหา Component ทั้งหมดและแปลงเป็น DTO Response ===
        return componentRepository.findAll()
                .stream()
                .map(componentConverter::convertEntityToResponse)
                .collect(Collectors.toList());
    }

    // จัดการการอัปเดตรูปภาพ: อัปโหลดใหม่, ลบ, หรือไม่ทำอะไร
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

    // สร้าง Entity ของ Component และ Inventory พร้อมจัดการรูปภาพ
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

    // คำนวณสต็อกใหม่และอัปเดตสถานะของ Component
    private void performStockAdjustment(Component component, Inventory inventory, int quantityChange) {
        int currentQuantity = inventory.getQuantity();
        int newQuantity = currentQuantity + quantityChange;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove " + Math.abs(quantityChange) + " items. Only " + currentQuantity + " are in stock.");
        }

        inventory.setQuantity(newQuantity);
        component.setActive(newQuantity > 0);
    }

    // ลบไฟล์ออกจาก S3 โดยใช้ URL และจัดการข้อผิดพลาด
    private void deleteS3File(String imageUrl) {
        try {
            String fileKey = s3Service.extractKeyFromUrl(imageUrl);
            if (fileKey != null) {
                boolean isFileDeleted = s3Service.deleteFileByKey(fileKey);
                if (isFileDeleted) {
                    log.info("... Associated file '{}' was successfully deleted from S3.", fileKey);
                } else {
                    log.warn("... Deleting file '{}' from S3 failed. Please check S3 logs.", fileKey);
                }
            }
        } catch (Exception e) {
            log.error("... Error while trying to delete S3 file from URL '{}'", imageUrl, e);
        }
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