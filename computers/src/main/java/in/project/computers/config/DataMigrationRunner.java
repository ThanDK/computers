package in.project.computers.config;

import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataMigrationRunner implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        log.info("--- Starting Data Migration Runner ---");

        migrateCartItems();

        // As per analysis, no migration is needed for Order entities.
        // The PendingOrderCleanupService query for `hold_expires_at: { $lt: now }`
        // will safely ignore old documents where `hold_expires_at` is null.

        log.info("--- Data Migration Runner Finished ---");
    }

    private void migrateCartItems() {
        log.info("Checking for cart items needing reservation field migration...");

        // Find all carts that have at least one item where 'stock_reserved' field does not exist.
        Query query = new Query(Criteria.where("items.stock_reserved").exists(false));

        // Prepare an update to set 'stock_reserved' to false on all items within the matched documents.
        // The '$[]' operator acts on all elements in the 'items' array.
        Update update = new Update().set("items.$[].stock_reserved", false);

        try {
            UpdateResult result = mongoTemplate.updateMulti(query, update, "carts");
            if (result.getModifiedCount() > 0) {
                log.info("Successfully migrated {} cart documents by setting default 'stock_reserved' to false.", result.getModifiedCount());
            } else {
                log.info("No legacy cart documents found requiring migration.");
            }
        } catch (Exception e) {
            log.error("An error occurred during cart data migration.", e);
        }
    }
}