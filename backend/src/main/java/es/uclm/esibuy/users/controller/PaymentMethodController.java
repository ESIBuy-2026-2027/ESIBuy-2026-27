package es.uclm.esibuy.users.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.dto.PaymentMethodRequest;
import es.uclm.esibuy.users.dto.PaymentMethodResponse;
import es.uclm.esibuy.users.service.PaymentMethodService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users/me/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> list(
            Authentication authentication) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(paymentMethodService.list(principal.id()));
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> add(
            Authentication authentication,
            @Valid @RequestBody PaymentMethodRequest request) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PaymentMethodResponse created = paymentMethodService.add(principal.id(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{paymentMethodId}/default")
    public ResponseEntity<PaymentMethodResponse> setDefault(
            Authentication authentication,
            @PathVariable String paymentMethodId) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                paymentMethodService.setDefault(principal.id(), paymentMethodId));
    }

    @PutMapping("/{paymentMethodId}")
    public ResponseEntity<PaymentMethodResponse> update(
            Authentication authentication,
            @PathVariable String paymentMethodId,
            @Valid @RequestBody PaymentMethodRequest request) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
                paymentMethodService.update(principal.id(), paymentMethodId, request));
    }

    @DeleteMapping("/{paymentMethodId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable String paymentMethodId) {

        if (authentication == null
                || !(authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        paymentMethodService.delete(principal.id(), paymentMethodId);

        return ResponseEntity.noContent().build();
    }
}