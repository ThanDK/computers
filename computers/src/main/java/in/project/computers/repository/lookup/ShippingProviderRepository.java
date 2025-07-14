package in.project.computers.repository.lookup;

import in.project.computers.entity.lookup.ShippingProvider;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ShippingProviderRepository extends MongoRepository<ShippingProvider, String> {
    Optional<ShippingProvider> findByName(String name);
}