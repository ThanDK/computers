package in.project.computers.repository.ComponentRepo;

import in.project.computers.entity.component.Component;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComponentRepository extends MongoRepository<Component, String> {

    Optional<Component> findByMpn(String mpn);

    /**
     * CORRECT: Checks EMBEDDED Sockets using dot notation on the nested '_id' field.
     */
    @Query(value = "{ $or: [ { 'socket._id': ObjectId(?0) }, { 'supportedSockets._id': ObjectId(?0) } ] }", exists = true)
    boolean existsBySocketId(String socketId);

    /**
     * CORRECT: Checks EMBEDDED RamTypes using dot notation on the nested '_id' field.
     */
    @Query(value = "{ 'ramType._id': ObjectId(?0) }", exists = true)
    boolean existsByRamTypeId(String ramTypeId);

    /**
     * FINAL FIX: Checks REFERENCED StorageInterfaces using dot notation on the DBRef's '$id' field.
     */
    @Query(value = "{ 'storageInterface.$id': ObjectId(?0) }", exists = true)
    boolean existsByStorageInterfaceId(String storageInterfaceId);

    /**
     * FINAL FIX: Checks REFERENCED FormFactors using dot notation on the DBRef's '$id' field.
     * This works for single references and for arrays of references.
     */
    @Query(value = "{ $or: [ " +
            "{ 'formFactor.$id': ObjectId(?0) }, " +
            "{ 'supportedFormFactors.$id': ObjectId(?0) }, " +
            "{ 'supportedPsuFormFactors.$id': ObjectId(?0) } " +
            "] }", exists = true)
    boolean existsByFormFactorId(String formFactorId);

    /**
     * CORRECT: This derived query works for @DBRef because Spring handles the '$id' translation automatically.
     */
    boolean existsByBrandId(String brandId);
}