package es.uclm.esibuy.users.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esibuy.common.dto.ApiResponse;
import es.uclm.esibuy.common.filter.CorrelationIdFilter;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.dto.UserResponseDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.AdminUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping({ "/api/v1/admin/users", "/api/admin/users", "/api/admin/usuarios" })
public class AdminUserController {

        private final AdminUserService adminUserService;

        public AdminUserController(AdminUserService adminUserService) {
                this.adminUserService = adminUserService;
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<UserResponseDTO>> getUser(
                        @PathVariable String id,
                        HttpServletRequest servletRequest,
                        CsrfToken csrfToken) {

                csrfToken.getToken();
                User user = adminUserService.findById(id);

                return ResponseEntity.ok(
                                buildResponse("Usuario cargado correctamente", user, servletRequest));
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<UserResponseDTO>> updateUser(
                        @PathVariable String id,
                        @Valid @RequestBody UpdateUserRequestDTO request,
                        HttpServletRequest servletRequest) {

                User user = adminUserService.update(id, request);

                return ResponseEntity.ok(
                                buildResponse("Usuario actualizado correctamente", user, servletRequest));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<Void>> deleteUser(
                        @PathVariable String id,
                        @AuthenticationPrincipal AuthenticatedUserPrincipal principal,
                        HttpServletRequest servletRequest) {

                if (principal == null) {
                        throw new IllegalStateException(
                                        "No se pudo validar la identidad del administrador");
                }

                adminUserService.delete(id, principal.id());

                Object correlationId = servletRequest.getAttribute(CorrelationIdFilter.ATTRIBUTE);

                return ResponseEntity.ok(
                                new ApiResponse<>(
                                                true,
                                                "Usuario eliminado correctamente",
                                                null,
                                                correlationId == null ? null : correlationId.toString()));
        }

        private ApiResponse<UserResponseDTO> buildResponse(
                        String message,
                        User user,
                        HttpServletRequest request) {

                String headquarters = user.getAdminProfile() != null
                                ? user.getAdminProfile().getHeadquarters()
                                : null;

                String businessName = user.getSellerProfile() != null
                                ? user.getSellerProfile().getBusinessName()
                                : null;

                String defaultCategory = user.getSellerProfile() != null
                                ? user.getSellerProfile().getDefaultCategory()
                                : null;

                java.time.LocalDate birthDate = user.getClientProfile() != null
                                ? user.getClientProfile().getBirthDate()
                                : null;

                String phone = user.getRole() == UserRole.ADMIN
                                ? null
                                : user.getPhone();

                UserResponseDTO data = UserResponseDTO.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .phone(phone)
                                .role(user.getRole() == null ? null : user.getRole().name())
                                .profileImage(user.getProfileImage())
                                .nationalId(user.getNationalId())
                                .headquarters(headquarters)
                                .businessName(businessName)
                                .birthDate(birthDate)
                                .defaultCategory(defaultCategory)
                                .build();

                Object correlationId = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);

                return new ApiResponse<>(
                                true,
                                message,
                                data,
                                correlationId == null ? null : correlationId.toString());
        }
}