package es.uclm.esibuy.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esibuy.common.dto.ApiResponse;
import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.dto.ClientRegistrationRequest;
import es.uclm.esibuy.users.dto.SellerRegistrationRequest;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.dto.UserResponseDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> eliminar(@PathVariable String id,
            @AuthenticationPrincipal AuthenticatedUserPrincipal principal, HttpServletRequest servletRequest) {
        if (principal == null) {
            throw new IllegalStateException("No se pudo validar la identidad del administrador");
        }
        adminUserService.eliminar(id, principal.id());
        Object correlationId = servletRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return ResponseEntity.ok(new ApiResponse<>(true, "Usuario eliminado correctamente", null,
                correlationId == null ? null : correlationId.toString()));
    }
    
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register/client")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerClient(@Valid @RequestBody ClientRegistrationRequest request,
            HttpServletRequest servletRequest) {

        UserResponseDTO createdUser = adminUserService.createClientAdmin(request);
        Object correlationId = servletRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Cliente creado con éxito", 
            createdUser, correlationId == null ? null : correlationId.toString());
        
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/register/seller")
    public ResponseEntity<ApiResponse<UserResponseDTO>> registerSeller(@Valid @RequestBody SellerRegistrationRequest request,
            HttpServletRequest servletRequest) {
                
        UserResponseDTO createdUser = adminUserService.createSellerAdmin(request);
        Object correlationId = servletRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        ApiResponse<UserResponseDTO> response = new ApiResponse<>(true, "Vendedor creado con éxito", 
            createdUser, correlationId == null ? null : correlationId.toString());
        
        return ResponseEntity.ok(response);
    }

    private ApiResponse<UserResponseDTO> response(String message, User user, HttpServletRequest request) {
        UserResponseDTO data = new UserResponseDTO(user.getId(), user.getEmail(), user.getFirstName(),
            user.getLastName(), user.getPhone(), user.getRole() == null ? null : user.getRole().name());
        Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        return new ApiResponse<>(true, message, data, correlationId == null ? null : correlationId.toString());
    }
}