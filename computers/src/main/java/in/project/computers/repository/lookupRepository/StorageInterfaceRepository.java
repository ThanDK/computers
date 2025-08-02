package in.project.computers.repository.lookupRepository;

import in.project.computers.entity.lookup.StorageInterface;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageInterfaceRepository extends MongoRepository<StorageInterface, String> {
    Optional<StorageInterface> findByName(String name);
    List<StorageInterface> findAllByName(String name);
}