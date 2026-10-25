package es.uclm.esibuy.pedidos.controller;

import es.uclm.esibuy.pedidos.dto.ActualizarEstadoPedidoDTO;
import es.uclm.esibuy.pedidos.dto.CheckoutResumenDTO;
import es.uclm.esibuy.pedidos.dto.CrearPedidoDTO;
import es.uclm.esibuy.pedidos.dto.PedidoConfirmadoDTO;
import es.uclm.esibuy.pedidos.dto.PedidoVendedorDTO;
import es.uclm.esibuy.pedidos.service.PedidoService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/checkout-resumen")
    @PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
    public ResponseEntity<CheckoutResumenDTO> obtenerCheckoutResumen(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ResponseEntity.ok(pedidoService.obtenerCheckoutResumen(principal.id()));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
    public ResponseEntity<PedidoConfirmadoDTO> crearPedido(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CrearPedidoDTO request) {
        return ResponseEntity.ok(pedidoService.crearPedido(principal.id(), request));
    }

    @GetMapping({"/vendedor", "/seller"})
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<List<PedidoVendedorDTO>> obtenerPedidosDelVendedor(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ResponseEntity.ok(pedidoService.obtenerPedidosDelVendedor(principal.id()));
    }

    @PatchMapping("/vendedor/{pedidoId}/estado")
    @PreAuthorize("hasRole('SELLER')")
    public ResponseEntity<Void> actualizarEstadoPedido(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String pedidoId,
            @Valid @RequestBody ActualizarEstadoPedidoDTO request) {
        pedidoService.actualizarEstadoPedido(principal.id(), pedidoId, request.estado(), request.justificacion());
        return ResponseEntity.noContent().build();
    }
}
