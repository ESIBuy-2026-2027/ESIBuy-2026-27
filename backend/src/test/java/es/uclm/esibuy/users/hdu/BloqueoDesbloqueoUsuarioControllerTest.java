package es.uclm.esibuy.users.hdu;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import es.uclm.esibuy.users.controller.AdminController;
import es.uclm.esibuy.users.service.AdminService;

@ExtendWith(MockitoExtension.class)
class BloqueoDesbloqueoUsuarioControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminController adminController = new AdminController(adminService);

        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .build();
    }

    @Test
    void unAdministradorPuedeSolicitarElBloqueoDeUnUsuario() throws Exception {
        // Act
        mockMvc.perform(post("/api/admin/usuarios/123/bloquear"))
                .andExpect(status().isOk());

        // Assert
        verify(adminService).bloquearUsuario("123");
    }
}