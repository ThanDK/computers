package in.project.computers.service.componentService;

import in.project.computers.dto.lookup.*;
import in.project.computers.entity.lookup.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface LookupService {

    Map<String, Object> getAllLookups();

    List<Socket> getAllSockets();
    List<RamType> getAllRamTypes();
    List<FormFactor> getAllFormFactors();
    List<StorageInterface> getAllStorageInterfaces();
    List<ShippingProvider> getAllShippingProviders();
    List<Brand> getAllBrands();

    Socket createSocket(SocketRequest request);
    Socket updateSocket(String id, SocketRequest request);
    void deleteSocket(String id);

    RamType createRamType(RamTypeRequest request);
    RamType updateRamType(String id, RamTypeRequest request);
    void deleteRamType(String id);

    FormFactor createFormFactor(FormFactorRequest request);
    FormFactor updateFormFactor(String id, FormFactorRequest request);
    void deleteFormFactor(String id);

    StorageInterface createStorageInterface(StorageInterfaceRequest request);
    StorageInterface updateStorageInterface(String id, StorageInterfaceRequest request);
    void deleteStorageInterface(String id);

    ShippingProvider createShippingProvider(ShippingProviderRequest request, MultipartFile image);
    ShippingProvider updateShippingProvider(String id, ShippingProviderRequest request, MultipartFile image);
    void deleteShippingProvider(String id);

    // FIX: Add the MultipartFile parameter to match the implementation
    Brand createBrand(BrandRequest request, MultipartFile image);
    Brand updateBrand(String id, BrandRequest request, MultipartFile image);
    void deleteBrand(String id);
}