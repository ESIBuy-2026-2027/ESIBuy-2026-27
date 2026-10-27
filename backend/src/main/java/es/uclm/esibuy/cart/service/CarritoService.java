package es.uclm.esibuy.cart.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.uclm.esibuy.cart.dto.ActualizarCantidadItemDTO;
import es.uclm.esibuy.cart.dto.AnadirItemCarritoDTO;
import es.uclm.esibuy.cart.dto.CarritoDTO;
import es.uclm.esibuy.cart.dto.ItemCarritoDTO;
import es.uclm.esibuy.cart.model.Carrito;
import es.uclm.esibuy.cart.model.ItemCarrito;
import es.uclm.esibuy.cart.repository.CarritoRepository;
import es.uclm.esibuy.common.exception.ProductNotFoundException;
import es.uclm.esibuy.common.exception.ProductoNoDisponibleException;
import es.uclm.esibuy.common.exception.StockInsuficienteException;
import es.uclm.esibuy.products.model.Product;
import es.uclm.esibuy.products.repository.ProductRepository;
import es.uclm.esibuy.users.model.CustomerType;
import es.uclm.esibuy.users.repository.UserRepository;

@Service
public class CarritoService {

    private final ProductRepository productRepository;
    private final CarritoRepository carritoRepository;
    private final UserRepository userRepository;

    private static final String CLIENTE_ID_OBLIGATORIO = "El identificador del cliente es obligatorio";
    private static final String PRODUCTO_ID_OBLIGATORIO = "El identificador del producto es obligatorio";

    @Autowired
    public CarritoService(
            ProductRepository productRepository,
            CarritoRepository carritoRepository,
            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.carritoRepository = carritoRepository;
        this.userRepository = userRepository;
    }

    public CarritoService(ProductRepository productRepository, CarritoRepository carritoRepository) {
        this(productRepository, carritoRepository, null);
    }

    public CarritoDTO anadirItem(String clienteId, AnadirItemCarritoDTO request) {
        validarSolicitud(clienteId, request);
        Product product = productRepository.findById(request.productoId())
                .orElseThrow(ProductNotFoundException::new);
        validarProductoDisponible(product);

        Carrito carrito = carritoRepository.findByClienteId(clienteId)
                .orElseGet(() -> new Carrito(clienteId));
        validarStock(product, carrito, request.cantidad());
        boolean isPremium = isPremiumClient(clienteId);
        BigDecimal effectivePrice = calculateEffectivePrice(product, isPremium);
        carrito.addOrIncrementItem(
                product.getId(),
                product.getName(),
                effectivePrice,
                request.cantidad(),
                product.getImageUrl());
        return toDto(carritoRepository.save(carrito));
    }

