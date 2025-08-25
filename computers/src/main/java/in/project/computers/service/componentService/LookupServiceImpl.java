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

@Service
@RequiredArgsConstructor
@Slf4j
public class LookupServiceImpl implements LookupService {

    // ==================[ Repositories & Services ]==================
    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final ComponentRepository componentRepository;
    private final ShippingProviderRepository shippingProviderRepository;
    private final BrandRepository brandRepository;
    private final S3Service s3Service;

    // ==================[ Lookup Retrieval ]==================
    @Override
    public Map<String, Object> getAllLookups() {
        Map<String, Object> lookups = new HashMap<>();
        lookups.put("sockets", socketRepository.findAll());
        lookups.put("ramTypes", ramTypeRepository.findAll());
        lookups.put("storageInterfaces", storageInterfaceRepository.findAll());
        lookups.put("shippingProviders", shippingProviderRepository.findAll());
        lookups.put("brands", brandRepository.findAll());
        Map<FormFactorType, List<FormFactor>> groupedFormFactors = formFactorRepository.findAll().stream()
                .collect(Collectors.groupingBy(FormFactor::getType));
        lookups.put("formFactors", groupedFormFactors);
        lookups.put("radiatorSizes", List.of(120, 140, 240, 280, 360, 420));
        return lookups;
    }

    @Override public List<Socket> getAllSockets() { return socketRepository.findAll(); }
    @Override public List<RamType> getAllRamTypes() { return ramTypeRepository.findAll(); }
    @Override public List<FormFactor> getAllFormFactors() { return formFactorRepository.findAll(); }
    @Override public List<StorageInterface> getAllStorageInterfaces() { return storageInterfaceRepository.findAll(); }
    @Override public List<ShippingProvider> getAllShippingProviders() { return shippingProviderRepository.findAll(); }
    @Override public List<Brand> getAllBrands() { return brandRepository.findAll(); }

    // ==================[ Socket Management ]==================
    @Override
    public Socket createSocket(SocketRequest request) {
        if (socketRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Socket with name '" + request.getName() + "' already exists.");
        }
        Socket socket = new Socket(null, request.getName(), request.getBrand());
        return socketRepository.save(socket);
    }

    @Override
    public Socket updateSocket(String id, SocketRequest request) {
        Socket socket = socketRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Socket not found with id: " + id));
        if (!socket.getName().equals(request.getName()) && componentRepository.existsBySocketId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the name of this Socket because it is currently in use. Please create a new Socket instead.");
        }
        Optional<Socket> existingByName = socketRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another socket with name '" + request.getName() + "' already exists.");
        }
        socket.setName(request.getName());
        socket.setBrand(request.getBrand());
        return socketRepository.save(socket);
    }

    @Override
    public void deleteSocket(String id) {
        if (!socketRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Socket not found with id: " + id);
        }
        if (componentRepository.existsBySocketId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Socket. It is currently in use by one or more components.");
        }
        socketRepository.deleteById(id);
    }

    // ==================[ RAM Type Management ]==================
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

    // ==================[ Form Factor Management ]==================
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

    // ==================[ Storage Interface Management ]==================
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

    // ==================[ Shipping Provider Management ]==================
    @Override
    public ShippingProvider createShippingProvider(ShippingProviderRequest request, MultipartFile image) {
        if (shippingProviderRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipping Provider with name '" + request.getName() + "' already exists.");
        }
        String imageUrl = (image != null && !image.isEmpty()) ? s3Service.uploadFile(image) : null;
        ShippingProvider provider = new ShippingProvider(null, request.getName(), imageUrl, request.getTrackingUrl());
        return shippingProviderRepository.save(provider);
    }

    @Override
    public ShippingProvider updateShippingProvider(String id, ShippingProviderRequest request, MultipartFile image) {
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping Provider not found with id: " + id));
        Optional<ShippingProvider> existingByName = shippingProviderRepository.findByName(request.getName());
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Another Shipping Provider with name '" + request.getName() + "' already exists.");
        }
        provider.setName(request.getName());
        provider.setTrackingUrl(request.getTrackingUrl());
        if (image != null && !image.isEmpty()) {
            if (StringUtils.hasText(provider.getImageUrl())) {
                s3Service.deleteFileByKey(s3Service.extractKeyFromUrl(provider.getImageUrl()));
            }
            provider.setImageUrl(s3Service.uploadFile(image));
        }
        return shippingProviderRepository.save(provider);
    }

    @Override
    public void deleteShippingProvider(String id) {
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping Provider not found with id: " + id));
        if (StringUtils.hasText(provider.getImageUrl())) {
            s3Service.deleteFileByKey(s3Service.extractKeyFromUrl(provider.getImageUrl()));
        }
        shippingProviderRepository.deleteById(id);
    }

    // ==================[ Brand Management ]==================
    @Override
    public Brand createBrand(BrandRequest request, MultipartFile image) {
        if (brandRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand with name '" + request.getName() + "' already exists.");
        }
        String logoUrl = (image != null && !image.isEmpty()) ? s3Service.uploadFile(image) : null;
        Brand brand = Brand.builder().name(request.getName()).logoUrl(logoUrl).build();
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
        brand.setName(request.getName());
        if (image != null && !image.isEmpty()) {
            if (StringUtils.hasText(brand.getLogoUrl())) {
                s3Service.deleteFileByKey(s3Service.extractKeyFromUrl(brand.getLogoUrl()));
            }
            brand.setLogoUrl(s3Service.uploadFile(image));
        }
        return brandRepository.save(brand);
    }

    @Override
    public void deleteBrand(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found with id: " + id));
        if (componentRepository.existsByBrandId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete Brand. It is currently in use by one or more components.");
        }
        if (StringUtils.hasText(brand.getLogoUrl())) {
            s3Service.deleteFileByKey(s3Service.extractKeyFromUrl(brand.getLogoUrl()));
        }
        brandRepository.deleteById(id);
    }
}