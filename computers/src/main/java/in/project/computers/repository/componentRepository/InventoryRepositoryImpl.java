package in.project.computers.repository.componentRepository;

import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.model.*;
import in.project.computers.DTO.inventory.StockConflictInfo;
import in.project.computers.entity.component.Component;
import in.project.computers.entity.component.Inventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class InventoryRepositoryImpl implements InventoryRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    private final ComponentRepository componentRepository;

    @Override
    public void bulkAtomicUpdateQuantities(Map<String, Integer> quantityChanges) {
        if (quantityChanges == null || quantityChanges.isEmpty()) {
            return;
        }

        List<String> componentIds = new ArrayList<>(quantityChanges.keySet());


        Query stockQuery = new Query(Criteria.where("componentId").in(componentIds));
        Map<String, Integer> stockBeforeUpdate = mongoTemplate.find(stockQuery, Inventory.class).stream()
                .collect(Collectors.toMap(Inventory::getComponentId, Inventory::getQuantity));

        List<WriteModel<Document>> bulkOps = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : quantityChanges.entrySet()) {
            String componentId = entry.getKey();
            Integer quantityChange = entry.getValue();

            Bson filter = Filters.eq("componentId", componentId);
            Bson update = Updates.inc("quantity", quantityChange);

            UpdateOneModel<Document> updateOneModel = new UpdateOneModel<>(filter, update, new UpdateOptions().upsert(false));
            bulkOps.add(updateOneModel);
        }

        mongoTemplate.getCollection("inventory").bulkWrite(bulkOps);

        List<String> componentsToReactivate = new ArrayList<>();
        for (String componentId : componentIds) {
            int stockBefore = stockBeforeUpdate.getOrDefault(componentId, 0);
            if (stockBefore == 0 && quantityChanges.get(componentId) > 0) {
                componentsToReactivate.add(componentId);
            }
        }

        if (!componentsToReactivate.isEmpty()) {
            Query query = new Query(Criteria.where("id").in(componentsToReactivate));
            Update update = new Update().set("isActive", true);
            mongoTemplate.updateMulti(query, update, Component.class);
            log.info("Reactivated {} components that are now back in stock.", componentsToReactivate.size());
        }
    }

    @Override
    public List<StockConflictInfo> attemptReservation(Map<String, Integer> requiredStock) {
        if (requiredStock == null || requiredStock.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> componentIds = new ArrayList<>(requiredStock.keySet());
        Map<String, Inventory> currentInventoryMap = mongoTemplate.find(
                new Query(Criteria.where("componentId").in(componentIds)),
                Inventory.class
        ).stream().collect(Collectors.toMap(Inventory::getComponentId, Function.identity()));

        Map<String, String> componentNameMap = componentRepository.findAllById(componentIds).stream()
                .collect(Collectors.toMap(Component::getId, Component::getName));

        List<StockConflictInfo> conflicts = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
            String componentId = entry.getKey();
            int required = entry.getValue();
            int available = currentInventoryMap.getOrDefault(componentId, new Inventory()).getQuantity();
            if (available < required) {
                conflicts.add(new StockConflictInfo(componentId, componentNameMap.getOrDefault(componentId, "N/A"), required, available));
            }
        }
        if (!conflicts.isEmpty()) {
            return conflicts;
        }

        List<WriteModel<Document>> bulkOps = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
            String componentId = entry.getKey();
            int required = entry.getValue();

            Bson filter = Filters.and(
                    Filters.eq("componentId", componentId),
                    Filters.gte("quantity", required)
            );
            Bson update = Updates.inc("quantity", -required);
            bulkOps.add(new UpdateOneModel<>(filter, update));
        }

        BulkWriteResult result = mongoTemplate.getCollection("inventory").bulkWrite(bulkOps);

        if (result.getModifiedCount() == requiredStock.size()) {
            List<String> componentsToDeactivate = new ArrayList<>();
            for(Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
                String componentId = entry.getKey();
                int required = entry.getValue();
                int initialStock = currentInventoryMap.getOrDefault(componentId, new Inventory()).getQuantity();

                if (initialStock - required == 0) {
                    componentsToDeactivate.add(componentId);
                }
            }

            if (!componentsToDeactivate.isEmpty()) {
                Query query = new Query(Criteria.where("id").in(componentsToDeactivate));
                Update update = new Update().set("isActive", false);
                mongoTemplate.updateMulti(query, update, Component.class);
                log.info("Deactivated {} components that are now out of stock.", componentsToDeactivate.size());
            }
            return new ArrayList<>();
        }

        List<WriteModel<Document>> rollbackOps = new ArrayList<>();
        Map<String, Inventory> postAttemptInventoryMap = mongoTemplate.find(
                new Query(Criteria.where("componentId").in(componentIds)),
                Inventory.class
        ).stream().collect(Collectors.toMap(Inventory::getComponentId, Function.identity()));

        for (Map.Entry<String, Integer> entry : requiredStock.entrySet()) {
            String componentId = entry.getKey();
            int required = entry.getValue();
            int initialStock = currentInventoryMap.getOrDefault(componentId, new Inventory()).getQuantity();
            int currentStock = postAttemptInventoryMap.getOrDefault(componentId, new Inventory()).getQuantity();

            if (currentStock == initialStock - required) {
                rollbackOps.add(new UpdateOneModel<>(
                        Filters.eq("componentId", componentId),
                        Updates.inc("quantity", required)
                ));
            } else {
                conflicts.add(new StockConflictInfo(componentId, componentNameMap.getOrDefault(componentId, "N/A"), required, initialStock));
            }
        }

        if (!rollbackOps.isEmpty()) {
            mongoTemplate.getCollection("inventory").bulkWrite(rollbackOps);
        }

        return conflicts;
    }
}