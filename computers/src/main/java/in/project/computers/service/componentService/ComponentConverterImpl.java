package in.project.computers.service.componentService;

import in.project.computers.entity.component.Inventory;
import in.project.computers.entity.component.*;
import in.project.computers.entity.lookup.*;
import in.project.computers.dto.component.componentRequest.*;
import in.project.computers.dto.component.componentResponse.*;
import in.project.computers.repository.ComponentRepo.InventoryRepository;
import in.project.computers.repository.ComponentRepo.lookup.FormFactorRepository;
import in.project.computers.repository.ComponentRepo.lookup.RamTypeRepository;
import in.project.computers.repository.ComponentRepo.lookup.SocketRepository;
import in.project.computers.repository.ComponentRepo.lookup.StorageInterfaceRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@org.springframework.stereotype.Component
@RequiredArgsConstructor
@Slf4j
public class ComponentConverterImpl implements ComponentConverter {

    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final InventoryRepository inventoryRepository;

    private final Map<Class<? extends ComponentRequest>, Function<ComponentRequest, Component>> entityConverters = new HashMap<>();
    private final Map<Class<? extends Component>, Function<Component, ComponentResponse>> responseConverters = new HashMap<>();

    @PostConstruct
    private void initializeAllConverters() {
        log.info("Initializing component converters with new embedded model...");
        entityConverters.put(CpuRequest.class, req -> buildCpuEntity((CpuRequest) req));
        entityConverters.put(MotherboardRequest.class, req -> buildMotherboardEntity((MotherboardRequest) req));
        entityConverters.put(RamKitRequest.class, req -> buildRamKitEntity((RamKitRequest) req));
        entityConverters.put(GpuRequest.class, req -> buildGpuEntity((GpuRequest) req));
        entityConverters.put(PsuRequest.class, req -> buildPsuEntity((PsuRequest) req));
        entityConverters.put(CaseRequest.class, req -> buildCaseEntity((CaseRequest) req));
        entityConverters.put(CoolerRequest.class, req -> buildCoolerEntity((CoolerRequest) req));
        entityConverters.put(StorageDriveRequest.class, req -> buildStorageDriveEntity((StorageDriveRequest) req));

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

    // ... other @Override methods are unchanged ...
    @Override
    public Component convertRequestToEntity(ComponentRequest request) {
        Function<ComponentRequest, Component> converter = entityConverters.get(request.getClass());
        if (converter == null) {
            log.error("No entity converter found for request type: {}", request.getType());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown request type: " + request.getType());
        }
        return converter.apply(request);
    }

    @Override
    public ComponentResponse convertEntityToResponse(Component entity) {
        if (entity == null) {
            return null;
        }
        Function<Component, ComponentResponse> converter = responseConverters.get(entity.getClass());
        if (converter == null) {
            log.error("No response converter found for entity type: {}", entity.getType());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot create response for type: " + entity.getType());
        }
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

    private <B extends Component.ComponentBuilder<?, ?>> B setCommonEntityProperties(B builder, ComponentRequest request) {
        builder.mpn(request.getMpn())
                .type(request.getType())
                .name(request.getName())
                .description(request.getDescription());
        return builder;
    }


    private Case buildCaseEntity(CaseRequest request) {
        List<FormFactor> motherboardFormFactors = request.getMotherboard_form_factor_support()
                .stream().map(name -> formFactorRepository.findByNameAndType(name, FormFactorType.MOTHERBOARD)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Motherboard form factor in list: " + name)))
                .collect(Collectors.toList());

        List<FormFactor> psuFormFactors = request.getPsu_form_factor_support()
                .stream().map(name -> formFactorRepository.findByNameAndType(name, FormFactorType.PSU)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PSU form factor in list: " + name)))
                .collect(Collectors.toList());

        return setCommonEntityProperties(Case.builder()
                .supportedFormFactors(motherboardFormFactors)
                .supportedPsuFormFactors(psuFormFactors)
                .max_gpu_length_mm(request.getMax_gpu_length_mm())
                .max_cooler_height_mm(request.getMax_cooler_height_mm())
                .bays_2_5_inch(request.getBays_2_5_inch())
                .bays_3_5_inch(request.getBays_3_5_inch())
                .supportedRadiatorSizesMm(request.getSupportedRadiatorSizesMm()), request) // ADDED
                .build();
    }

    // ... Motherboard, Psu, StorageDrive entities are unchanged ...
    private Motherboard buildMotherboardEntity(MotherboardRequest request) {
        Socket socket = socketRepository.findByName(request.getSocket()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid socket: " + request.getSocket()));
        RamType ramType = ramTypeRepository.findByName(request.getRam_type()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RAM type: " + request.getRam_type()));
        FormFactor formFactor = formFactorRepository.findByNameAndType(request.getForm_factor(), FormFactorType.MOTHERBOARD)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Motherboard form factor: " + request.getForm_factor()));

        return setCommonEntityProperties(
                Motherboard.builder()
                        .socket(socket)
                        .ramType(ramType)
                        .formFactor(formFactor)
                        .max_ram_gb(request.getMax_ram_gb())
                        .pcie_x16_slot_count(request.getPcie_x16_slot_count())
                        .wattage(request.getWattage())
                        .ram_slot_count(request.getRam_slot_count())
                        .sata_port_count(request.getSata_port_count())
                        .m2_slot_count(request.getM2_slot_count()), request)
                .build();
    }

    private Psu buildPsuEntity(PsuRequest request) {
        FormFactor formFactor = formFactorRepository.findByNameAndType(request.getForm_factor(), FormFactorType.PSU)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PSU form factor: " + request.getForm_factor()));
        return setCommonEntityProperties(Psu.builder()
                .wattage(request.getWattage())
                .formFactor(formFactor), request)
                .build();
    }

    private StorageDrive buildStorageDriveEntity(StorageDriveRequest request) {
        StorageInterface storageInterface = storageInterfaceRepository.findByName(request.getStorage_interface())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage interface: " + request.getStorage_interface()));
        FormFactor formFactor = formFactorRepository.findByNameAndType(request.getForm_factor(), FormFactorType.STORAGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Storage form factor: " + request.getForm_factor()));

        return setCommonEntityProperties(StorageDrive.builder()
                .storageInterface(storageInterface)
                .capacity_gb(request.getCapacity_gb())
                .formFactor(formFactor), request)
                .build();
    }

    private Cooler buildCoolerEntity(CoolerRequest request) {
        List<Socket> sockets = request.getSocket_support()
                .stream()
                .map(name -> socketRepository.findByName(name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid socket in list: " + name)))
                .collect(Collectors.toList());

        return setCommonEntityProperties(Cooler.builder()
                .supportedSockets(sockets)
                .height_mm(request.getHeight_mm())
                .wattage(request.getWattage())
                .radiatorSize_mm(request.getRadiatorSize_mm()), request) // ADDED
                .build();
    }

    // ... RamKit, Cpu, Gpu builders are unchanged ...
    private RamKit buildRamKitEntity(RamKitRequest request) {
        RamType ramType = ramTypeRepository.findByName(request.getRam_type()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RAM type: " + request.getRam_type()));
        return setCommonEntityProperties(RamKit.builder()
                .ramType(ramType)
                .ram_size_gb(request.getRam_size_gb())
                .moduleCount(request.getModuleCount())
                .wattage(request.getWattage()), request)
                .build();
    }

    private Cpu buildCpuEntity(CpuRequest request) {
        Socket socket = socketRepository.findByName(request.getSocket()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid socket: " + request.getSocket()));
        return setCommonEntityProperties(
                Cpu.builder()
                        .socket(socket)
                        .wattage(request.getWattage()), request)
                .build();
    }

    private Gpu buildGpuEntity(GpuRequest request) {
        return setCommonEntityProperties(Gpu.builder()
                .wattage(request.getWattage())
                .length_mm(request.getLength_mm()), request)
                .build();
    }

    private <B extends ComponentResponse.ComponentResponseBuilder<?, ?>> B setCommonResponseProperties(B builder, Component entity) {
        Optional<Inventory> inventoryOpt = inventoryRepository.findByComponentId(entity.getId());
        int quantity = inventoryOpt.map(Inventory::getQuantity).orElse(0);
        BigDecimal price = inventoryOpt.map(Inventory::getPrice).orElse(BigDecimal.ZERO);

        builder.id(entity.getId())
                .mpn(entity.getMpn())
                .isActive(entity.isActive())
                .type(entity.getType())
                .name(entity.getName())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .quantity(quantity)
                .price(price);
        return builder;
    }

    private CaseResponse buildCaseResponse(Case entity) {
        List<String> motherboardFFNames = (entity.getSupportedFormFactors() == null) ? Collections.emptyList() :
                entity.getSupportedFormFactors().stream().map(FormFactor::getName).collect(Collectors.toList());
        List<String> psuFFNames = (entity.getSupportedPsuFormFactors() == null) ? Collections.emptyList() :
                entity.getSupportedPsuFormFactors().stream().map(FormFactor::getName).collect(Collectors.toList());

        return setCommonResponseProperties(CaseResponse.builder()
                .motherboard_form_factor_support(motherboardFFNames)
                .psu_form_factor_support(psuFFNames)
                .max_gpu_length_mm(entity.getMax_gpu_length_mm())
                .max_cooler_height_mm(entity.getMax_cooler_height_mm())
                .bays_2_5_inch(entity.getBays_2_5_inch())
                .bays_3_5_inch(entity.getBays_3_5_inch())
                .supportedRadiatorSizesMm( // ADDED
                        (entity.getSupportedRadiatorSizesMm() == null) ? Collections.emptyList() : entity.getSupportedRadiatorSizesMm()
                ), entity)
                .build();
    }

    // ... Psu, StorageDrive, Cpu, Motherboard, RamKit response builders are unchanged ...
    private PsuResponse buildPsuResponse(Psu entity) {
        String formFactorName = Optional.ofNullable(entity.getFormFactor()).map(FormFactor::getName).orElse("N/A");
        return setCommonResponseProperties(PsuResponse.builder()
                .wattage(entity.getWattage())
                .form_factor(formFactorName), entity)
                .build();
    }

    private StorageDriveResponse buildStorageDriveResponse(StorageDrive entity) {
        String interfaceName = Optional.ofNullable(entity.getStorageInterface()).map(StorageInterface::getName).orElse("N/A");
        String formFactorName = Optional.ofNullable(entity.getFormFactor()).map(FormFactor::getName).orElse("N/A");
        return setCommonResponseProperties(StorageDriveResponse.builder()
                .storage_interface(interfaceName)
                .capacity_gb(entity.getCapacity_gb())
                .form_factor(formFactorName), entity)
                .build();
    }

    private CpuResponse buildCpuResponse(Cpu entity) {
        String socketName = Optional.ofNullable(entity.getSocket()).map(Socket::getName).orElse("N/A");
        return setCommonResponseProperties(CpuResponse.builder()
                .socket(socketName)
                .wattage(entity.getWattage()), entity)
                .build();
    }

    private MotherboardResponse buildMotherboardResponse(Motherboard entity) {
        String socketName = Optional.ofNullable(entity.getSocket()).map(Socket::getName).orElse("N/A");
        String ramTypeName = Optional.ofNullable(entity.getRamType()).map(RamType::getName).orElse("N/A");
        String formFactorName = Optional.ofNullable(entity.getFormFactor()).map(FormFactor::getName).orElse("N/A");
        return setCommonResponseProperties(MotherboardResponse.builder()
                .socket(socketName)
                .ram_type(ramTypeName)
                .form_factor(formFactorName)
                .wattage(entity.getWattage())
                .max_ram_gb(entity.getMax_ram_gb())
                .pcie_x16_slot_count(entity.getPcie_x16_slot_count())
                .ram_slot_count(entity.getRam_slot_count())
                .sata_port_count(entity.getSata_port_count())
                .m2_slot_count(entity.getM2_slot_count()), entity)
                .build();
    }

    private RamKitResponse buildRamKitResponse(RamKit entity) {
        String ramTypeName = Optional.ofNullable(entity.getRamType()).map(RamType::getName).orElse("N/A");
        return setCommonResponseProperties(RamKitResponse.builder()
                .ram_type(ramTypeName)
                .ram_size_gb(entity.getRam_size_gb())
                .moduleCount(entity.getModuleCount())
                .wattage(entity.getWattage()), entity)
                .build();
    }

    private CoolerResponse buildCoolerResponse(Cooler entity) {
        List<String> socketNames = (entity.getSupportedSockets() == null) ? Collections.emptyList() :
                entity.getSupportedSockets().stream().map(Socket::getName).collect(Collectors.toList());
        return setCommonResponseProperties(CoolerResponse.builder()
                .socket_support(socketNames)
                .height_mm(entity.getHeight_mm())
                .wattage(entity.getWattage())
                .radiatorSize_mm(entity.getRadiatorSize_mm()), entity) // ADDED
                .build();
    }

    // Gpu response builder is unchanged
    private GpuResponse buildGpuResponse(Gpu entity) {
        return setCommonResponseProperties(GpuResponse.builder()
                .wattage(entity.getWattage())
                .length_mm(entity.getLength_mm()), entity)
                .build();
    }


    @Override
    public void updateEntityFromRequest(Component entityToUpdate, ComponentRequest request) {
        log.debug("Updating entity of type {} from request of type {}", entityToUpdate.getClass().getSimpleName(), request.getClass().getSimpleName());
        // First, update the common properties
        updateCommonProperties(entityToUpdate, request);

        // Then, update the specific properties based on the entity type
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

    // =========================================================================
    // SECTION:(NEW SECTION)
    // =========================================================================

    private void updateCommonProperties(Component entity, ComponentRequest request) {
        entity.setName(request.getName());
        entity.setMpn(request.getMpn());
        entity.setDescription(request.getDescription());
    }

    private void updateCpuEntity(Cpu entity, CpuRequest request) {
        entity.setWattage(request.getWattage());
        Socket socket = socketRepository.findByName(request.getSocket()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid socket: " + request.getSocket()));
        entity.setSocket(socket);
    }

    private void updateMotherboardEntity(Motherboard entity, MotherboardRequest request) {
        Socket socket = socketRepository.findByName(request.getSocket()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid socket: " + request.getSocket()));
        RamType ramType = ramTypeRepository.findByName(request.getRam_type()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RAM type: " + request.getRam_type()));
        FormFactor formFactor = formFactorRepository.findByNameAndType(request.getForm_factor(), FormFactorType.MOTHERBOARD)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Motherboard form factor: " + request.getForm_factor()));
        entity.setSocket(socket);
        entity.setRamType(ramType);
        entity.setFormFactor(formFactor);
        entity.setWattage(request.getWattage());
        entity.setMax_ram_gb(request.getMax_ram_gb());
        entity.setPcie_x16_slot_count(request.getPcie_x16_slot_count());
        entity.setRam_slot_count(request.getRam_slot_count());
        entity.setSata_port_count(request.getSata_port_count());
        entity.setM2_slot_count(request.getM2_slot_count());
    }

    private void updateRamKitEntity(RamKit entity, RamKitRequest request) {
        RamType ramType = ramTypeRepository.findByName(request.getRam_type()).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid RAM type: " + request.getRam_type()));
        entity.setRamType(ramType);
        entity.setRam_size_gb(request.getRam_size_gb());
        entity.setModuleCount(request.getModuleCount());
        entity.setWattage(request.getWattage());
    }

    private void updateGpuEntity(Gpu entity, GpuRequest request) {
        entity.setWattage(request.getWattage());
        entity.setLength_mm(request.getLength_mm());
    }

    private void updatePsuEntity(Psu entity, PsuRequest request) {
        FormFactor formFactor = formFactorRepository.findByNameAndType(request.getForm_factor(), FormFactorType.PSU)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PSU form factor: " + request.getForm_factor()));
        entity.setWattage(request.getWattage());
        entity.setFormFactor(formFactor);
    }

    private void updateStorageDriveEntity(StorageDrive entity, StorageDriveRequest request) {
        StorageInterface storageInterface = storageInterfaceRepository.findByName(request.getStorage_interface())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage interface: " + request.getStorage_interface()));
        FormFactor formFactor = formFactorRepository.findByNameAndType(request.getForm_factor(), FormFactorType.STORAGE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Storage form factor: " + request.getForm_factor()));
        entity.setStorageInterface(storageInterface);
        entity.setCapacity_gb(request.getCapacity_gb());
        entity.setFormFactor(formFactor);
    }

    private void updateCoolerEntity(Cooler entity, CoolerRequest request) {
        List<Socket> sockets = request.getSocket_support()
                .stream()
                .map(name -> socketRepository.findByName(name)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid socket in list: " + name)))
                .collect(Collectors.toList());
        entity.setSupportedSockets(sockets);
        entity.setHeight_mm(request.getHeight_mm());
        entity.setWattage(request.getWattage());
        entity.setRadiatorSize_mm(request.getRadiatorSize_mm());
    }

    private void updateCaseEntity(Case entity, CaseRequest request) {
        List<FormFactor> motherboardFormFactors = request.getMotherboard_form_factor_support()
                .stream().map(name -> formFactorRepository.findByNameAndType(name, FormFactorType.MOTHERBOARD)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Motherboard form factor in list: " + name)))
                .collect(Collectors.toList());

        List<FormFactor> psuFormFactors = request.getPsu_form_factor_support()
                .stream().map(name -> formFactorRepository.findByNameAndType(name, FormFactorType.PSU)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid PSU form factor in list: " + name)))
                .collect(Collectors.toList());
        entity.setSupportedFormFactors(motherboardFormFactors);
        entity.setSupportedPsuFormFactors(psuFormFactors);
        entity.setMax_gpu_length_mm(request.getMax_gpu_length_mm());
        entity.setMax_cooler_height_mm(request.getMax_cooler_height_mm());
        entity.setBays_2_5_inch(request.getBays_2_5_inch());
        entity.setBays_3_5_inch(request.getBays_3_5_inch());
        entity.setSupportedRadiatorSizesMm(request.getSupportedRadiatorSizesMm());
    }
}