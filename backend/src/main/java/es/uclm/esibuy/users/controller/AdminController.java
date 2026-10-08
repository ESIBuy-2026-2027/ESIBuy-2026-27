
package es.uclm.esibuy.users.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import es.uclm.esibuy.users.dto.AltaAdminDTO;
import es.uclm.esibuy.users.dto.UserAdminDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/alta")
    public ResponseEntity<Map<String, Object>> darAltaAdministrador(
            @Valid @RequestBody AltaAdminDTO request) {
        User user = adminService.crearAdministrador(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildAdminResponse(user));
    }

    private Map<String, Object> buildAdminResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("nombre", user.getFirstName());
        response.put("apellidos", user.getLastName());
        response.put("dni", user.getNationalId());
        response.put("rol", user.getRole() != null ? user.getRole().name() : null);
        response.put("activo", user.isActive());
        response.put("fechaIncorporacion", user.getAdminProfile() != null ? user.getAdminProfile().getHireDate() : null);
        response.put("sede", user.getAdminProfile() != null ? user.getAdminProfile().getHeadquarters() : null);
        response.put("imagenPerfil", user.getProfileImage());
        return response;
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserAdminDTO>> obtenerTodosLosUsuarios(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String estado) {

        return ResponseEntity.ok(adminService.obtenerUsuariosFiltrados(rol, estado));
    }


    @PostMapping("/usuarios/{id}/bloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> bloquearUsuario(@PathVariable String id) {
        adminService.bloquearUsuario(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/usuarios/{id}/desbloquear")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desbloquearUsuario(@PathVariable String id) {
        adminService.desbloquearUsuario(id);
        return ResponseEntity.ok().build();
    }
}
