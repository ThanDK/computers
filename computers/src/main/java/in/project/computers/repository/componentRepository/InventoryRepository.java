// in/project/computers/repository/InventoryRepository.java
package in.project.computers.repository.componentRepository;

import in.project.computers.entity.component.Inventory;
import org.springframework.data.mongodb.repository.MongoRepository; // CHANGED
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends MongoRepository<Inventory, String>, InventoryRepositoryCustom {
    Optional<Inventory> findByComponentId(String componentId);
    List<Inventory> findByComponentIdIn(List<String> componentIds);
}