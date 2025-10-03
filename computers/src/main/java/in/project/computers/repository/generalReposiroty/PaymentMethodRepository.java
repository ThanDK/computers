package in.project.computers.repository.generalReposiroty;

import in.project.computers.entity.payment.PaymentMethod;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends MongoRepository<PaymentMethod, String> {

    /**
     * Finds the currently active default payment method.
     * @param isDefault Should always be true for this query.
     * @return An Optional containing the default PaymentMethod if one exists.
     */
    Optional<PaymentMethod> findByIsDefault(boolean isDefault);
}