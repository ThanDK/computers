package in.project.computers.service.componentService;

import in.project.computers.DTO.lookup.*;
import in.project.computers.entity.lookup.*;

import in.project.computers.repository.componentRepository.ComponentRepository;
import in.project.computers.repository.lookupRepository.*;
import in.project.computers.service.awsS3Bucket.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

// คลาสหลักสำหรับจัดการ Business Logic ทั้งหมดที่เกี่ยวกับข้อมูลอ้างอิง (Lookup Data)
@Service
@RequiredArgsConstructor
@Slf4j
public class LookupServiceImpl implements LookupService {

    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final ComponentRepository componentRepository;
    private final ShippingProviderRepository shippingProviderRepository;
    private final BrandRepository brandRepository;
    private final S3Service s3Service;

    @Override
    public Map<String, Object> getAllLookups() {
        // === [GET-ALL-LOOKUPS-1] สร้าง Map สำหรับรวบรวมข้อมูล Lookup ทั้งหมด ===
        Map<String, Object> lookups = new HashMap<>();
        lookups.put("sockets", socketRepository.findAll());
        lookups.put("ramTypes", ramTypeRepository.findAll());
        lookups.put("storageInterfaces", storageInterfaceRepository.findAll());
        lookups.put("shippingProviders", shippingProviderRepository.findAll());
        lookups.put("brands", brandRepository.findAll());

        // === [GET-ALL-LOOKUPS-2] ดึงข้อมูล FormFactor และจัดกลุ่มตาม Type (MOTHERBOARD, PSU, STORAGE) ===
        Map<FormFactorType, List<FormFactor>> groupedFormFactors = formFactorRepository.findAll().stream()
                .collect(Collectors.groupingBy(FormFactor::getType));
        lookups.put("formFactors", groupedFormFactors);

        // === [GET-ALL-LOOKUPS-3] เพิ่มข้อมูลขนาด Radiator ที่เป็นค่าคงที่ (Hardcoded) ===
        lookups.put("radiatorSizes", List.of(120, 140, 240, 280, 360, 420));

        // === [GET-ALL-LOOKUPS-4] ส่งคืน Map ที่มีข้อมูล Lookup ทั้งหมด ===
        return lookups;
    }

    // === Individual Getters ===
    @Override
    public List<Socket> getAllSockets() { return socketRepository.findAll(); }
    @Override
    public List<RamType> getAllRamTypes() { return ramTypeRepository.findAll(); }
    @Override
    public List<FormFactor> getAllFormFactors() { return formFactorRepository.findAll(); }
    @Override
    public List<StorageInterface> getAllStorageInterfaces() { return storageInterfaceRepository.findAll(); }
    @Override
    public List<ShippingProvider> getAllShippingProviders() { return shippingProviderRepository.findAll(); }
    @Override
    public List<Brand> getAllBrands() { return brandRepository.findAll(); }

    // --- Socket CRUD ---
    @Override
    public Socket createSocket(SocketRequest request) {
        // === [CREATE-SOCKET-1] ตรวจสอบว่ามี Socket ชื่อนี้อยู่แล้วหรือไม่ ===
        if (socketRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Socket with name '" + request.getName() + "' already exists.");
        }
        // === [CREATE-SOCKET-2] สร้างอ็อบเจกต์ Socket ใหม่จาก Request ===
        Socket socket = new Socket(null, request.getName(), request.getBrand());
        // === [CREATE-SOCKET-3] บันทึกและส่งคืนข้อมูลที่สร้างใหม่ ===
        return socketRepository.save(socket);
    }

