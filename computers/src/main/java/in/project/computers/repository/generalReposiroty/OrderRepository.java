package in.project.computers.repository.generalReposiroty;

import com.mongodb.lang.NonNull;
import in.project.computers.entity.order.Order;
import in.project.computers.entity.order.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;


import java.time.Instant;
import java.util.List;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
    @Override
    @NonNull
    List<Order> findAll();

    List<Order> findByCreatedAtBetween(Instant startDate, Instant endDate);

    List<Order> findTop5ByOrderByCreatedAtDesc();


    long countByOrderStatusIn(List<OrderStatus> statuses);

}
