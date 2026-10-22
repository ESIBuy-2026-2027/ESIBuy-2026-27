package es.uclm.esibuy.pedidos.controller;

import es.uclm.esibuy.pedidos.dto.CheckoutResumenDTO;
import es.uclm.esibuy.pedidos.dto.CrearPedidoDTO;
import es.uclm.esibuy.pedidos.dto.PedidoConfirmadoDTO;
import es.uclm.esibuy.pedidos.service.PedidoService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
@PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/checkout-resumen")
    public ResponseEntity<CheckoutResumenDTO> obtenerCheckoutResumen(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        return ResponseEntity.ok(pedidoService.obtenerCheckoutResumen(principal.id()));
    }

    @PostMapping
    public ResponseEntity<PedidoConfirmadoDTO> crearPedido(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody CrearPedidoDTO request) {
        return ResponseEntity.ok(pedidoService.crearPedido(principal.id(), request));
    }
}
