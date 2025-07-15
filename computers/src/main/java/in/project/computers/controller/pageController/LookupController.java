package in.project.computers.controller.pageController;

import in.project.computers.dto.lookup.*;
import in.project.computers.entity.lookup.*;
import in.project.computers.service.componentService.LookupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/lookups")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LookupController {

    private final LookupService lookupService;

    /**
     * Endpoint to fetch all lookup data in a structured map.
     * Useful for populating entire forms on the frontend at once.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllLookupsForFormComponent() {
        return ResponseEntity.ok(lookupService.getAllLookups());
    }

    // --- Sockets Management ---
    // =================================================================

    @GetMapping("/sockets")
    public ResponseEntity<List<Socket>> getAllSockets() {
        return ResponseEntity.ok(lookupService.getAllSockets());
    }

    @PostMapping("/sockets")
    public ResponseEntity<Socket> createSocket(@Valid @RequestBody SocketRequest request) {
        Socket createdSocket = lookupService.createSocket(request);
        return new ResponseEntity<>(createdSocket, HttpStatus.CREATED);
    }

    @PutMapping("/sockets/{id}")
    public ResponseEntity<Socket> updateSocket(@PathVariable String id, @Valid @RequestBody SocketRequest request) {
        Socket updatedSocket = lookupService.updateSocket(id, request);
        return ResponseEntity.ok(updatedSocket);
    }

    @DeleteMapping("/sockets/{id}")
    public ResponseEntity<Void> deleteSocket(@PathVariable String id) {
        lookupService.deleteSocket(id);
        return ResponseEntity.noContent().build();
    }

    // --- RAM Types Management ---
    // =================================================================

    @GetMapping("/ram-types")
    public ResponseEntity<List<RamType>> getAllRamTypes() {
        return ResponseEntity.ok(lookupService.getAllRamTypes());
    }

    @PostMapping("/ram-types")
    public ResponseEntity<RamType> createRamType(@Valid @RequestBody RamTypeRequest request) {
        RamType createdRamType = lookupService.createRamType(request);
        return new ResponseEntity<>(createdRamType, HttpStatus.CREATED);
    }

    @PutMapping("/ram-types/{id}")
    public ResponseEntity<RamType> updateRamType(@PathVariable String id, @Valid @RequestBody RamTypeRequest request) {
        RamType updatedRamType = lookupService.updateRamType(id, request);
        return ResponseEntity.ok(updatedRamType);
    }

    @DeleteMapping("/ram-types/{id}")
    public ResponseEntity<Void> deleteRamType(@PathVariable String id) {
        lookupService.deleteRamType(id);
        return ResponseEntity.noContent().build();
    }

    // --- Form Factors Management ---
    // =================================================================

    @GetMapping("/form-factors")
    public ResponseEntity<List<FormFactor>> getAllFormFactors() {
        return ResponseEntity.ok(lookupService.getAllFormFactors());
    }

    @PostMapping("/form-factors")
    public ResponseEntity<FormFactor> createFormFactor(@Valid @RequestBody FormFactorRequest request) {
        FormFactor createdFormFactor = lookupService.createFormFactor(request);
        return new ResponseEntity<>(createdFormFactor, HttpStatus.CREATED);
    }

    @PutMapping("/form-factors/{id}")
    public ResponseEntity<FormFactor> updateFormFactor(@PathVariable String id, @Valid @RequestBody FormFactorRequest request) {
        FormFactor updatedFormFactor = lookupService.updateFormFactor(id, request);
        return ResponseEntity.ok(updatedFormFactor);
    }

    @DeleteMapping("/form-factors/{id}")
    public ResponseEntity<Void> deleteFormFactor(@PathVariable String id) {
        lookupService.deleteFormFactor(id);
        return ResponseEntity.noContent().build();
    }

    // --- Storage Interfaces Management ---
    // =================================================================

    @GetMapping("/storage-interfaces")
    public ResponseEntity<List<StorageInterface>> getAllStorageInterfaces() {
        return ResponseEntity.ok(lookupService.getAllStorageInterfaces());
    }

    @PostMapping("/storage-interfaces")
    public ResponseEntity<StorageInterface> createStorageInterface(@Valid @RequestBody StorageInterfaceRequest request) {
        StorageInterface createdInterface = lookupService.createStorageInterface(request);
        return new ResponseEntity<>(createdInterface, HttpStatus.CREATED);
    }

    @PutMapping("/storage-interfaces/{id}")
    public ResponseEntity<StorageInterface> updateStorageInterface(@PathVariable String id, @Valid @RequestBody StorageInterfaceRequest request) {
        StorageInterface updatedInterface = lookupService.updateStorageInterface(id, request);
        return ResponseEntity.ok(updatedInterface);
    }

    @DeleteMapping("/storage-interfaces/{id}")
    public ResponseEntity<Void> deleteStorageInterface(@PathVariable String id) {
        lookupService.deleteStorageInterface(id);
        return ResponseEntity.noContent().build();
    }

    // --- Shipping Providers Management ---
    // =================================================================

    @GetMapping("/shipping-providers")
    public ResponseEntity<List<ShippingProvider>> getAllShippingProviders() {
        return ResponseEntity.ok(lookupService.getAllShippingProviders());
    }

    @PostMapping(value = "/shipping-providers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShippingProvider> createShippingProvider(
            @RequestPart("provider") @Valid ShippingProviderRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        ShippingProvider createdProvider = lookupService.createShippingProvider(request, image);
        return new ResponseEntity<>(createdProvider, HttpStatus.CREATED);
    }

    @PutMapping(value = "/shipping-providers/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ShippingProvider> updateShippingProvider(
            @PathVariable String id,
            @RequestPart("provider") @Valid ShippingProviderRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        ShippingProvider updatedProvider = lookupService.updateShippingProvider(id, request, image);
        return ResponseEntity.ok(updatedProvider);
    }

    @DeleteMapping("/shipping-providers/{id}")
    public ResponseEntity<Void> deleteShippingProvider(@PathVariable String id) {
        lookupService.deleteShippingProvider(id);
        return ResponseEntity.noContent().build();
    }
}