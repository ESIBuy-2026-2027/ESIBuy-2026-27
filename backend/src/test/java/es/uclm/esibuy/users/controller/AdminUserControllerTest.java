package es.uclm.esibuy.users.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.uclm.esibuy.common.exception.UserDeletionConflictException;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.security.AuthenticatedUserPrincipal;
import es.uclm.esibuy.users.dto.UpdateUserRequestDTO;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.AdminUserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import es.uclm.esibuy.security.config.SecurityConfig;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTest {

    private static final String USER_ID = "507f1f77bcf86cd799439011";
    private static final String ADMIN_ID = "507f1f77bcf86cd799439012";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanUpdateUserWithoutChangingSensitiveProperties() throws Exception {
        User updated = new User("cliente@example.com", "Ana", "García", UserRole.CLIENT);
        updated.setPhone("600111222");
        when(adminUserService.actualizar(eq("user-1"), any(UpdateUserRequestDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/api/v1/admin/users/user-1")
            .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"firstName":" Ana ","lastName":"García","phone":"600111222",
                         "role":"ADMIN","password":"ChangedPassword123!"}
                        """))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("cliente@example.com"))
                .andExpect(jsonPath("$.data.role").value("CLIENT"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        verify(adminUserService).actualizar(eq("user-1"), any(UpdateUserRequestDTO.class));
    }

    @Test
    void nonAdminCannotUpdateUser() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/user-1")
                .with(user("customer").roles("CLIENT"))
            .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"García\"}"))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void anonymousCannotUpdateUser() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/user-1")
                .with(anonymous())
            .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"García\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsInvalidAndScriptLikeValues() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/user-1")
            .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"<script>alert(1)</script>\",\"lastName\":\"García\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsMalformedJsonAsBadRequest() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/user-1")
            .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El formato de la solicitud no es válido"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsPhoneWithOtherThanNineDigits() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/user-1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"Ana\",\"lastName\":\"García\",\"phone\":\"555555555555555\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("El teléfono debe contener exactamente 9 dígitos numéricos"));
    }

    @Test
    void adminCanDeleteUserAndReceivesSuccessMessage() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/{id}", USER_ID)
                .with(authentication(adminAuthentication()))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario eliminado correctamente"));

        verify(adminUserService).eliminar(USER_ID, ADMIN_ID);
    }

    @Test
    void adminCanDeleteUserViaApiAdminUsuariosEndpoint() throws Exception {
        mockMvc.perform(delete("/api/admin/usuarios/{id}", USER_ID)
                .with(authentication(adminAuthentication()))
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Usuario eliminado correctamente"));

        verify(adminUserService).eliminar(USER_ID, ADMIN_ID);
    }

    @Test
    void nonAdminCannotDeleteUserViaApiAdminUsuariosEndpoint() throws Exception {
        mockMvc.perform(delete("/api/admin/usuarios/{id}", USER_ID)
                .with(user("customer").roles("CLIENT"))
                .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void nonAdminCannotDeleteUser() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/{id}", USER_ID)
                .with(user("customer").roles("CLIENT"))
                .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(header().exists("X-Correlation-ID"));
    }

    @Test
    void invalidDeleteRequestReturnsGenericBadRequestWithCorrelationId() throws Exception {
        doThrow(new IllegalStateException("El identificador del usuario no es válido"))
                .when(adminUserService).eliminar(USER_ID, ADMIN_ID);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", USER_ID)
                .with(authentication(adminAuthentication()))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void missingUserReturnsNotFoundWithCorrelationId() throws Exception {
        doThrow(new UserNotFoundException()).when(adminUserService).eliminar(USER_ID, ADMIN_ID);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", USER_ID)
                .with(authentication(adminAuthentication()))
                .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(header().exists("X-Correlation-ID"))
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void sellerWithProductsGetsConflictResponse() throws Exception {
        doThrow(new UserDeletionConflictException()).when(adminUserService).eliminar(USER_ID, ADMIN_ID);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", USER_ID)
                .with(authentication(adminAuthentication()))
                .with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No se puede eliminar el vendedor mientras tenga productos asociados"))
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    private UsernamePasswordAuthenticationToken adminAuthentication() {
        AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(
                ADMIN_ID, "admin@example.com", UserRole.ADMIN, true);
        return UsernamePasswordAuthenticationToken.authenticated(principal, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }
}