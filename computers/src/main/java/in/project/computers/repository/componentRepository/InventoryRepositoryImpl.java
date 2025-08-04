package in.project.computers.repository.componentRepository;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOneModel;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.model.WriteModel;
import lombok.RequiredArgsConstructor;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryImpl implements InventoryRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public void bulkAtomicUpdateQuantities(Map<String, Integer> quantityChanges) {
        if (quantityChanges == null || quantityChanges.isEmpty()) {
            return;
        }

        List<WriteModel<org.bson.Document>> bulkOps = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : quantityChanges.entrySet()) {
            String componentId = entry.getKey();
            Integer quantityChange = entry.getValue();

            Bson filter = Filters.eq("componentId", componentId);

            Bson update = Updates.inc("quantity", quantityChange);

            UpdateOneModel<org.bson.Document> updateOneModel = new UpdateOneModel<>(filter, update, new UpdateOptions().upsert(false));
            bulkOps.add(updateOneModel);
        }

        mongoTemplate.getCollection("inventory").bulkWrite(bulkOps);
    }
}