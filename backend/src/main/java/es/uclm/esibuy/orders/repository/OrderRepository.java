
package es.uclm.esibuy.orders.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import es.uclm.esibuy.orders.model.Order;
import es.uclm.esibuy.orders.model.OrderStatus;

public interface OrderRepository extends MongoRepository<Order, String> {

    List<Order> findAllByCustomerIdOrderByCreatedAtDesc(String customerId);

    Optional<Order> findByIdAndCustomerId(String id, String customerId);

    List<Order> findAllByItemsProductIdInAndStatusIn(
            Collection<String> productIds,
            Collection<OrderStatus> statuses);
}
