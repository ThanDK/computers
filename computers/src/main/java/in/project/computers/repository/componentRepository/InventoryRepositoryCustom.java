package in.project.computers.repository.componentRepository;

import java.util.Map;

public interface InventoryRepositoryCustom {
    void bulkAtomicUpdateQuantities(Map<String, Integer> quantityChanges);
}