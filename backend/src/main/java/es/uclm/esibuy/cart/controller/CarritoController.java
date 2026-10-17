package es.uclm.esibuy.cart.controller;

import es.uclm.esibuy.cart.dto.AnadirItemCarritoDTO;
import es.uclm.esibuy.cart.dto.CarritoDTO;
import es.uclm.esibuy.cart.service.CarritoService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carrito")
@PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
public class CarritoController {

    private final CarritoService carritoService;

    public CarritoController(CarritoService carritoService) {
        this.carritoService = carritoService;
    }

    @PostMapping("/items")
    public ResponseEntity<CarritoDTO> anadirItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody AnadirItemCarritoDTO request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(carritoService.anadirItem(principal.id(), request));
    }

    @GetMapping
    public ResponseEntity<CarritoDTO> getCarrito(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(carritoService.getCarrito(principal.id()));
    }
}
