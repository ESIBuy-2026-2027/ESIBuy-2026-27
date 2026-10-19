package es.uclm.esibuy.pedidos.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.mongodb.client.result.UpdateResult;
import es.uclm.esibuy.cart.dto.ItemCarritoDTO;
import es.uclm.esibuy.cart.model.Carrito;
import es.uclm.esibuy.cart.model.ItemCarrito;
import es.uclm.esibuy.cart.repository.CarritoRepository;
import es.uclm.esibuy.common.exception.PagoFallidoException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.RecursoCheckoutNoEncontradoException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.pedidos.dto.CheckoutResumenDTO;
import es.uclm.esibuy.pedidos.dto.CrearPedidoDTO;
import es.uclm.esibuy.pedidos.dto.MetodoPagoCheckoutDTO;
import es.uclm.esibuy.pedidos.dto.PedidoConfirmadoDTO;
import es.uclm.esibuy.pedidos.model.DireccionEnvioPedido;
import es.uclm.esibuy.pedidos.model.MetodoPagoPedido;
import es.uclm.esibuy.pedidos.model.Pedido;
import es.uclm.esibuy.pedidos.model.PedidoEstado;
import es.uclm.esibuy.pedidos.model.PedidoItem;
import es.uclm.esibuy.pedidos.repository.PedidoRepository;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.dto.ShippingAddressResponse;
import es.uclm.esibuy.users.model.ClientProfile;
import es.uclm.esibuy.users.model.PaymentMethod;
import es.uclm.esibuy.users.model.ShippingAddress;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.repository.UserRepository;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private final UserRepository userRepository;
    private final CarritoRepository carritoRepository;
    private final ProductRepository productRepository;
    private final PedidoRepository pedidoRepository;
    private final MongoTemplate mongoTemplate;
    private final PasarelaPago pasarelaPago;

    private static final String STOCK_STRING = "stock";

    public PedidoService(
            UserRepository userRepository,
            CarritoRepository carritoRepository,
            ProductRepository productRepository,
            PedidoRepository pedidoRepository,
            MongoTemplate mongoTemplate,
            PasarelaPago pasarelaPago) {
        this.userRepository = userRepository;
        this.carritoRepository = carritoRepository;
        this.productRepository = productRepository;
        this.pedidoRepository = pedidoRepository;
        this.mongoTemplate = mongoTemplate;
        this.pasarelaPago = pasarelaPago;
    }

    public CheckoutResumenDTO obtenerCheckoutResumen(String clienteId) {
        User cliente = obtenerCliente(clienteId);
        ClientProfile perfil = cliente.getClientProfile();
        Carrito carrito = carritoRepository.findByClienteId(clienteId).orElseGet(() -> new Carrito(clienteId));
        List<ShippingAddressResponse> direcciones = perfil.getAddresses().stream()
                .map(ShippingAddressResponse::from)
                .toList();
        List<MetodoPagoCheckoutDTO> metodosPago = perfil.getPaymentMethods().stream()
                .map(MetodoPagoCheckoutDTO::from)
                .toList();
        List<ItemCarritoDTO> items = carrito.getItems().stream()
                .map(this::toItemCarritoDTO)
                .toList();
        return new CheckoutResumenDTO(direcciones, metodosPago, items, carrito.getPrecioTotal());
    }

    public PedidoConfirmadoDTO crearPedido(String clienteId, CrearPedidoDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("La solicitud del pedido es obligatoria");
        }
        User cliente = obtenerCliente(clienteId);
        ShippingAddress direccion = obtenerDireccion(cliente.getClientProfile(), request.direccionId());
        PaymentMethod metodoPago = obtenerMetodoPago(cliente.getClientProfile(), request.metodoPagoId());
        Carrito carrito = carritoRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("El carrito está vacío"));
        if (carrito.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }

        List<PedidoItem> items = prepararItems(carrito);
        BigDecimal total = items.stream()
                .map(PedidoItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reservarStock(items);

        MetodoPagoPedido resumenPago = MetodoPagoPedido.from(metodoPago);
        boolean pagoAceptado;
        try {
            pagoAceptado = pasarelaPago.cobrar(resumenPago, total);
        } catch (RuntimeException exception) {
            liberarStock(items, exception);
            throw exception;
        }
        if (!pagoAceptado) {
            PagoFallidoException excepcion = new PagoFallidoException();
            liberarStock(items, excepcion);
            throw new PagoFallidoException();
        }

        Pedido pedido = new Pedido(
                clienteId,
                items,
                total,
                DireccionEnvioPedido.from(direccion),
                resumenPago,
                PedidoEstado.PAGADO);
        Pedido guardado = pedidoRepository.save(pedido);
        carrito.vaciar();
        carritoRepository.save(carrito);
        return new PedidoConfirmadoDTO(
                guardado.getId(),
                guardado.getPrecioTotal(),
                guardado.getEstado(),
                guardado.getFechaCreacion());
    }

    private User obtenerCliente(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }
        User cliente = userRepository.findById(clienteId).orElseThrow(UserNotFoundException::new);
        if (cliente.getClientProfile() == null) {
            throw new UserNotFoundException();
        }
        return cliente;
    }

    private ShippingAddress obtenerDireccion(ClientProfile perfil, String direccionId) {
        return perfil.getAddresses().stream()
                .filter(address -> address.getId().equals(direccionId))
                .findFirst()
                .orElseThrow(RecursoCheckoutNoEncontradoException::new);
    }

    private PaymentMethod obtenerMetodoPago(ClientProfile perfil, String metodoPagoId) {
        return perfil.getPaymentMethods().stream()
                .filter(method -> method.getId().equals(metodoPagoId))
                .findFirst()
                .orElseThrow(RecursoCheckoutNoEncontradoException::new);
    }

    private List<PedidoItem> prepararItems(Carrito carrito) {
        List<PedidoItem> items = new ArrayList<>();
        for (ItemCarrito item : carrito.getItems()) {
            Product producto = productRepository.findById(item.getProductoId())
                    .orElseThrow(ProductNotFoundException::new);
            if (!producto.isAvailable()) {
                throw new ProductoNoDisponibleException();
            }
            if (producto.getStock() < item.getCantidad()) {
                throw stockInsuficiente(producto);
            }
            BigDecimal precio = producto.getPrice();
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.getCantidad()));
            items.add(new PedidoItem(
                    producto.getId(), producto.getName(), item.getCantidad(), precio, subtotal));
        }
        return List.copyOf(items);
    }

    private void reservarStock(List<PedidoItem> items) {
        List<PedidoItem> reservados = new ArrayList<>();
        try {
            for (PedidoItem item : items) {
                Query query = Query.query(Criteria.where("_id").is(item.productoId())
                        .and(STOCK_STRING).gte(item.cantidad())
                        .and("available").is(true));
                Product actualizado = mongoTemplate.findAndModify(
                        query,
                        new Update().inc(STOCK_STRING, -item.cantidad()),
                        FindAndModifyOptions.options().returnNew(true),
                        Product.class);
                if (actualizado == null) {
                    throw new StockInsuficienteException("Stock insuficiente para " + item.nombre());
                }
                reservados.add(item);
            }
        } catch (RuntimeException exception) {
            liberarStock(reservados, exception);
            throw exception;
        }
    }

    private void liberarStock(List<PedidoItem> items, RuntimeException causa) {
        IllegalStateException errorLiberacion = null;
        for (PedidoItem item : items) {
            try {
                Query query = Query.query(Criteria.where("_id").is(item.productoId()));
                UpdateResult resultado = mongoTemplate.updateFirst(
                        query, new Update().inc(STOCK_STRING, item.cantidad()), Product.class);
                if (resultado == null || !resultado.wasAcknowledged() || resultado.getMatchedCount() == 0) {
                    throw new IllegalStateException("No se pudo liberar una reserva de stock");
                }
            } catch (RuntimeException exception) {
                if (errorLiberacion == null) {
                    errorLiberacion = new IllegalStateException("No se pudo liberar el stock reservado", causa);
                }
                errorLiberacion.addSuppressed(exception);
            }
        }
        if (errorLiberacion != null) {
            throw errorLiberacion;
        }
    }

    private StockInsuficienteException stockInsuficiente(Product producto) {
        return new StockInsuficienteException("Stock insuficiente para " + producto.getName());
    }

    private ItemCarritoDTO toItemCarritoDTO(ItemCarrito item) {
        String imageUrl = item.getImageUrl();
        if ((imageUrl == null || imageUrl.isBlank()) && item.getProductoId() != null) {
            imageUrl = productRepository.findById(item.getProductoId())
                    .map(Product::getImageUrl)
                    .orElse(null);
        }
        return new ItemCarritoDTO(
                item.getProductoId(),
                item.getNombre(),
                item.getPrecioUnitario(),
                item.getCantidad(),
                item.getSubtotal(),
                imageUrl);
    }
}
