package es.uclm.esibuy.users.controller;

import java.time.LocalDateTime;
import java.util.List;
import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.common.exception.DuplicateUserException;
import es.uclm.esibuy.users.dto.AltaAdminDTO;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        public void ComprobarAltaAdministradorValido() throws Exception {
                String json = """
                                {
                                    "dni": "12345678Z",
                                    "nombre": "Carlos",
                                    "apellidos": "Gómez Pérez",
                                    "email": "carlos.admin@esibuy.com",
                                    "contrasena": "Admin123456!#",
                                    "repetirContrasena": "Admin123456!#",
                                    "sede": "Ciudad Real",
                                    "imagenPerfil": "https://avatar.ejemplo.com/carlos.png"
                                }
                                """;

                User admin = new User("carlos.admin@esibuy.com", "Carlos", "Gómez Pérez", UserRole.ADMIN);
                admin.setNationalId("12345678Z");
                admin.setActive(true);
                admin.setProfileImage("https://avatar.ejemplo.com/carlos.png");
                admin.setAdminProfile(new AdminProfile("Ciudad Real", LocalDateTime.now()));

                when(adminService.crearAdministrador(any(AltaAdminDTO.class))).thenReturn(admin);

                mockMvc.perform(post("/api/admin/alta")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.email").value("carlos.admin@esibuy.com"))
                                .andExpect(jsonPath("$.rol").value("ADMIN"))
                                .andExpect(jsonPath("$.activo").value(true))
                                .andExpect(jsonPath("$.fechaIncorporacion").isNotEmpty());
        }

        @Test
        public void ComprobarErrorPorEmailDuplicado() throws Exception {
                String json = """
                                {
                                    "dni": "12345678Z",
                                    "nombre": "Carlos",
                                    "apellidos": "Gómez Pérez",
                                    "email": "carlos.duplicado@esibuy.com",
                                    "contrasena": "Admin123456!#",
                                    "repetirContrasena": "Admin123456!#",
                                    "sede": "Ciudad Real",
                                    "imagenPerfil": "https://avatar.ejemplo.com/carlos.png"
                                }
                                """;

                when(adminService.crearAdministrador(any(AltaAdminDTO.class)))
                                .thenThrow(new DuplicateUserException("Ya existe un usuario con ese email"));

                mockMvc.perform(post("/api/admin/alta")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.message").value("Ya existe un usuario con ese email"));
        }

        @Test
        public void ComprobarErrorPorFormatoEmailInvalido() throws Exception {
                String json = """
                                {
                                    "dni": "12345678Z",
                                    "nombre": "Carlos",
                                    "apellidos": "Gómez Pérez",
                                    "email": "carlos.invalido.sin.arroba",
                                    "contrasena": "Admin123456!#",
                                    "repetirContrasena": "Admin123456!#",
                                    "sede": "Ciudad Real",
                                    "imagenPerfil": "https://avatar.ejemplo.com/carlos.png"
                                }
                                """;

                mockMvc.perform(post("/api/admin/alta")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("La solicitud contiene errores"))
                                .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        public void ComprobarErrorPorFormatoContrasenaInvalido() throws Exception {
                String json = """
                                {
                                    "dni": "12345678Z",
                                    "nombre": "Carlos",
                                    "apellidos": "Gómez Pérez",
                                    "email": "carlos.admin@esibuy.com",
                                    "contrasena": "passwordinvalida",
                                    "repetirContrasena": "passwordinvalida",
                                    "sede": "Ciudad Real",
                                    "imagenPerfil": "https://avatar.ejemplo.com/carlos.png"
                                }
                                """;

                mockMvc.perform(post("/api/admin/alta")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("La solicitud contiene errores"))
                                .andExpect(jsonPath("$.errors.contrasena").exists());
        }

        @Test
        public void ComprobarErrorPorContrasenasNoCoincidentes() throws Exception {
                String json = """
                                {
                                    "dni": "12345678Z",
                                    "nombre": "Carlos",
                                    "apellidos": "Gómez Pérez",
                                    "email": "carlos.admin@esibuy.com",
                                    "contrasena": "Admin123456!#",
                                    "repetirContrasena": "Distinta123456!#",
                                    "sede": "Ciudad Real",
                                    "imagenPerfil": "https://avatar.ejemplo.com/carlos.png"
                                }
                                """;

                mockMvc.perform(post("/api/admin/alta")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("La solicitud contiene errores"))
                                .andExpect(jsonPath("$.errors.contrasenasCoincidentes").exists());
        }

        @Test
        public void ComprobarErrorPorDatosObligatoriosIncompletos() throws Exception {
                String json = "{}";

                mockMvc.perform(post("/api/admin/alta")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("La solicitud contiene errores"))
                                .andExpect(jsonPath("$.errors.dni").exists())
                                .andExpect(jsonPath("$.errors.nombre").exists())
                                .andExpect(jsonPath("$.errors.apellidos").exists())
                                .andExpect(jsonPath("$.errors.email").exists())
                                .andExpect(jsonPath("$.errors.contrasena").exists());
        }

        @Test
        void shouldReturnTodosLosUsuarios() throws Exception {
                when(adminService.obtenerUsuariosFiltrados(null, null)).thenReturn(List.of());
                mockMvc.perform(get("/api/admin/usuarios")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray());
        }
}
