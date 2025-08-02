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

// คลาสหลักสำหรับจัดการ Business Logic ทั้งหมดที่เกี่ยวกับ Component
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
        // === [CREATE-1] เริ่มต้นกระบวนการสร้างชิ้นส่วนใหม่ ===
        log.info("Attempting to create a new component with MPN: {}", request.getMpn());
        // === [CREATE-2] ตรวจสอบว่า MPN (Manufacturer Part Number) ซ้ำกับที่มีในระบบหรือไม่ ===
        if (componentRepository.findByMpn(request.getMpn()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Component with MPN " + request.getMpn() + " already exists.");
        }
        // === [CREATE-3] เรียกใช้เมธอดภายในเพื่อสร้าง Entity และ Inventory พร้อมจัดการรูปภาพ ===
        Component savedComponent = createNewComponentAndInventory(request, imageFile);
        // === [CREATE-4] แปลง Entity ที่บันทึกแล้วให้เป็น DTO สำหรับส่งคืน ===
        return componentConverter.convertEntityToResponse(savedComponent);
    }


    @Override
    @Transactional
    public ComponentResponse updateComponent(String componentId, ComponentRequest request, MultipartFile imageFile, boolean removeImage) {
        // === [UPDATE-1] ค้นหา Component ที่ต้องการอัปเดตจาก ID ===
        log.info("Updating component ID: {}", componentId);
        Component component = findComponentById(componentId);

        // === [UPDATE-2] จัดการตรรกะเกี่ยวกับรูปภาพ (อัปโหลดใหม่, ลบ, หรือไม่ทำอะไร) ===
        handleImageUpdate(component, imageFile, removeImage);

        // === [UPDATE-3] เรียกใช้ Converter เพื่ออัปเดตข้อมูลใน Entity จาก Request ===
        componentConverter.updateEntityFromRequest(component, request);

        // === [UPDATE-4] บันทึก Component ที่อัปเดตแล้วลงฐานข้อมูล ===
        Component updatedComponent = componentRepository.save(component);

        // === [UPDATE-5] ตรวจสอบและอัปเดตราคาใน Inventory หากมีการเปลี่ยนแปลง ===
        Inventory inventory = findInventoryByComponentId(componentId);
        if (request.getPrice() != null && !request.getPrice().equals(inventory.getPrice())) {
            inventory.setPrice(request.getPrice());
            inventoryRepository.save(inventory);
            log.info("... price for component ID {} updated to: {}", componentId, request.getPrice());
        }

        log.info("Successfully saved updates for component ID: {}", componentId);
        // === [UPDATE-6] แปลง Entity ที่อัปเดตล่าสุดให้เป็น DTO สำหรับส่งคืน ===
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
        // === [ADJUST-STOCK-3] เรียกใช้เมธอดภายในเพื่อคำนวณและปรับสต็อก ===
        performStockAdjustment(component, inventory, request.getQuantity());
        // === [ADJUST-STOCK-4] บันทึกข้อมูลที่อัปเดตแล้วลงฐานข้อมูล ===
        inventoryRepository.save(inventory);
        componentRepository.save(component);
        // === [ADJUST-STOCK-5] แปลง Entity เป็น DTO เพื่อส่งคืนข้อมูลล่าสุด ===
        return componentConverter.convertEntityToResponse(findComponentById(componentId));
    }


    @Override
    @Transactional
    public void deleteComponent(String componentId) {
        // === [DELETE-1] เริ่มกระบวนการลบ Component ===
        log.info("Attempting to delete component with ID: {}", componentId);
        // === [DELETE-2] ค้นหา Component และ Inventory ที่เกี่ยวข้องเพื่อเตรียมลบ ===
        Component componentToDelete = findComponentById(componentId);
        Inventory inventoryToDelete = findInventoryByComponentId(componentId);
        String imageUrl = componentToDelete.getImageUrl();

        // === [DELETE-3] ตรวจสอบและลบรูปภาพที่เกี่ยวข้องออกจาก S3 (ถ้ามี) ===
        if (imageUrl != null && !imageUrl.isBlank()) {
            deleteS3File(imageUrl);
        }
        // === [DELETE-4] ลบข้อมูลออกจากฐานข้อมูล (Inventory ก่อน Component) ===
        inventoryRepository.delete(inventoryToDelete);
        componentRepository.delete(componentToDelete);
        log.info("... component and inventory with ID: {} deleted successfully from DB.", componentId);
    }


    @Override
    @Transactional(readOnly = true)
    public ComponentResponse getComponentDetailsById(String componentId) {
        // === [GET-BY-ID-1] ดึงข้อมูล Component และแปลงเป็น DTO ===
        log.debug("Fetching details for component ID: {}", componentId);
        Component component = findComponentById(componentId);
        return componentConverter.convertEntityToResponse(component);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentResponse> getAllComponents() {
        // === [GET-ALL-1] ดึงข้อมูล Component ทั้งหมดและแปลงเป็น List ของ DTO ===
        log.debug("Fetching all components from the database.");
        return componentRepository.findAll()
                .stream()
                .map(componentConverter::convertEntityToResponse)
                .collect(Collectors.toList());
    }

    // --- Private Helper Methods ---

    /**
     * เมธอดภายในสำหรับจัดการการอัปเดต/ลบรูปภาพ
     */
    private void handleImageUpdate(Component component, MultipartFile imageFile, boolean removeImage) {
        String oldImageUrl = component.getImageUrl();

        // === [UPDATE-2.1] กรณีมีไฟล์ใหม่: ลบไฟล์เก่า (ถ้ามี) และอัปโหลดไฟล์ใหม่ ===
        if (imageFile != null && !imageFile.isEmpty()) {
            log.info("... new image provided. Replacing old image if it exists.");
            if (oldImageUrl != null && !oldImageUrl.isBlank()) {
                deleteS3File(oldImageUrl);
            }
            String newImageUrl = s3Service.uploadFile(imageFile);
            component.setImageUrl(newImageUrl);
            return; // สิ้นสุดการทำงานในส่วนนี้
        }

        // === [UPDATE-2.2] กรณีต้องการลบรูปภาพที่มีอยู่: ลบไฟล์และตั้งค่า URL เป็น null ===
        if (removeImage && oldImageUrl != null && !oldImageUrl.isBlank()) {
            log.info("... removing existing image for component ID: {}", component.getId());
            deleteS3File(oldImageUrl);
            component.setImageUrl(null);
        }
    }

    /**
     * เมธอดภายในสำหรับสร้าง Component และ Inventory ใหม่
     */
    private Component createNewComponentAndInventory(ComponentRequest request, MultipartFile imageFile) {
        // === [CREATE-3.1] อัปโหลดรูปภาพไปที่ S3 (ถ้ามี) ===
        String imageUrl = null;
        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = s3Service.uploadFile(imageFile);
        }
        // === [CREATE-3.2] เรียกใช้ Converter เพื่อสร้าง Component Entity จาก Request ===
        Component componentEntity = componentConverter.convertRequestToEntity(request);
        componentEntity.setImageUrl(imageUrl);
        componentEntity.setActive(request.getQuantity() > 0);
        // === [CREATE-3.3] บันทึก Component Entity ลง DB เพื่อให้ได้ ID ===
        Component savedComponent = componentRepository.save(componentEntity);
        // === [CREATE-3.4] สร้างและบันทึก Inventory ที่เชื่อมโยงกับ Component ===
        Inventory inventory = Inventory.builder()
                .componentId(savedComponent.getId())
                .quantity(request.getQuantity())
                .price(request.getPrice())
                .build();
        inventoryRepository.save(inventory);
        return savedComponent;
    }

    /**
     * เมธอดภายในสำหรับปรับจำนวนสต็อกและสถานะ Active
     */
    private void performStockAdjustment(Component component, Inventory inventory, int quantityChange) {
        // === [ADJUST-STOCK-3.1] คำนวณสต็อกใหม่และตรวจสอบว่าไม่ติดลบ ===
        int currentQuantity = inventory.getQuantity();
        int newQuantity = currentQuantity + quantityChange;
        if (newQuantity < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove " + Math.abs(quantityChange) + " items. Only " + currentQuantity + " are in stock.");
        }
        // === [ADJUST-STOCK-3.2] ตั้งค่าสต็อกใหม่และอัปเดตสถานะ Active ของ Component ===
        inventory.setQuantity(newQuantity);
        component.setActive(newQuantity > 0);
    }

    /**
     * เมธอดสำหรับลบไฟล์ออกจาก S3 โดยดึงชื่อไฟล์จาก URL
     */
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

    /**
     * เมธอดสำหรับค้นหา Component จาก ID หรือโยน Exception หากไม่พบ
     */
    private Component findComponentById(String id) {
        return componentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Component not found with ID: " + id));
    }

    /**
     * เมธอดสำหรับค้นหา Inventory จาก Component ID หรือโยน Exception หากไม่พบ
     */
    private Inventory findInventoryByComponentId(String componentId) {
        return inventoryRepository.findByComponentId(componentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Data inconsistency: Inventory record not found for Component ID: " + componentId));
    }
}