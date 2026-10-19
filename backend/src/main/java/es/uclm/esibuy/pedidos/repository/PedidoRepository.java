package es.uclm.esibuy.pedidos.repository;

import es.uclm.esibuy.pedidos.model.Pedido;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PedidoRepository extends MongoRepository<Pedido, String> {
}
