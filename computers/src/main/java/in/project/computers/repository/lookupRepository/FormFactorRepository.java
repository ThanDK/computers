package in.project.computers.repository.lookupRepository;

import in.project.computers.entity.lookup.FormFactor;
import in.project.computers.entity.lookup.FormFactorType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormFactorRepository extends MongoRepository<FormFactor, String> {

    Optional<FormFactor> findByName(String name);

    Optional<FormFactor> findByNameAndType(String name, FormFactorType type);

    List<FormFactor> findByType(FormFactorType type);
}