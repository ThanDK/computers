package in.project.computers.repository.ComponentRepo.lookup;

import in.project.computers.entity.lookup.StorageInterface;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StorageInterfaceRepository extends MongoRepository<StorageInterface, String> {
    Optional<StorageInterface> findByName(String name);
}