    public CarritoDTO getCarrito(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException(CLIENTE_ID_OBLIGATORIO);
        }
        return carritoRepository.findByClienteId(clienteId)
                .map(this::toDto)
                .orElseGet(() -> toDto(new Carrito(clienteId)));
    }

    public CarritoDTO actualizarCantidadItem(
            String clienteId, String productoId, ActualizarCantidadItemDTO request) {
        validarSolicitudActualizacion(clienteId, productoId, request);
        Carrito carrito = obtenerCarrito(clienteId);
        ItemCarrito item = buscarItem(carrito, productoId);

        if (request.cantidad() > item.getCantidad()) {
            Product product = productRepository.findById(productoId)
                    .orElseThrow(ProductNotFoundException::new);
            validarProductoDisponible(product);
            validarStock(product, request.cantidad());
        }

        carrito.updateItemQuantity(productoId, request.cantidad());
        return toDto(carritoRepository.save(carrito));
    }

    public CarritoDTO eliminarItem(String clienteId, String productoId) {
        validarClienteYProducto(clienteId, productoId);
        Carrito carrito = obtenerCarrito(clienteId);
        if (!carrito.removeItem(productoId)) {
            throw new ProductNotFoundException();
        }
        return toDto(carritoRepository.save(carrito));
    }

    private void validarSolicitud(String clienteId, AnadirItemCarritoDTO request) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException(CLIENTE_ID_OBLIGATORIO);
        }
        if (request == null || request.productoId() == null || request.productoId().isBlank()) {
            throw new IllegalArgumentException(PRODUCTO_ID_OBLIGATORIO);
        }
        if (request.cantidad() == null || request.cantidad() < 1) {
            throw new IllegalArgumentException("La cantidad debe ser como mínimo 1");
        }
    }

    private void validarSolicitudActualizacion(
            String clienteId, String productoId, ActualizarCantidadItemDTO request) {
        validarClienteYProducto(clienteId, productoId);
        if (request == null || request.cantidad() == null || request.cantidad() < 0) {
            throw new IllegalArgumentException("La cantidad debe ser como mínimo 0");
        }
    }

    private void validarClienteYProducto(String clienteId, String productoId) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException(CLIENTE_ID_OBLIGATORIO);
        }
        if (productoId == null || productoId.isBlank()) {
            throw new IllegalArgumentException(PRODUCTO_ID_OBLIGATORIO);
        }
    }

    private Carrito obtenerCarrito(String clienteId) {
        return carritoRepository.findByClienteId(clienteId)
                .orElseThrow(ProductNotFoundException::new);
    }

    private ItemCarrito buscarItem(Carrito carrito, String productoId) {
        return carrito.getItems().stream()
                .filter(item -> item.getProductoId().equals(productoId))
                .findFirst()
                .orElseThrow(ProductNotFoundException::new);
    }

    private void validarProductoDisponible(Product product) {
        if (!product.isAvailable()) {
            throw new ProductoNoDisponibleException();
        }
    }

    private void validarStock(Product product, Carrito carrito, int cantidadSolicitada) {
        int cantidadEnCarrito = carrito.getItems().stream()
                .filter(item -> item.getProductoId().equals(product.getId()))
                .mapToInt(ItemCarrito::getCantidad)
                .findFirst()
                .orElse(0);
        validarStock(product, (long) cantidadEnCarrito + cantidadSolicitada);
    }

    private void validarStock(Product product, long cantidadSolicitada) {
        if (cantidadSolicitada > product.getStock()) {
            throw new StockInsuficienteException();
        }
    }

    private CarritoDTO toDto(Carrito carrito) {
        boolean isPremium = isPremiumClient(carrito.getClienteId());
        List<ItemCarritoDTO> items = carrito.getItems().stream()
                .map(item -> toDto(item, isPremium))
                .toList();

        BigDecimal totalSinDescuento = items.stream()
                .map(item -> (item.precioOriginal() != null ? item.precioOriginal() : item.precioUnitario())
                        .multiply(BigDecimal.valueOf(item.cantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal precioTotal = carrito.getPrecioTotal();
        BigDecimal descuentoTotal = totalSinDescuento.compareTo(precioTotal) > 0
                ? totalSinDescuento.subtract(precioTotal)
                : BigDecimal.ZERO;

        return new CarritoDTO(
                carrito.getId(),
                carrito.getClienteId(),
                items,
                precioTotal,
                totalSinDescuento,
                descuentoTotal);
    }

    private ItemCarritoDTO toDto(ItemCarrito item, boolean isPremium) {
        String imageUrl = item.getImageUrl();
        BigDecimal precioOriginal = item.getPrecioUnitario();
        BigDecimal descuento = BigDecimal.ZERO;
        boolean esDescuentoPremium = false;

        if (item.getProductoId() != null) {
            Product product = productRepository.findById(item.getProductoId()).orElse(null);
            if (product != null) {
                if (imageUrl == null || imageUrl.isBlank()) {
                    imageUrl = product.getImageUrl();
                }
                precioOriginal = product.getPrice();
                if (isPremium && product.getPremiumDiscount() != null
                        && product.getPremiumDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    descuento = product.getPremiumDiscount();
                    esDescuentoPremium = true;
                } else if (product.getDiscount() != null
                        && product.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    descuento = product.getDiscount();
                }
            }
        }

        return new ItemCarritoDTO(
                item.getProductoId(),
                item.getNombre(),
                item.getPrecioUnitario(),
                item.getCantidad(),
                item.getSubtotal(),
                imageUrl,
                precioOriginal,
                descuento,
                esDescuentoPremium);
    }

    private boolean isPremiumClient(String clienteId) {
        if (clienteId == null || userRepository == null) {
            return false;
        }
        return userRepository.findById(clienteId)
                .map(user -> user.getClientProfile() != null
                        && CustomerType.PREMIUM.equals(user.getClientProfile().getCustomerType()))
                .orElse(false);
    }

    private BigDecimal calculateEffectivePrice(Product product, boolean isPremium) {
        BigDecimal price = product.getPrice();
        BigDecimal discount = BigDecimal.ZERO;

        if (isPremium && product.getPremiumDiscount() != null
                && product.getPremiumDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discount = product.getPremiumDiscount();
        } else if (product.getDiscount() != null
                && product.getDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discount = product.getDiscount();
        }

        if (discount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal factor = BigDecimal.valueOf(100).subtract(discount)
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            return price.multiply(factor).setScale(2, RoundingMode.HALF_UP);
        }

        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
