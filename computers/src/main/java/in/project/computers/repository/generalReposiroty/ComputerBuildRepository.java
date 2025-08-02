package in.project.computers.repository.generalReposiroty;

import in.project.computers.entity.computerBuild.ComputerBuild;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComputerBuildRepository extends MongoRepository<ComputerBuild, String> {
    List<ComputerBuild> findByUserId(String userId);
}