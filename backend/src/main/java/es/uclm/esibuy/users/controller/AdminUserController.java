package es.uclm.esibuy.users.controller;

import es.uclm.esibuy.common.dto.ApiResponse;
import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.dto.UserResponseDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> obtener(@PathVariable String id,
            HttpServletRequest servletRequest, CsrfToken csrfToken) {
        csrfToken.getToken();
        User user = adminUserService.obtenerPorId(id);
        return ResponseEntity.ok(response("Usuario cargado correctamente", user, servletRequest));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponseDTO>> actualizar(@PathVariable String id,
            @Valid @RequestBody UpdateUserRequestDTO request, HttpServletRequest servletRequest) {
        User user = adminUserService.actualizar(id, request);
        return ResponseEntity.ok(response("Usuario actualizado correctamente", user, servletRequest));
    }

    private ApiResponse<UserResponseDTO> response(String message, User user, HttpServletRequest request) {
        UserResponseDTO data = new UserResponseDTO(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), user.getPhone());
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return new ApiResponse<>(true, message, data, correlationId == null ? null : correlationId.toString());
    }
}