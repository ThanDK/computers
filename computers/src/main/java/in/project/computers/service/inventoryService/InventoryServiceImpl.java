package in.project.computers.service.inventoryService;

import in.project.computers.entity.component.Inventory;
import in.project.computers.repository.componentRepository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public Inventory setStock(String componentId, int quantity) {
        Inventory inventory = inventoryRepository.findByComponentId(componentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inventory for component ID " + componentId + " not found."));

        inventory.setQuantity(quantity);
        return inventoryRepository.save(inventory);
    }
}