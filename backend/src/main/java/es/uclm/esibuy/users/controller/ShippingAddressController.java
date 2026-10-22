package es.uclm.esibuy.users.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.dto.ShippingAddressRequest;
import es.uclm.esibuy.users.dto.ShippingAddressResponse;
import es.uclm.esibuy.users.service.ShippingAddressService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me/addresses")
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;

    public ShippingAddressController(ShippingAddressService shippingAddressService) {
        this.shippingAddressService = shippingAddressService;
    }

    @GetMapping
    public ResponseEntity<List<ShippingAddressResponse>> list(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(shippingAddressService.list(principal.id()).stream()
                .map(ShippingAddressResponse::from)
                .toList());
    }

    @PostMapping
    public ResponseEntity<ShippingAddressResponse> add(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @Valid @RequestBody ShippingAddressRequest request) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ShippingAddressResponse.from(shippingAddressService.add(principal.id(), request)));
    }

    @PutMapping("/{addressId}")
    public ResponseEntity<ShippingAddressResponse> update(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String addressId,
            @Valid @RequestBody ShippingAddressRequest request) {
        if (principal == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(shippingAddressService.update(principal.id(), addressId, request));
    }

    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            @PathVariable String addressId) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        shippingAddressService.delete(principal.id(), addressId);

        return ResponseEntity.noContent().build();
    }
}