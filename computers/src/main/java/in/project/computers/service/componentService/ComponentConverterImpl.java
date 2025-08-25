package in.project.computers.service.componentService;

import in.project.computers.entity.component.Inventory;
import in.project.computers.entity.component.*;
import in.project.computers.entity.lookup.*;
import in.project.computers.DTO.component.componentRequest.*;
import in.project.computers.DTO.component.componentResponse.*;
import in.project.computers.repository.componentRepository.InventoryRepository;
import in.project.computers.repository.lookupRepository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// คลาสสำหรับแปลงข้อมูลระหว่าง Request DTO, Entity, และ Response DTO ของ Component
@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
public class ComponentConverterImpl implements ComponentConverter {

    // === [DEPENDENCIES] ประกาศ Dependencies และ Map สำหรับเก็บฟังก์ชันแปลงข้อมูล ===
    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final InventoryRepository inventoryRepository;
    private final BrandRepository brandRepository;

    private final Map<Class<? extends ComponentRequest>, Function<ComponentRequest, Component>> entityConverters = new HashMap<>();
    private final Map<Class<? extends Component>, Function<Component, ComponentResponse>> responseConverters = new HashMap<>();

    @PostConstruct
    private void initializeAllConverters() {
        log.info("Initializing component converters...");
        // === [SETUP-1] กำหนดค่าเริ่มต้นให้กับ Map สำหรับการแปลง Request -> Entity ===
        entityConverters.put(CpuRequest.class, req -> buildCpuEntity((CpuRequest) req));
        entityConverters.put(MotherboardRequest.class, req -> buildMotherboardEntity((MotherboardRequest) req));
        entityConverters.put(RamKitRequest.class, req -> buildRamKitEntity((RamKitRequest) req));
        entityConverters.put(GpuRequest.class, req -> buildGpuEntity((GpuRequest) req));
        entityConverters.put(PsuRequest.class, req -> buildPsuEntity((PsuRequest) req));
        entityConverters.put(CaseRequest.class, req -> buildCaseEntity((CaseRequest) req));
        entityConverters.put(CoolerRequest.class, req -> buildCoolerEntity((CoolerRequest) req));
        entityConverters.put(StorageDriveRequest.class, req -> buildStorageDriveEntity((StorageDriveRequest) req));

        // === [SETUP-2] กำหนดค่าเริ่มต้นให้กับ Map สำหรับการแปลง Entity -> Response ===
        responseConverters.put(Cpu.class, entity -> buildCpuResponse((Cpu) entity));
        responseConverters.put(Motherboard.class, entity -> buildMotherboardResponse((Motherboard) entity));
        responseConverters.put(RamKit.class, entity -> buildRamKitResponse((RamKit) entity));
        responseConverters.put(Gpu.class, entity -> buildGpuResponse((Gpu) entity));
        responseConverters.put(Psu.class, entity -> buildPsuResponse((Psu) entity));
        responseConverters.put(Case.class, entity -> buildCaseResponse((Case) entity));
        responseConverters.put(Cooler.class, entity -> buildCoolerResponse((Cooler) entity));
        responseConverters.put(StorageDrive.class, entity -> buildStorageDriveResponse((StorageDrive) entity));
        log.info("Component converters initialized successfully.");
    }

