package in.project.computers.controller.test;

import in.project.computers.DTO.inventory.SetStockRequest;
import in.project.computers.entity.component.Inventory;
import in.project.computers.service.inventoryService.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryAdminController {

    private final InventoryService inventoryService;

    @PostMapping("/set-stock")
    public ResponseEntity<Inventory> setStockForComponent(@Valid @RequestBody SetStockRequest request) {
        Inventory updatedInventory = inventoryService.setStock(request.getComponentId(), request.getQuantity());
        return ResponseEntity.ok(updatedInventory);
    }
}