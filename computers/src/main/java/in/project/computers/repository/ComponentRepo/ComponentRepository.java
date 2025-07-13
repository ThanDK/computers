package in.project.computers.repository.ComponentRepo;

import in.project.computers.entity.component.Component;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComponentRepository extends MongoRepository<Component, String> {
    Optional<Component> findByMpn(String mpn);


    @Query(value = "{ $or: [ { 'socket._id': ?0 }, { 'supportedSockets._id': ?0 } ] }", exists = true)
    boolean existsBySocketId(String socketId);


    @Query(value = "{ 'ramType._id': ?0 }", exists = true)
    boolean existsByRamTypeId(String ramTypeId);

    @Query(value = "{ 'storageInterface._id': ?0 }", exists = true)
    boolean existsByStorageInterfaceId(String storageInterfaceId);

    @Query(value = "{ $or: [ " +
            "{ 'formFactor._id': ?0 }, " +
            "{ 'supportedFormFactors._id': ?0 }, " +
            "{ 'supportedPsuFormFactors._id': ?0 } " +
            "] }", exists = true)
    boolean existsByFormFactorId(String formFactorId);
}