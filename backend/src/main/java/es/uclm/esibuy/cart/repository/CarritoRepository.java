package es.uclm.esibuy.cart.repository;

import java.util.Optional;

import es.uclm.esibuy.cart.model.Carrito;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CarritoRepository extends MongoRepository<Carrito, String> {

    Optional<Carrito> findByClienteId(String clienteId);
}
