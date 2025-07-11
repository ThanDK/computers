package in.project.computers.repository.ComponentRepo;

import in.project.computers.entity.component.Component;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComponentRepository extends MongoRepository<Component, String> {

    Optional<Component> findByMpn(String mpn);

}
