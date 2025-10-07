package in.project.computers.service.inventoryService;

import in.project.computers.entity.component.Inventory;

public interface InventoryService {
    Inventory setStock(String componentId, int quantity);
}