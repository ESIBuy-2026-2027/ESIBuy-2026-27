package es.uclm.esibuy.cart.controller;

import es.uclm.esibuy.cart.dto.AddCartItemDTO;
import es.uclm.esibuy.cart.dto.CartDTO;
import es.uclm.esibuy.cart.dto.UpdateCartItemQuantityDTO;
import es.uclm.esibuy.cart.service.CartService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carrito")
@PreAuthorize("hasAnyRole('CLIENTE', 'CLIENT')")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<CartDTO> addItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody AddCartItemDTO request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(cartService.addItem(principal.id(), request));
    }

    @GetMapping
    public ResponseEntity<CartDTO> getCart(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(cartService.getCart(principal.id()));
    }

    @PutMapping("/items/{productoId}")
    public ResponseEntity<CartDTO> updateItemQuantity(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String productoId,
            @Valid @RequestBody UpdateCartItemQuantityDTO request) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                cartService.updateItemQuantity(principal.id(), productoId, request));
    }

    @DeleteMapping("/items/{productoId}")
    public ResponseEntity<CartDTO> removeItem(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String productoId) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(cartService.removeItem(principal.id(), productoId));
    }
}