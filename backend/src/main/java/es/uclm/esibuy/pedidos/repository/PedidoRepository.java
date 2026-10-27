package es.uclm.esibuy.pedidos.repository;

import es.uclm.esibuy.pedidos.model.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends MongoRepository<Pedido, String> {

    List<Pedido> findAllByClienteIdOrderByFechaCreacionDesc(String clienteId);

    Optional<Pedido> findByIdAndClienteId(String id, String clienteId);
}