    @Override
    public Component convertRequestToEntity(ComponentRequest request) {
        // === [CREATE-3.2.1] ค้นหาฟังก์ชันแปลงที่เหมาะสมจาก Map ตามประเภทของ Request ===
        Function<ComponentRequest, Component> converter = entityConverters.get(request.getClass());
        if (converter == null) {
            log.error("No entity converter found for request type: {}", request.getType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown request type: " + request.getType());
        }
        // === [CREATE-3.2.2] เรียกใช้ฟังก์ชันแปลงเพื่อสร้าง Entity ===
        return converter.apply(request);
    }

    @Override
    public ComponentResponse convertEntityToResponse(Component entity) {
        if (entity == null) {
            return null;
        }
        // === [RESPONSE-CONV-1] ค้นหาฟังก์ชันแปลงที่เหมาะสมจาก Map ตามประเภทของ Entity ===
        Function<Component, ComponentResponse> converter = responseConverters.get(entity.getClass());
        if (converter == null) {
            log.error("No response converter found for entity type: {}", entity.getType());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot create response for type: " + entity.getType());
        }
        // === [RESPONSE-CONV-2] เรียกใช้ฟังก์ชันแปลงเพื่อสร้าง DTO Response ===
        return converter.apply(entity);
    }

    @Override
    public <T extends Component, R extends ComponentResponse> R convertEntityToResponse(T entity, Class<R> responseClass) {
        if (entity == null) {
            return null;
        }
        ComponentResponse baseResponse = convertEntityToResponse(entity);
        return responseClass.cast(baseResponse);
    }

    @Override
    public void updateEntityFromRequest(Component entityToUpdate, ComponentRequest request) {
        log.debug("Updating entity of type {} from request of type {}", entityToUpdate.getClass().getSimpleName(), request.getClass().getSimpleName());
        // === [UPDATE-4.1] อัปเดตคุณสมบัติพื้นฐาน (Common Properties) ที่มีในทุก Component ===
        updateCommonProperties(entityToUpdate, request);

        // === [UPDATE-4.2] ใช้ switch-case เพื่อเรียกเมธอดอัปเดตเฉพาะสำหรับแต่ละประเภทของ Component ===
        switch (entityToUpdate) {
            case Cpu cpu -> updateCpuEntity(cpu, (CpuRequest) request);
            case Motherboard motherboard -> updateMotherboardEntity(motherboard, (MotherboardRequest) request);
            case RamKit ramKit -> updateRamKitEntity(ramKit, (RamKitRequest) request);
            case Gpu gpu -> updateGpuEntity(gpu, (GpuRequest) request);
            case Psu psu -> updatePsuEntity(psu, (PsuRequest) request);
            case Case aCase -> updateCaseEntity(aCase, (CaseRequest) request);
            case Cooler cooler -> updateCoolerEntity(cooler, (CoolerRequest) request);
            case StorageDrive storageDrive -> updateStorageDriveEntity(storageDrive, (StorageDriveRequest) request);
            default -> {
                log.error("No specific update logic found for entity type: {}", entityToUpdate.getClass().getSimpleName());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot update entity of type: " + entityToUpdate.getType());
            }
        }
    }

    // --- Private Helper Methods: Common Property Setters ---
    private <B extends Component.ComponentBuilder<?, ?>> B setCommonEntityProperties(B builder, ComponentRequest request) {
        builder.mpn(request.getMpn())
                .type(request.getType())
                .name(request.getName())
                .description(request.getDescription())
                .brand(findBrandById(request.getBrandId()));
        return builder;
    }

    private <B extends ComponentResponse.ComponentResponseBuilder<?, ?>> B setCommonResponseProperties(B builder, Component entity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByComponentId(entity.getId());
        builder.id(entity.getId())
                .mpn(entity.getMpn())
                .isActive(entity.isActive())
                .type(entity.getType())
                .name(entity.getName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .quantity(inventoryOpt.map(Inventory::getQuantity).orElse(0))
                .price(inventoryOpt.map(Inventory::getPrice).orElse(BigDecimal.ZERO))
                .brandName(getOptionalName(entity.getBrand(), Brand::getName));
        return builder;
    }

    private void updateCommonProperties(Component entity, ComponentRequest request) {
        entity.setName(request.getName())
                .setMpn(request.getMpn())
                .setDescription(request.getDescription());

        if (entity.getBrand() == null || !entity.getBrand().getId().equals(request.getBrandId())) {
            entity.setBrand(findBrandById(request.getBrandId()));
        }
    }

    // --- Private Helper Methods: Build Entity ---
    private Case buildCaseEntity(CaseRequest request) {
        return setCommonEntityProperties(Case.builder()
                .supportedFormFactors(findFormFactorsByNames(request.getMotherboard_form_factor_support(), FormFactorType.MOTHERBOARD))
                .supportedPsuFormFactors(findFormFactorsByNames(request.getPsu_form_factor_support(), FormFactorType.PSU))
                .max_gpu_length_mm(request.getMax_gpu_length_mm())
                .max_cooler_height_mm(request.getMax_cooler_height_mm())
                .bays_2_5_inch(request.getBays_2_5_inch())
                .bays_3_5_inch(request.getBays_3_5_inch())
                .supportedRadiatorSizesMm(request.getSupportedRadiatorSizesMm()), request)
                .build();
    }

    private Motherboard buildMotherboardEntity(MotherboardRequest request) {
        return setCommonEntityProperties(Motherboard.builder()
                .socket(findSocketByName(request.getSocket()))
                .ramType(findRamTypeByName(request.getRam_type()))
                .formFactor(findFormFactorByNameAndType(request.getForm_factor(), FormFactorType.MOTHERBOARD))
                .max_ram_gb(request.getMax_ram_gb())
                .pcie_x16_slot_count(request.getPcie_x16_slot_count())
                .wattage(request.getWattage())
                .ram_slot_count(request.getRam_slot_count())
                .sata_port_count(request.getSata_port_count())
                .m2_slot_count(request.getM2_slot_count()), request)
                .build();
    }

    private Psu buildPsuEntity(PsuRequest request) {
        return setCommonEntityProperties(Psu.builder()
                .wattage(request.getWattage())
                .formFactor(findFormFactorByNameAndType(request.getForm_factor(), FormFactorType.PSU)), request)
                .build();
    }

    private StorageDrive buildStorageDriveEntity(StorageDriveRequest request) {
        return setCommonEntityProperties(StorageDrive.builder()
                .storageInterface(findStorageInterfaceByName(request.getStorage_interface()))
                .capacity_gb(request.getCapacity_gb())
                .formFactor(findFormFactorByNameAndType(request.getForm_factor(), FormFactorType.STORAGE)), request)
                .build();
    }

    private Cooler buildCoolerEntity(CoolerRequest request) {
        return setCommonEntityProperties(Cooler.builder()
                .supportedSockets(findSocketsByNames(request.getSocket_support()))
                .height_mm(request.getHeight_mm())
                .wattage(request.getWattage())
                .radiatorSize_mm(request.getRadiatorSize_mm()), request)
                .build();
    }

    private RamKit buildRamKitEntity(RamKitRequest request) {
        return setCommonEntityProperties(RamKit.builder()
                .ramType(findRamTypeByName(request.getRam_type()))
                .ram_size_gb(request.getRam_size_gb())
                .moduleCount(request.getModuleCount())
                .wattage(request.getWattage()), request)
                .build();
    }

    private Cpu buildCpuEntity(CpuRequest request) {
        return setCommonEntityProperties(Cpu.builder()
                .socket(findSocketByName(request.getSocket()))
                .wattage(request.getWattage()), request)
                .build();
    }

    private Gpu buildGpuEntity(GpuRequest request) {
        return setCommonEntityProperties(Gpu.builder()
                .wattage(request.getWattage())
                .length_mm(request.getLength_mm()), request)
                .build();
    }

    // --- Private Helper Methods: Build Response DTO (per type) ---
    private CaseResponse buildCaseResponse(Case entity) {
        return setCommonResponseProperties(CaseResponse.builder()
                .motherboard_form_factor_support(getOptionalNames(entity.getSupportedFormFactors(), FormFactor::getName))
                .psu_form_factor_support(getOptionalNames(entity.getSupportedPsuFormFactors(), FormFactor::getName))
                .max_gpu_length_mm(entity.getMax_gpu_length_mm())
                .max_cooler_height_mm(entity.getMax_cooler_height_mm())
                .bays_2_5_inch(entity.getBays_2_5_inch())
                .bays_3_5_inch(entity.getBays_3_5_inch())
                .supportedRadiatorSizesMm((entity.getSupportedRadiatorSizesMm() == null) ? Collections.emptyList() : entity.getSupportedRadiatorSizesMm()), entity)
                .build();
    }

    private PsuResponse buildPsuResponse(Psu entity) {
        return setCommonResponseProperties(PsuResponse.builder()
                .wattage(entity.getWattage())
                .form_factor(getOptionalName(entity.getFormFactor(), FormFactor::getName)), entity)
                .build();
    }

    private StorageDriveResponse buildStorageDriveResponse(StorageDrive entity) {
        return setCommonResponseProperties(StorageDriveResponse.builder()
                .storage_interface(getOptionalName(entity.getStorageInterface(), StorageInterface::getName))
                .capacity_gb(entity.getCapacity_gb())
                .form_factor(getOptionalName(entity.getFormFactor(), FormFactor::getName)), entity)
                .build();
    }

    private CpuResponse buildCpuResponse(Cpu entity) {
        return setCommonResponseProperties(CpuResponse.builder()
                .socket(getOptionalName(entity.getSocket(), Socket::getName))
                .wattage(entity.getWattage()), entity)
                .build();
    }

    private MotherboardResponse buildMotherboardResponse(Motherboard entity) {
        return setCommonResponseProperties(MotherboardResponse.builder()
                .socket(getOptionalName(entity.getSocket(), Socket::getName))
                .ram_type(getOptionalName(entity.getRamType(), RamType::getName))
                .form_factor(getOptionalName(entity.getFormFactor(), FormFactor::getName))
                .wattage(entity.getWattage())
                .max_ram_gb(entity.getMax_ram_gb())
                .pcie_x16_slot_count(entity.getPcie_x16_slot_count())
                .ram_slot_count(entity.getRam_slot_count())
                .sata_port_count(entity.getSata_port_count())
                .m2_slot_count(entity.getM2_slot_count()), entity)
                .build();
    }

    private RamKitResponse buildRamKitResponse(RamKit entity) {
        return setCommonResponseProperties(RamKitResponse.builder()
                .ram_type(getOptionalName(entity.getRamType(), RamType::getName))
                .ram_size_gb(entity.getRam_size_gb())
                .moduleCount(entity.getModuleCount())
                .wattage(entity.getWattage()), entity)
                .build();
    }

    private CoolerResponse buildCoolerResponse(Cooler entity) {
        return setCommonResponseProperties(CoolerResponse.builder()
                .socket_support(getOptionalNames(entity.getSupportedSockets(), Socket::getName))
                .height_mm(entity.getHeight_mm())
                .wattage(entity.getWattage())
                .radiatorSize_mm(entity.getRadiatorSize_mm()), entity)
                .build();
    }

    private GpuResponse buildGpuResponse(Gpu entity) {
        return setCommonResponseProperties(GpuResponse.builder()
                .wattage(entity.getWattage())
                .length_mm(entity.getLength_mm()), entity)
                .build();
    }

    // --- Private Helper Methods: Update Entity (per type) ---
    private void updateCpuEntity(Cpu entity, CpuRequest request) {
        entity.setWattage(request.getWattage())
                .setSocket(findSocketByName(request.getSocket()));
    }

    private void updateMotherboardEntity(Motherboard entity, MotherboardRequest request) {
        entity.setSocket(findSocketByName(request.getSocket()))
                .setRamType(findRamTypeByName(request.getRam_type()))
                .setFormFactor(findFormFactorByNameAndType(request.getForm_factor(), FormFactorType.MOTHERBOARD))
                .setWattage(request.getWattage())
                .setMax_ram_gb(request.getMax_ram_gb())
                .setPcie_x16_slot_count(request.getPcie_x16_slot_count())
                .setRam_slot_count(request.getRam_slot_count())
                .setSata_port_count(request.getSata_port_count())
                .setM2_slot_count(request.getM2_slot_count());
    }

    private void updateRamKitEntity(RamKit entity, RamKitRequest request) {
        entity.setRamType(findRamTypeByName(request.getRam_type()))
                .setRam_size_gb(request.getRam_size_gb())
                .setModuleCount(request.getModuleCount())
                .setWattage(request.getWattage());
    }

    private void updateGpuEntity(Gpu entity, GpuRequest request) {
        entity.setWattage(request.getWattage())
                .setLength_mm(request.getLength_mm());
    }

    private void updatePsuEntity(Psu entity, PsuRequest request) {
        entity.setWattage(request.getWattage())
                .setFormFactor(findFormFactorByNameAndType(request.getForm_factor(), FormFactorType.PSU));
    }

    private void updateStorageDriveEntity(StorageDrive entity, StorageDriveRequest request) {
        entity.setStorageInterface(findStorageInterfaceByName(request.getStorage_interface()))
                .setCapacity_gb(request.getCapacity_gb())
                .setFormFactor(findFormFactorByNameAndType(request.getForm_factor(), FormFactorType.STORAGE));
    }

    private void updateCoolerEntity(Cooler entity, CoolerRequest request) {
        entity.setSupportedSockets(findSocketsByNames(request.getSocket_support()))
                .setHeight_mm(request.getHeight_mm())
                .setWattage(request.getWattage())
                .setRadiatorSize_mm(request.getRadiatorSize_mm());
    }

    private void updateCaseEntity(Case entity, CaseRequest request) {
        entity.setSupportedFormFactors(findFormFactorsByNames(request.getMotherboard_form_factor_support(), FormFactorType.MOTHERBOARD))
                .setSupportedPsuFormFactors(findFormFactorsByNames(request.getPsu_form_factor_support(), FormFactorType.PSU))
                .setMax_gpu_length_mm(request.getMax_gpu_length_mm())
                .setMax_cooler_height_mm(request.getMax_cooler_height_mm())
                .setBays_2_5_inch(request.getBays_2_5_inch())
                .setBays_3_5_inch(request.getBays_3_5_inch())
                .setSupportedRadiatorSizesMm(request.getSupportedRadiatorSizesMm());
    }

    // --- Private Helper Methods: Lookup Finders ---
    private Brand findBrandById(String id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Brand ID: " + id));
    }

    private Socket findSocketByName(String name) {
        return socketRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid socket: " + name));
    }

    private List<Socket> findSocketsByNames(List<String> names) {
        if (names == null) return Collections.emptyList();
        return names.stream()
                .map(this::findSocketByName)
                .collect(Collectors.toList());
    }

    private RamType findRamTypeByName(String name) {
        return ramTypeRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RAM type: " + name));
    }

    private FormFactor findFormFactorByNameAndType(String name, FormFactorType type) {
        return formFactorRepository.findByNameAndType(name, type)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid " + type.name() + " form factor: " + name));
    }

    private List<FormFactor> findFormFactorsByNames(List<String> names, FormFactorType type) {
        if (names == null) return Collections.emptyList();
        return names.stream()
                .map(name -> findFormFactorByNameAndType(name, type))
                .collect(Collectors.toList());
    }

    private StorageInterface findStorageInterfaceByName(String name) {
        return storageInterfaceRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage interface: " + name));
    }

    // --- Private Helper Methods: Utility ---
    private <T> String getOptionalName(T entity, Function<T, String> nameExtractor) {
        return Optional.ofNullable(entity).map(nameExtractor).orElse(null);
    }

    private <T> List<String> getOptionalNames(List<T> entities, Function<T, String> nameExtractor) {
        if (entities == null || entities.isEmpty()) {
            return Collections.emptyList();
        }
        return entities.stream().map(nameExtractor).collect(Collectors.toList());
    }
}