package in.project.computers.repository.generalReposiroty;

import in.project.computers.entity.order.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);

    // --- NEW METHOD FOR CLEANUP SERVICE ---
    /**
     * Finds all carts that contain at least one item with an active reservation
     * that has expired as of the given timestamp.
     *
     * @param now The current time to check against.
     * @return A list of carts with expired reservations.
     */
    @Query("{ 'items.stock_reserved': true, 'items.reservation_expires_at': { $lt: ?0 } }")
    List<Cart> findCartsWithExpiredReservations(Instant now);
}