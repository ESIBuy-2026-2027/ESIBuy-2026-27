
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

    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdmin(
            @Valid @RequestBody AltaAdminDTO request) {
        User user = adminService.createAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(buildAdminResponse(user));
    }

    private Map<String, Object> buildAdminResponse(User user) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("firstName", user.getFirstName());
        response.put("lastName", user.getLastName());
        response.put("nationalId", user.getNationalId());
        response.put("role", user.getRole() != null ? user.getRole().name() : null);
        response.put("active", user.isActive());
        response.put("hireDate",
                user.getAdminProfile() != null ? user.getAdminProfile().getHireDate() : null);
        response.put("headquarters", user.getAdminProfile() != null ? user.getAdminProfile().getHeadquarters() : null);
        response.put("profileImage", user.getProfileImage());
        return response;
    }

    @GetMapping({ "/users", "/usuarios" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserAdminDTO>> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {

        return ResponseEntity.ok(adminService.getFilteredUsers(role, status));
    }

    @PostMapping({ "/usuarios/{id}/bloquear", "/users/{id}/block" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> blockUser(@PathVariable String id) {
        adminService.blockUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping({ "/usuarios/{id}/desbloquear", "/users/{id}/unblock" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unblockUser(@PathVariable String id) {
        adminService.unblockUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping({ "/usuarios/{id}/activar", "/users/{id}/activate" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable String id) {
        adminService.activateUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping({ "/usuarios/{id}/desactivar", "/users/{id}/deactivate" })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable String id) {
        adminService.deactivateUser(id);
        return ResponseEntity.ok().build();
    }
}
