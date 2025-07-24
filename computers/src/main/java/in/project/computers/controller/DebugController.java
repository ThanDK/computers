package in.project.computers.controller;

import in.project.computers.repository.ComponentRepo.ComponentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/debug") // A new, public base path
@RequiredArgsConstructor
@Slf4j
public class DebugController {

    private final ComponentRepository componentRepository;

    @GetMapping("/is-socket-used/{id}")
    public ResponseEntity<String> debugCheckSocket(@PathVariable String id) {
        log.info("--- DEBUG: Checking Socket ID '{}'", id);
        boolean result = componentRepository.existsBySocketId(id);
        log.info("--- DEBUG: existsBySocketId returned: {}", result);
        return ResponseEntity.ok(String.format("DEBUG RESULT for Socket ID '%s' is: %b", id, result));
    }

    @GetMapping("/is-ram-type-used/{id}")
    public ResponseEntity<String> debugCheckRamType(@PathVariable String id) {
        log.info("--- DEBUG: Checking RamType ID '{}'", id);
        boolean result = componentRepository.existsByRamTypeId(id);
        log.info("--- DEBUG: existsByRamTypeId returned: {}", result);
        return ResponseEntity.ok(String.format("DEBUG RESULT for RamType ID '%s' is: %b", id, result));
    }

    @GetMapping("/is-form-factor-used/{id}")
    public ResponseEntity<String> debugCheckFormFactor(@PathVariable String id) {
        log.info("--- DEBUG: Checking FormFactor ID '{}'", id);
        boolean result = componentRepository.existsByFormFactorId(id);
        log.info("--- DEBUG: existsByFormFactorId returned: {}", result);
        return ResponseEntity.ok(String.format("DEBUG RESULT for FormFactor ID '%s' is: %b", id, result));
    }

    @GetMapping("/is-storage-interface-used/{id}")
    public ResponseEntity<String> debugCheckStorageInterface(@PathVariable String id) {
        log.info("--- DEBUG: Checking StorageInterface ID '{}'", id);
        boolean result = componentRepository.existsByStorageInterfaceId(id);
        log.info("--- DEBUG: existsByStorageInterfaceId returned: {}", result);
        return ResponseEntity.ok(String.format("DEBUG RESULT for StorageInterface ID '%s' is: %b", id, result));
    }

    @GetMapping("/is-brand-used/{id}")
    public ResponseEntity<String> debugCheckBrand(@PathVariable String id) {
        log.info("--- DEBUG: Checking Brand ID '{}'", id);
        boolean result = componentRepository.existsByBrandId(id);
        log.info("--- DEBUG: existsByBrandId returned: {}", result);
        return ResponseEntity.ok(String.format("DEBUG RESULT for Brand ID '%s' is: %b", id, result));
    }
}