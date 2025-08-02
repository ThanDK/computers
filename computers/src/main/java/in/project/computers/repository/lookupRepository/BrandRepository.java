package in.project.computers.repository.lookupRepository;

import in.project.computers.entity.lookup.Brand;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrandRepository extends MongoRepository<Brand, String> {
    Optional<Brand> findByName(String name);
}