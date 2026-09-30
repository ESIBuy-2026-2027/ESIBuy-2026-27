package es.uclm.esibuy.users.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.uclm.esibuy.users.dto.CandidatoDTO;
import es.uclm.esibuy.users.dto.PromoverAdminDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/candidatos")
    public ResponseEntity<List<CandidatoDTO>> obtenerCandidatos() {
        return ResponseEntity.ok(userService.obtenerCandidatos());
    }

    @PutMapping("/promover/{idUsuario}")
    public ResponseEntity<Map<String, Object>> promoverAAdmin(
            @PathVariable String idUsuario,
            @Valid @RequestBody PromoverAdminDTO request) {
        User user = userService.promoverAAdmin(idUsuario, request);
        return ResponseEntity.ok(buildAdminResponse(user));
    }

    private Map<String, Object> buildAdminResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("nombre", user.getFirstName());
        response.put("apellidos", user.getLastName());
        response.put("role", user.getRole() != null ? user.getRole().name() : null);
        response.put("active", user.isActive());
        response.put("sede", user.getAdminProfile() != null ? user.getAdminProfile().getHeadquarters() : null);
        response.put("profileImage", user.getProfileImage());
        return response;
    }
}
