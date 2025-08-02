package in.project.computers.repository.lookupRepository;

import in.project.computers.entity.lookup.Socket;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocketRepository extends MongoRepository<Socket, String> {
    Optional<Socket> findByName(String name);
}