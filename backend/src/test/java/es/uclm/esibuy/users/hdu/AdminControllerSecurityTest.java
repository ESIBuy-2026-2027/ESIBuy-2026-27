package es.uclm.esibuy.users.hdu;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.security.config.SecurityConfig;
import es.uclm.esibuy.users.controller.AdminController;
import es.uclm.esibuy.users.service.AdminService;

@WebMvcTest(AdminController.class)
@Import({ SecurityConfig.class, ApiExceptionHandler.class })
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminService adminService;

    @Test
    void unUsuarioNoAdministradorNoPuedeBloquearUnaCuenta() throws Exception {
        mockMvc.perform(
                post("/api/admin/usuarios/123/bloquear")
                        .with(user("usuario@test.com").roles("CLIENT"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void unUsuarioNoAdministradorNoPuedeDesbloquearUnaCuenta() throws Exception {
        mockMvc.perform(
                post("/api/admin/usuarios/123/desbloquear")
                        .with(user("usuario@test.com").roles("CLIENT"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }
}