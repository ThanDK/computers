package in.project.computers.repository.lookupRepository;

import in.project.computers.entity.lookup.RamType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RamTypeRepository extends MongoRepository<RamType, String> {
    Optional<RamType> findByName(String name);
}