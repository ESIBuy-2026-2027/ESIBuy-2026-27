package es.uclm.esibuy.users.controller;

import java.util.List;
import es.uclm.esibuy.users.dto.UserAdminDTO;
import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.UserNotFoundException;
import es.uclm.esibuy.users.dto.CandidatoDTO;
import es.uclm.esibuy.users.dto.PromoverAdminDTO;
import es.uclm.esibuy.users.model.AdminProfile;
import es.uclm.esibuy.users.model.User;
import es.uclm.esibuy.users.model.UserRole;
import es.uclm.esibuy.users.service.AdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminController adminController = new AdminController(adminService);
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnCandidatosList() throws Exception {
        CandidatoDTO c1 = new CandidatoDTO("1", "Ana", "García", "ana@esibuy.com", "CLIENT", true, "avatar1", null);
        CandidatoDTO c2 = new CandidatoDTO("2", "Carlos", "López", "carlos@esibuy.com", "SELLER", false, "avatar2",
                null);

        when(adminService.obtenerCandidatos()).thenReturn(List.of(c1, c2));

        mockMvc.perform(get("/api/admin/candidatos")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Ana"))
                .andExpect(jsonPath("$[0].email").value("ana@esibuy.com"))
                .andExpect(jsonPath("$[1].nombre").value("Carlos"));
    }

    @Test
    void shouldPromoteUserToAdminSuccessfully() throws Exception {
        String json = """
                {
                    "nombre": "Laura",
                    "apellidos": "Sánchez",
                    "sede": "Ciudad Real",
                    "imagenPerfil": "avatar-laura"
                }
                """;

        User promotedUser = new User("laura@esibuy.com", "Laura", "Sánchez", UserRole.ADMIN);
        promotedUser.setAdminProfile(new AdminProfile("Ciudad Real"));
        promotedUser.setProfileImage("avatar-laura");

        when(adminService.promoverAAdmin(eq("usr-1"), any(PromoverAdminDTO.class))).thenReturn(promotedUser);

        mockMvc.perform(put("/api/admin/promover/usr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laura"))
                .andExpect(jsonPath("$.apellidos").value("Sánchez"))
                .andExpect(jsonPath("$.email").value("laura@esibuy.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.sede").value("Ciudad Real"));
    }

    @Test
    void shouldReturnBadRequestWhenValidationFailsOnPromote() throws Exception {
        String invalidJson = """
                {
                    "nombre": "",
                    "apellidos": "",
                    "sede": "Sede"
                }
                """;

        mockMvc.perform(put("/api/admin/promover/usr-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La solicitud contiene errores"))
                .andExpect(jsonPath("$.errors.nombre").exists())
                .andExpect(jsonPath("$.errors.apellidos").exists());
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        String json = """
                {
                    "nombre": "Laura",
                    "apellidos": "Sánchez",
                    "sede": "Ciudad Real"
                }
                """;

        when(adminService.promoverAAdmin(eq("inexistente"), any(PromoverAdminDTO.class)))
                .thenThrow(new UserNotFoundException("Usuario no encontrado con ID: inexistente"));

        mockMvc.perform(put("/api/admin/promover/inexistente")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Usuario no encontrado"));
    }

    @Test
    void shouldReturnTodosLosUsuarios() throws Exception {
        // Arrange: Simulamos que el servicio devuelve una lista (usaremos nuestro
        // futuro DTO)
        // Esto fallará porque obtenerTodosLosUsuarios() aún no existe en AdminService
        when(adminService.obtenerTodosLosUsuarios()).thenReturn(List.of());

        // Act & Assert: Verificamos que el nuevo endpoint responde con 200 OK y un
        // array
        mockMvc.perform(get("/api/admin/usuarios")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
