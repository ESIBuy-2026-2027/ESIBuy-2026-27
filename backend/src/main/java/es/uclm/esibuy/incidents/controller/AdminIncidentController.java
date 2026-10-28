package es.uclm.esibuy.incidents.controller;

import es.uclm.esibuy.incidents.dto.CreateIncidentRequest;
import es.uclm.esibuy.incidents.dto.IncidentResponse;
import es.uclm.esibuy.incidents.model.Incident;
import es.uclm.esibuy.incidents.service.IncidentService;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/incidents")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIncidentController {

    private final IncidentService incidentService;

    public AdminIncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentResponse> create(
            @Valid @RequestBody CreateIncidentRequest request,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
            Authentication authentication) {
        String author = "admin";
        if (principal != null && principal.email() != null && !principal.email().isBlank()) {
            author = principal.email();
        } else if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            author = authentication.getName();
        }

        Incident created = incidentService.create(request, author);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentResponse.from(created));
    }
}

