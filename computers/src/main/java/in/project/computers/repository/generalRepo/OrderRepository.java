package in.project.computers.repository.generalRepo;

import com.mongodb.lang.NonNull;
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.OrderStatus;
import org.springframework.data.repository.CrudRepository;

import java.time.Instant;
import java.util.List;

public interface OrderRepository extends CrudRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    @Override
    @NonNull
    List<Order> findAll();

    // Finds all orders created within the specified time frame.
    List<Order> findByCreatedAtBetween(Instant startDate, Instant endDate);

    // Finds the top 5 most recent orders for the dashboard widget.
    List<Order> findTop5ByOrderByCreatedAtDesc();

    // Efficiently counts orders with specific statuses for the stat card.
    long countByOrderStatusIn(List<OrderStatus> statuses);
}