    @Override
    public Socket updateSocket(String id, SocketRequest request) {
        // === [UPDATE-SOCKET-1] ค้นหา Socket ที่ต้องการอัปเดตจาก ID ===
        Socket socket = socketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Socket not found with id: " + id));

        // === [UPDATE-SOCKET-2] ตรวจสอบเงื่อนไขป้องกัน: ห้ามเปลี่ยนชื่อ Socket หากมีการใช้งานอยู่ ===
        if (!socket.getName().equals(request.getName()) && componentRepository.existsBySocketId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the name of this Socket because it is currently in use. Please create a new Socket instead.");
        }

        // === [UPDATE-SOCKET-3] ตรวจสอบว่าชื่อใหม่ซ้ำกับ Socket อื่นที่มีอยู่แล้วหรือไม่ ===
        Optional<Socket> existingByName = socketRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another socket with name '" + request.getName() + "' already exists.");
        }
        // === [UPDATE-SOCKET-4] อัปเดตข้อมูลและบันทึก ===
        socket.setName(request.getName());
        socket.setBrand(request.getBrand());
        return socketRepository.save(socket);
    }

    @Override
    public void deleteSocket(String id) {
        // === [DELETE-SOCKET-1] ตรวจสอบว่ามี Socket ID นี้ในระบบหรือไม่ ===
        if (!socketRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Socket not found with id: " + id);
        }
        // === [DELETE-SOCKET-2] ตรวจสอบว่า Socket ถูกใช้งานโดย Component หรือไม่ (ป้องกันการลบ) ===
        if (componentRepository.existsBySocketId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Socket. It is currently in use by one or more components.");
        }
        // === [DELETE-SOCKET-3] หากไม่ถูกใช้งาน ให้ดำเนินการลบ ===
        socketRepository.deleteById(id);
    }

    // --- RAM Type CRUD ---
    @Override
    public RamType createRamType(RamTypeRequest request) {
        if (ramTypeRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "RAM Type with name '" + request.getName() + "' already exists.");
        }
        RamType ramType = new RamType(null, request.getName());
        return ramTypeRepository.save(ramType);
    }

    @Override
    public RamType updateRamType(String id, RamTypeRequest request) {
        RamType ramType = ramTypeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "RAM Type not found with id: " + id));

        if (!ramType.getName().equals(request.getName()) && componentRepository.existsByRamTypeId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the name of this RAM Type because it is currently in use. Please create a new RAM Type instead.");
        }

        Optional<RamType> existingByName = ramTypeRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another RAM Type with name '" + request.getName() + "' already exists.");
        }
        ramType.setName(request.getName());
        return ramTypeRepository.save(ramType);
    }

    @Override
    public void deleteRamType(String id) {
        if (!ramTypeRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "RAM Type not found with id: " + id);
        }
        if (componentRepository.existsByRamTypeId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete RAM Type. It is currently in use by one or more components.");
        }
        ramTypeRepository.deleteById(id);
    }

    // --- Form Factor CRUD ---
    @Override
    public FormFactor createFormFactor(FormFactorRequest request) {
        if (formFactorRepository.findByNameAndType(request.getName(), request.getType()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Form Factor with name '" + request.getName() + "' and type '" + request.getType() + "' already exists.");
        }
        FormFactor formFactor = new FormFactor(null, request.getName(), request.getType());
        return formFactorRepository.save(formFactor);
    }

    @Override
    public FormFactor updateFormFactor(String id, FormFactorRequest request) {
        FormFactor formFactor = formFactorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Form Factor not found with id: " + id));

        if (!formFactor.getName().equals(request.getName()) && componentRepository.existsByFormFactorId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the name of this Form Factor because it is currently in use. Please create a new Form Factor instead.");
        }

        Optional<FormFactor> existing = formFactorRepository.findByNameAndType(request.getName(), request.getType());
        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another Form Factor with name '" + request.getName() + "' and type '" + request.getType() + "' already exists.");
        }
        formFactor.setName(request.getName());
        formFactor.setType(request.getType());
        return formFactorRepository.save(formFactor);
    }

    @Override
    public void deleteFormFactor(String id) {
        if (!formFactorRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Form Factor not found with id: " + id);
        }
        if (componentRepository.existsByFormFactorId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Form Factor. It is currently in use by one or more components.");
        }
        formFactorRepository.deleteById(id);
    }

    // --- Storage Interface CRUD ---
    @Override
    public StorageInterface createStorageInterface(StorageInterfaceRequest request) {
        if (storageInterfaceRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Storage Interface with name '" + request.getName() + "' already exists.");
        }
        StorageInterface storageInterface = new StorageInterface(null, request.getName());
        return storageInterfaceRepository.save(storageInterface);
    }

    @Override
    public StorageInterface updateStorageInterface(String id, StorageInterfaceRequest request) {
        StorageInterface storageInterface = storageInterfaceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Storage Interface not found with id: " + id));

        if (!storageInterface.getName().equals(request.getName()) && componentRepository.existsByStorageInterfaceId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the name of this Storage Interface because it is currently in use. Please create a new one instead.");
        }

        Optional<StorageInterface> existingByName = storageInterfaceRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another Storage Interface with name '" + request.getName() + "' already exists.");
        }
        storageInterface.setName(request.getName());
        return storageInterfaceRepository.save(storageInterface);
    }

    @Override
    public void deleteStorageInterface(String id) {
        if (!storageInterfaceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Storage Interface not found with id: " + id);
        }
        if (componentRepository.existsByStorageInterfaceId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Storage Interface. It is currently in use by one or more components.");
        }
        storageInterfaceRepository.deleteById(id);
    }

    // --- Shipping Provider CRUD ---
    @Override
    public ShippingProvider createShippingProvider(ShippingProviderRequest request, MultipartFile image) {
        // === [CREATE-SHIPPING-PROVIDER-1] ตรวจสอบชื่อซ้ำ ===
        if (shippingProviderRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipping Provider with name '" + request.getName() + "' already exists.");
        }
        // === [CREATE-SHIPPING-PROVIDER-2] จัดการรูปภาพ (ถ้ามี) ===
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = s3Service.uploadFile(image);
        } else if (StringUtils.hasText(request.getImageUrl())) {
            imageUrl = request.getImageUrl();
        }
        // === [CREATE-SHIPPING-PROVIDER-3] สร้างและบันทึก Entity ===
        ShippingProvider provider = new ShippingProvider(null, request.getName(), imageUrl, request.getTrackingUrl());
        return shippingProviderRepository.save(provider);
    }

    @Override
    public ShippingProvider updateShippingProvider(String id, ShippingProviderRequest request, MultipartFile image) {
        // === [UPDATE-SHIPPING-PROVIDER-1] ค้นหาและตรวจสอบชื่อซ้ำ ===
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping Provider not found with id: " + id));
        Optional<ShippingProvider> existingByName = shippingProviderRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another Shipping Provider with name '" + request.getName() + "' already exists.");
        }
        // === [UPDATE-SHIPPING-PROVIDER-2] จัดการรูปภาพ: หากมีไฟล์ใหม่ ให้ลบไฟล์เก่า (ถ้ามี) และอัปโหลดใหม่ ===
        String imageUrl = provider.getImageUrl();
        if (image != null && !image.isEmpty()) {
            if (StringUtils.hasText(provider.getImageUrl()) && provider.getImageUrl().contains("s3.amazonaws.com")) {
                String oldKey = extractKeyFromUrl(provider.getImageUrl());
                if(oldKey != null) s3Service.deleteFile(oldKey);
            }
            imageUrl = s3Service.uploadFile(image);
        } else if (request.getImageUrl() != null) {
            imageUrl = request.getImageUrl();
        }
        // === [UPDATE-SHIPPING-PROVIDER-3] อัปเดตข้อมูลและบันทึก ===
        provider.setName(request.getName());
        provider.setImageUrl(imageUrl);
        provider.setTrackingUrl(request.getTrackingUrl());
        return shippingProviderRepository.save(provider);
    }

    @Override
    public void deleteShippingProvider(String id) {
        // === [DELETE-SHIPPING-PROVIDER-1] ค้นหา Entity ที่จะลบ ===
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping Provider not found with id: " + id));
        // === [DELETE-SHIPPING-PROVIDER-2] ลบรูปภาพที่เกี่ยวข้องออกจาก S3 (ถ้ามี) ===
        if (StringUtils.hasText(provider.getImageUrl()) && provider.getImageUrl().contains("s3.amazonaws.com")) {
            String keyToDelete = extractKeyFromUrl(provider.getImageUrl());
            if(keyToDelete != null) s3Service.deleteFile(keyToDelete);
        }
        // === [DELETE-SHIPPING-PROVIDER-3] ลบข้อมูลออกจากฐานข้อมูล ===
        shippingProviderRepository.deleteById(id);
    }

    // --- Brand CRUD ---
    @Override
    public Brand createBrand(BrandRequest request, MultipartFile image) {
        if (brandRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand with name '" + request.getName() + "' already exists.");
        }
        String logoUrl = null;
        if (image != null && !image.isEmpty()) {
            logoUrl = s3Service.uploadFile(image);
        }
        Brand brand = Brand.builder()
                .name(request.getName())
                .logoUrl(logoUrl)
                .build();
        return brandRepository.save(brand);
    }

    @Override
    public Brand updateBrand(String id, BrandRequest request, MultipartFile image) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found with id: " + id));
        if (!brand.getName().equals(request.getName()) && componentRepository.existsByBrandId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the name of this Brand because it is currently in use. Please create a new Brand instead.");
        }
        Optional<Brand> existingByName = brandRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another brand with name '" + request.getName() + "' already exists.");
        }
        String logoUrl = brand.getLogoUrl();
        if (image != null && !image.isEmpty()) {
            if (StringUtils.hasText(brand.getLogoUrl()) && brand.getLogoUrl().contains("s3.amazonaws.com")) {
                String oldKey = extractKeyFromUrl(brand.getLogoUrl());
                if(oldKey != null) s3Service.deleteFile(oldKey);
            }
            logoUrl = s3Service.uploadFile(image);
        }
        brand.setName(request.getName());
        brand.setLogoUrl(logoUrl);
        return brandRepository.save(brand);
    }

    @Override
    public void deleteBrand(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found with id: " + id));
        if (componentRepository.existsByBrandId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Brand. It is currently in use by one or more components.");
        }
        if (StringUtils.hasText(brand.getLogoUrl()) && brand.getLogoUrl().contains("s3.amazonaws.com")) {
            String keyToDelete = extractKeyFromUrl(brand.getLogoUrl());
            if(keyToDelete != null) s3Service.deleteFile(keyToDelete);
        }
        brandRepository.deleteById(id);
    }

    /**
     * เมธอดภายในสำหรับดึงชื่อไฟล์ (Key) จาก URL ของ S3
     * @param fileUrl URL เต็มของไฟล์
     * @return ชื่อไฟล์ (Key) หรือ null หากเกิดข้อผิดพลาด
     */
    private String extractKeyFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        try {
            return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            log.error("Could not extract key from S3 URL: {}", fileUrl, e);
            return null;
        }
    }
}