package es.uclm.esibuy.pedidos.model;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private PedidoEstadoPago estadoPago;
    private LocalDateTime fechaCreacion;
    private List<EstadoPedidoHistorial> historialEstados = new ArrayList<>();
    private LocalDate estimatedDeliveryDate;

    protected Pedido() {
    }

    public Pedido(
            String clienteId,
            List<PedidoItem> items,
            BigDecimal precioTotal,
            DireccionEnvioPedido direccionEnvio,
            MetodoPagoPedido metodoPago,
            PedidoEstado estado) {
        this(clienteId, items, precioTotal, direccionEnvio, metodoPago, estado, PedidoEstadoPago.PAGADO);
    }

    public Pedido(
            String clienteId,
            List<PedidoItem> items,
            BigDecimal precioTotal,
            DireccionEnvioPedido direccionEnvio,
            MetodoPagoPedido metodoPago,
            PedidoEstado estado,
            PedidoEstadoPago estadoPago) {
        this.clienteId = Objects.requireNonNull(clienteId);
        this.items = List.copyOf(items);
        this.precioTotal = Objects.requireNonNull(precioTotal);
        this.direccionEnvio = Objects.requireNonNull(direccionEnvio);
        this.metodoPago = Objects.requireNonNull(metodoPago);
        this.estado = Objects.requireNonNull(estado);
        this.estadoPago = Objects.requireNonNull(estadoPago);
        this.fechaCreacion = LocalDateTime.now();
        this.historialEstados.add(new EstadoPedidoHistorial(estado, fechaCreacion));
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

    public PedidoEstadoPago getEstadoPago() {
        return estadoPago == null ? PedidoEstadoPago.PAGADO : estadoPago;
    }

    public void setEstado(PedidoEstado estado) {
        PedidoEstado nuevoEstado = Objects.requireNonNull(estado);
        if (this.estado != nuevoEstado) {
            LocalDateTime fechaCambio = LocalDateTime.now();
            this.estado = nuevoEstado;
            if (historialEstados == null) {
                historialEstados = new ArrayList<>();
            }
            historialEstados.add(new EstadoPedidoHistorial(nuevoEstado, fechaCambio));
            if (nuevoEstado == PedidoEstado.ENVIADO) {
                estimatedDeliveryDate = calcularFechaEstimadaEntrega(fechaCambio.toLocalDate());
            } else if (nuevoEstado != PedidoEstado.ENTREGADO) {
                estimatedDeliveryDate = null;
            }
        }
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public List<EstadoPedidoHistorial> getHistorialEstados() {
        return historialEstados == null ? List.of() : List.copyOf(historialEstados);
    }

    public LocalDate getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    static LocalDate calcularFechaEstimadaEntrega(LocalDate fechaEnvio) {
        LocalDate fechaEstimada = fechaEnvio;
        int diasHabiles = 0;
        while (diasHabiles < 3) {
            fechaEstimada = fechaEstimada.plusDays(1);
            DayOfWeek diaSemana = fechaEstimada.getDayOfWeek();
            if (diaSemana != DayOfWeek.SATURDAY && diaSemana != DayOfWeek.SUNDAY) {
                diasHabiles++;
            }
        }
        return fechaEstimada;
    }

}
