package es.uclm.esibuy.cart.service;

import es.uclm.esibuy.cart.dto.AnadirItemCarritoDTO;
import es.uclm.esibuy.cart.dto.ActualizarCantidadItemDTO;
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
import org.springframework.stereotype.Service;

@Service
public class CarritoService {

    private final ProductRepository productRepository;
    private final CarritoRepository carritoRepository;

    public CarritoService(ProductRepository productRepository, CarritoRepository carritoRepository) {
        this.productRepository = productRepository;
        this.carritoRepository = carritoRepository;
    }

    public CarritoDTO anadirItem(String clienteId, AnadirItemCarritoDTO request) {
        validarSolicitud(clienteId, request);
        Product product = productRepository.findById(request.productoId())
                .orElseThrow(ProductNotFoundException::new);
        validarProductoDisponible(product);

        Carrito carrito = carritoRepository.findByClienteId(clienteId)
                .orElseGet(() -> new Carrito(clienteId));
        validarStock(product, carrito, request.cantidad());
        carrito.addOrIncrementItem(
                product.getId(),
                product.getName(),
                product.getPrice(),
                request.cantidad(),
                product.getImageUrl());
        return toDto(carritoRepository.save(carrito));
    }

    public CarritoDTO getCarrito(String clienteId) {
        if (clienteId == null || clienteId.isBlank()) {
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
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
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }
        if (request == null || request.productoId() == null || request.productoId().isBlank()) {
            throw new IllegalArgumentException("El identificador del producto es obligatorio");
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
            throw new IllegalArgumentException("El identificador del cliente es obligatorio");
        }
        if (productoId == null || productoId.isBlank()) {
            throw new IllegalArgumentException("El identificador del producto es obligatorio");
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
        return new CarritoDTO(
                carrito.getId(),
                carrito.getClienteId(),
                carrito.getItems().stream().map(this::toDto).toList(),
                carrito.getPrecioTotal());
    }

    private ItemCarritoDTO toDto(ItemCarrito item) {
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
