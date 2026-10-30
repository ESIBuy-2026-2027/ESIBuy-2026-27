
package es.uclm.esibuy.orders.controller;

import java.util.List;

import es.uclm.esibuy.orders.dto.CheckoutSummaryDTO;
import es.uclm.esibuy.orders.dto.CreateOrderDTO;
import es.uclm.esibuy.orders.dto.CustomerOrderDTO;
import es.uclm.esibuy.orders.dto.OrderConfirmationDTO;
import es.uclm.esibuy.orders.dto.SellerOrderDTO;
import es.uclm.esibuy.orders.dto.ShippingTrackingDTO;
import es.uclm.esibuy.orders.dto.UpdateOrderStatusDTO;
import es.uclm.esibuy.orders.service.OrderService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/checkout-resumen")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
    public ResponseEntity<CheckoutSummaryDTO> getCheckoutSummary(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @RequestParam(name = "vendedorId", required = false) String sellerId,
            @RequestParam(name = "productoIds", required = false) List<String> productIds) {

        if ((sellerId != null && !sellerId.isBlank())
                || (productIds != null && !productIds.isEmpty())) {
            return ResponseEntity.ok(
                    orderService.getCheckoutSummary(principal.id(), sellerId, productIds));
        }

        return ResponseEntity.ok(orderService.getCheckoutSummary(principal.id()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
    public ResponseEntity<OrderConfirmationDTO> createOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CreateOrderDTO request) {

        return ResponseEntity.ok(orderService.createOrder(principal.id(), request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
    public ResponseEntity<List<CustomerOrderDTO>> getCustomerOrders(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {

        return ResponseEntity.ok(orderService.getCustomerOrders(principal.id()));
    }

    @GetMapping("/{pedidoId}")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
    public ResponseEntity<CustomerOrderDTO> getCustomerOrder(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("pedidoId") String orderId) {

        return ResponseEntity.ok(
                orderService.getCustomerOrder(principal.id(), orderId));
    }

    @GetMapping("/{pedidoId}/historial-estados")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
    public ResponseEntity<ShippingTrackingDTO> getShippingStatusHistory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("pedidoId") String orderId) {

        return ResponseEntity.ok(
                orderService.getShippingStatusHistory(principal.id(), orderId));
    }

    @GetMapping({ "/vendedor", "/seller" })
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<SellerOrderDTO>> getSellerOrders(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {

        return ResponseEntity.ok(orderService.getSellerOrders(principal.id()));
    }

    @PatchMapping("/vendedor/{pedidoId}/estado")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> updateOrderStatus(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable("pedidoId") String orderId,
            @Valid @RequestBody UpdateOrderStatusDTO request) {

        orderService.updateOrderStatus(
                principal.id(),
                orderId,
                request.status(),
                request.justification());

        return ResponseEntity.noContent().build();
    }
}
