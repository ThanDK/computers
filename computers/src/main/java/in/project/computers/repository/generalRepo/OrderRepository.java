package in.project.computers.repository.generalRepo;

import com.mongodb.lang.NonNull;
import in.project.computers.entity.order.Order;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface OrderRepository extends CrudRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    @Override
    @NonNull
    List<Order> findAll();
}
