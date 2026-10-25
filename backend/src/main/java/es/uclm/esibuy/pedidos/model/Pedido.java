package es.uclm.esibuy.pedidos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("pedidos")
public class Pedido {

    @Id
    private String id;
    private String clienteId;
    private List<PedidoItem> items;
    private BigDecimal precioTotal;
    private DireccionEnvioPedido direccionEnvio;
    private MetodoPagoPedido metodoPago;
    private PedidoEstado estado;
    private LocalDateTime fechaCreacion;

    protected Pedido() {
    }

    public Pedido(
            String clienteId,
            List<PedidoItem> items,
            BigDecimal precioTotal,
            DireccionEnvioPedido direccionEnvio,
            MetodoPagoPedido metodoPago,
            PedidoEstado estado) {
        this.clienteId = Objects.requireNonNull(clienteId);
        this.items = List.copyOf(items);
        this.precioTotal = Objects.requireNonNull(precioTotal);
        this.direccionEnvio = Objects.requireNonNull(direccionEnvio);
        this.metodoPago = Objects.requireNonNull(metodoPago);
        this.estado = Objects.requireNonNull(estado);
        this.fechaCreacion = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getClienteId() {
        return clienteId;
    }

    public List<PedidoItem> getItems() {
        return List.copyOf(items);
    }

    public BigDecimal getPrecioTotal() {
        return precioTotal;
    }

    public DireccionEnvioPedido getDireccionEnvio() {
        return direccionEnvio;
    }

    public MetodoPagoPedido getMetodoPago() {
        return metodoPago;
    }

    public PedidoEstado getEstado() {
        return estado;
    }

    public void setEstado(PedidoEstado estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }
}
