package es.uclm.esibuy.pedidos.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mongodb.client.result.UpdateResult;
import es.uclm.esibuy.cart.dto.ItemCarritoDTO;
import es.uclm.esibuy.cart.model.Carrito;
import es.uclm.esibuy.cart.model.ItemCarrito;
import es.uclm.esibuy.cart.repository.CarritoRepository;
import es.uclm.esibuy.common.exception.PagoFallidoException;
import es.uclm.esibuy.common.exception.PedidoNotFoundException;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.RecursoCheckoutNoEncontradoException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.pedidos.dto.CheckoutResumenDTO;
import es.uclm.esibuy.pedidos.dto.CrearPedidoDTO;
import es.uclm.esibuy.pedidos.dto.MetodoPagoCheckoutDTO;
import es.uclm.esibuy.pedidos.dto.PedidoConfirmadoDTO;
import es.uclm.esibuy.pedidos.dto.ItemPedidoVendedorDTO;
import es.uclm.esibuy.pedidos.dto.ItemPedidoClienteDTO;
import es.uclm.esibuy.pedidos.dto.DireccionEnvioClienteDTO;
import es.uclm.esibuy.pedidos.dto.EstadoPedidoHistorialDTO;
import es.uclm.esibuy.pedidos.dto.MetodoPagoClienteDTO;
import es.uclm.esibuy.pedidos.dto.PedidoClienteDTO;
import es.uclm.esibuy.pedidos.dto.PedidoVendedorDTO;
import es.uclm.esibuy.pedidos.dto.SeguimientoEnvioDTO;
import es.uclm.esibuy.pedidos.model.DireccionEnvioPedido;
import es.uclm.esibuy.pedidos.model.MetodoPagoPedido;
import es.uclm.esibuy.pedidos.model.Pedido;
import es.uclm.esibuy.pedidos.model.PedidoEstado;
import es.uclm.esibuy.pedidos.model.PedidoEstadoPago;
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

    public CheckoutResumenDTO obtenerCheckoutResumen(String clienteId, String vendedorId, List<String> productoIds) {
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
                .filter(dto -> vendedorId == null || vendedorId.isBlank() || vendedorId.equals(dto.vendedorId()))
                .filter(dto -> productoIds == null || productoIds.isEmpty() || productoIds.contains(dto.productoId()))
                .toList();
        BigDecimal precioTotal = items.stream()
                .map(ItemCarritoDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CheckoutResumenDTO(direcciones, metodosPago, items, precioTotal);
    }

    public CheckoutResumenDTO obtenerCheckoutResumen(String clienteId, String vendedorId) {
        return obtenerCheckoutResumen(clienteId, vendedorId, null);
    }

    public CheckoutResumenDTO obtenerCheckoutResumen(String clienteId) {
        return obtenerCheckoutResumen(clienteId, null);
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

        List<PedidoItem> items = prepararItems(carrito, request.vendedorId(), request.productoIds());
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
                PedidoEstado.PENDIENTE,
                PedidoEstadoPago.PAGADO);
        Pedido guardado = pedidoRepository.save(pedido);
        for (PedidoItem item : items) {
            carrito.removeItem(item.productoId());
        }
        carritoRepository.save(carrito);
        return new PedidoConfirmadoDTO(
                guardado.getId(),
                guardado.getPrecioTotal(),
                guardado.getEstado(),
                guardado.getFechaCreacion(),
                guardado.getEstadoPago());
    }

    public List<PedidoVendedorDTO> obtenerPedidosDelVendedor(String vendedorId) {
        if (vendedorId == null || vendedorId.isBlank()) {
            throw new IllegalArgumentException("El identificador del vendedor es obligatorio");
        }

        Set<String> productosDelVendedor = productRepository.findAllBySellerId(vendedorId).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        return pedidoRepository.findAll().stream()
                .map(pedido -> toSellerOrder(pedido, productosDelVendedor))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public List<PedidoClienteDTO> obtenerPedidosDelCliente(String clienteId) {
        obtenerCliente(clienteId);
        return pedidoRepository.findAllByClienteIdOrderByFechaCreacionDesc(clienteId).stream()
                .map(this::toClientOrder)
                .toList();
    }

    public PedidoClienteDTO obtenerPedidoDelCliente(String clienteId, String pedidoId) {
        obtenerCliente(clienteId);
        if (pedidoId == null || pedidoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del pedido es obligatorio");
        }
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, clienteId)
                .orElseThrow(PedidoNotFoundException::new);
        return toClientOrder(pedido);
    }

    public SeguimientoEnvioDTO obtenerHistorialEstadosEnvio(String clienteId, String pedidoId) {
        obtenerCliente(clienteId);
        if (pedidoId == null || pedidoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del pedido es obligatorio");
        }
        Pedido pedido = pedidoRepository.findByIdAndClienteId(pedidoId, clienteId)
                .orElseThrow(PedidoNotFoundException::new);
        List<EstadoPedidoHistorialDTO> historialEstados = pedido.getHistorialEstados().stream()
                .map(evento -> new EstadoPedidoHistorialDTO(evento.estado(), evento.fecha()))
                .toList();
        return new SeguimientoEnvioDTO(historialEstados, pedido.getEstimatedDeliveryDate());
    }

    public void actualizarEstadoPedido(
            String vendedorId,
            String pedidoId,
            PedidoEstado estado,
            String justificacion) {
        if (vendedorId == null || vendedorId.isBlank() || pedidoId == null || pedidoId.isBlank()) {
            throw new IllegalArgumentException("El vendedor y el pedido son obligatorios");
        }
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe"));
        if (estado == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        if (esRetroceso(pedido.getEstado(), estado)
                && (justificacion == null || justificacion.isBlank())) {
            throw new IllegalArgumentException("La justificación es obligatoria para retroceder el estado");
        }
        Set<String> productosDelVendedor = productRepository.findAllBySellerId(vendedorId).stream()
                .map(Product::getId)
                .collect(Collectors.toSet());
        boolean perteneceAlVendedor = pedido.getItems().stream()
                .anyMatch(item -> productosDelVendedor.contains(item.productoId()));
        if (!perteneceAlVendedor) {
            throw new IllegalArgumentException("El pedido no pertenece al vendedor");
        }
        pedido.setEstado(estado);
        pedidoRepository.save(pedido);
    }

    private boolean esRetroceso(PedidoEstado actual, PedidoEstado nuevo) {
        if (actual == null) {
            return false;
        }
        List<PedidoEstado> flujo = List.of(
                PedidoEstado.PENDIENTE,
                PedidoEstado.PREPARANDO,
                PedidoEstado.ENVIADO,
                PedidoEstado.ENTREGADO);
        int posicionActual = flujo.indexOf(actual);
        int posicionNueva = flujo.indexOf(nuevo);
        return posicionActual >= 0 && posicionNueva >= 0 && posicionNueva < posicionActual;
    }

    private PedidoVendedorDTO toSellerOrder(Pedido pedido, Set<String> productosDelVendedor) {
        List<ItemPedidoVendedorDTO> items = pedido.getItems().stream()
                .filter(item -> productosDelVendedor.contains(item.productoId()))
                .map(item -> new ItemPedidoVendedorDTO(
                        item.productoId(),
                        item.nombre(),
                        item.cantidad(),
                        item.precioUnitario(),
                        item.subtotal(),
                        resolveImageUrl(item)))
                .toList();

        if (items.isEmpty()) {
            return null;
        }

        User cliente = userRepository.findById(pedido.getClienteId()).orElse(null);
        String clienteNombre = cliente == null
                ? "Cliente no disponible"
                : (cliente.getFirstName() + " " + cliente.getLastName()).trim();
        String clienteEmail = cliente == null ? null : cliente.getEmail();
        BigDecimal total = items.stream()
                .map(ItemPedidoVendedorDTO::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PedidoVendedorDTO(
                pedido.getId(),
                pedido.getFechaCreacion(),
                clienteNombre,
                clienteEmail,
                formatAddress(pedido.getDireccionEnvio()),
                items,
                total,
                pedido.getEstado());
    }

    private PedidoClienteDTO toClientOrder(Pedido pedido) {
        List<ItemPedidoClienteDTO> items = pedido.getItems().stream()
                .map(item -> new ItemPedidoClienteDTO(
                        item.productoId(),
                        item.nombre(),
                        item.cantidad(),
                        item.precioUnitario(),
                        item.subtotal(),
                        resolveImageUrl(item)))
                .toList();
        return new PedidoClienteDTO(
                pedido.getId(),
                pedido.getFechaCreacion(),
                items,
                pedido.getPrecioTotal(),
                pedido.getEstado(),
                toClientShippingAddress(pedido.getDireccionEnvio()),
                pedido.getMetodoPago() == null
                        ? null
                        : new MetodoPagoClienteDTO(
                                pedido.getMetodoPago().tipo(),
                                pedido.getMetodoPago().lastFourDigits()),
                pedido.getEstadoPago());
    }

    private DireccionEnvioClienteDTO toClientShippingAddress(DireccionEnvioPedido address) {
        return new DireccionEnvioClienteDTO(
                address.recipientName(),
                address.street(),
                address.additionalInfo(),
                address.postalCode(),
                address.city(),
                address.province(),
                address.phone());
    }

    private String resolveImageUrl(PedidoItem item) {
        if (item.imageUrl() != null && !item.imageUrl().isBlank()) {
            return item.imageUrl();
        }
        return productRepository.findById(item.productoId())
                .map(Product::getImageUrl)
                .orElse(null);
    }

    private String formatAddress(DireccionEnvioPedido address) {
        return String.join(", ", List.of(
                address.street(),
                address.postalCode() + " " + address.city(),
                address.province()));
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

    private List<PedidoItem> prepararItems(Carrito carrito, String vendedorId, List<String> productoIds) {
        List<PedidoItem> items = new ArrayList<>();
        Set<String> sellers = new java.util.HashSet<>();
        for (ItemCarrito item : carrito.getItems()) {
            if (productoIds != null && !productoIds.isEmpty() && !productoIds.contains(item.getProductoId())) {
                continue;
            }
            Product producto = productRepository.findById(item.getProductoId())
                    .orElseThrow(ProductNotFoundException::new);
            String itemSellerId = producto.getSellerId() != null ? producto.getSellerId() : "SIN_VENDEDOR";
            if (vendedorId != null && !vendedorId.isBlank()) {
                if (!vendedorId.equals(itemSellerId)) {
                    continue;
                }
            }
            if (!producto.isAvailable()) {
                throw new ProductoNoDisponibleException();
            }
            if (producto.getStock() < item.getCantidad()) {
                throw stockInsuficiente(producto);
            }
            sellers.add(itemSellerId);
            BigDecimal precio = producto.getPrice();
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(item.getCantidad()));
            items.add(new PedidoItem(
                    producto.getId(),
                    producto.getName(),
                    item.getCantidad(),
                    precio,
                    subtotal,
                    producto.getImageUrl()));
        }
        if (items.isEmpty()) {
            throw new IllegalArgumentException("No hay productos válidos para tramitar este pedido.");
        }
        if (sellers.size() > 1) {
            throw new IllegalArgumentException(
                    "No se puede realizar un pedido con productos de diferentes vendedores. Debes realizar un pedido independiente por cada vendedor.");
        }
        return List.copyOf(items);
    }

    private List<PedidoItem> prepararItems(Carrito carrito, String vendedorId) {
        return prepararItems(carrito, vendedorId, null);
    }

    private List<PedidoItem> prepararItems(Carrito carrito) {
        return prepararItems(carrito, null);
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
        String vendedorId = null;
        String vendedorNombre = null;
        if (item.getProductoId() != null) {
            Product product = productRepository.findById(item.getProductoId()).orElse(null);
            if (product != null) {
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = product.getImageUrl();
                }
                vendedorId = product.getSellerId();
                vendedorNombre = resolveSellerName(vendedorId);
            }
        }
        return new ItemCarritoDTO(
                item.getProductoId(),
                item.getNombre(),
                item.getPrecioUnitario(),
                item.getCantidad(),
                item.getSubtotal(),
                imageUrl,
                vendedorId,
                vendedorNombre);
    }

    private String resolveSellerName(String sellerId) {
        if (sellerId == null || sellerId.isBlank() || userRepository == null) {
            return null;
        }
        return userRepository.findById(sellerId)
                .map(user -> {
                    if (user.getSellerProfile() != null
                            && user.getSellerProfile().getBusinessName() != null
                            && !user.getSellerProfile().getBusinessName().isBlank()) {
                        return user.getSellerProfile().getBusinessName();
                    }
                    String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                    String lastName = user.getLastName() != null ? user.getLastName() : "";
                    String fullName = (firstName + " " + lastName).trim();
                    return !fullName.isBlank() ? fullName : "Vendedor " + sellerId;
                })
                .orElse("Vendedor " + sellerId);
    }
}
