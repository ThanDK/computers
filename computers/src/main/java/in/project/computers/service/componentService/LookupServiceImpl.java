package in.project.computers.service.componentService;

import in.project.computers.dto.lookup.*;
import in.project.computers.entity.lookup.*;
import in.project.computers.repository.ComponentRepo.ComponentRepository;
import in.project.computers.repository.lookup.*;
import in.project.computers.service.AWSS3Bucket.S3Service;
import lombok.RequiredArgsConstructor;
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
public class LookupServiceImpl implements LookupService {

    private final SocketRepository socketRepository;
    private final RamTypeRepository ramTypeRepository;
    private final FormFactorRepository formFactorRepository;
    private final StorageInterfaceRepository storageInterfaceRepository;
    private final ComponentRepository componentRepository;
    private final ShippingProviderRepository shippingProviderRepository;
    private final BrandRepository brandRepository;
    private final S3Service s3Service;

    // ... (getAllLookups, and all Socket, RamType, FormFactor, StorageInterface methods are unchanged) ...
    @Override
    public Map<String, Object> getAllLookups() {
        Map<String, Object> lookups = new HashMap<>();
        lookups.put("sockets", socketRepository.findAll());
        lookups.put("ramTypes", ramTypeRepository.findAll());
        lookups.put("storageInterfaces", storageInterfaceRepository.findAll());
        Map<FormFactorType, List<FormFactor>> groupedFormFactors = formFactorRepository.findAll().stream()
                .collect(Collectors.groupingBy(FormFactor::getType));
        lookups.put("formFactors", groupedFormFactors);
        lookups.put("shippingProviders", shippingProviderRepository.findAll());
        lookups.put("radiatorSizes", List.of(120, 140, 240, 280, 360, 420));
        lookups.put("brands", brandRepository.findAll());
        return lookups;
    }

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
    @Override
    public ShippingProvider createShippingProvider(ShippingProviderRequest request, MultipartFile image) {
        if (shippingProviderRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Shipping Provider with name '" + request.getName() + "' already exists.");
        }

        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            imageUrl = s3Service.uploadFile(image);
        } else if (StringUtils.hasText(request.getImageUrl())) {
            imageUrl = request.getImageUrl();
        }

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

        String imageUrl = provider.getImageUrl();

        if (image != null && !image.isEmpty()) {

            if (StringUtils.hasText(provider.getImageUrl()) && provider.getImageUrl().contains("s3.amazonaws.com")) {
                String oldKey = extractKeyFromUrl(provider.getImageUrl());
                s3Service.deleteFile(oldKey);
            }
            imageUrl = s3Service.uploadFile(image);
        } else if (request.getImageUrl() != null) {
            imageUrl = request.getImageUrl();
        }

        provider.setName(request.getName());
        provider.setImageUrl(imageUrl);
        provider.setTrackingUrl(request.getTrackingUrl());

        return shippingProviderRepository.save(provider);
    }

    @Override
    public void deleteShippingProvider(String id) {
        ShippingProvider provider = shippingProviderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shipping Provider not found with id: " + id));

        if (StringUtils.hasText(provider.getImageUrl()) && provider.getImageUrl().contains("s3.amazonaws.com")) {
            String keyToDelete = extractKeyFromUrl(provider.getImageUrl());
            s3Service.deleteFile(keyToDelete);
        }

        shippingProviderRepository.deleteById(id);
    }

    // --- BRAND METHODS ---

    @Override
    public Brand createBrand(BrandRequest request, MultipartFile image) {
        if (brandRepository.findByName(request.getName()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Brand with name '" + request.getName() + "' already exists.");
        }

        String logoUrl = null;
        if (image != null && !image.isEmpty()) {
            logoUrl = s3Service.uploadFile(image);
        }
        // FIX: Removed the incorrect call to request.getLogoUrl()

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
        // FIX: Removed the incorrect call to request.getLogoUrl()

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

    // --- FIX: ADDED THE MISSING HELPER METHOD ---
    private String extractKeyFromUrl(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return null;
        }
        try {
            // Assumes a URL structure like https://<bucket-name>.s3.<region>.amazonaws.com/<key>
            return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
        } catch (Exception e) {
            // Log the exception if you have a logger configured
            return null;
        }
    }
}