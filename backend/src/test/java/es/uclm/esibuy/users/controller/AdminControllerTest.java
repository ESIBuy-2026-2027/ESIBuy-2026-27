package es.uclm.esibuy.users.controller;

import java.time.LocalDateTime;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    private static final String ADMIN_CREATE_API = "/api/admin/create-admin";
    private static final String MESSAGE_JSON_PATH = "$.message";
    private static final String VALIDATION_ERROR_MESSAGE = "La solicitud contiene errores";

    @Mock
    private AdminService adminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AdminController adminController = new AdminController(adminService);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void ComprobarAltaAdministradorValido() throws Exception {
        String json = """
                {
                    "nationalId": "12345678Z",
                    "firstName": "Carlos",
                    "lastName": "Gómez Pérez",
                    "email": "carlos.admin@esibuy.com",
                    "password": "Admin123456!#",
                    "confirmPassword": "Admin123456!#",
                    "headquarters": "Ciudad Real",
                    "profileImage": "https://avatar.ejemplo.com/carlos.png"
                }
                """;

        User admin = new User("carlos.admin@esibuy.com", "Carlos", "Gómez Pérez", UserRole.ADMIN);
        admin.setNationalId("12345678Z");
        admin.setActive(true);
        admin.setProfileImage("https://avatar.ejemplo.com/carlos.png");
        admin.setAdminProfile(new AdminProfile("Ciudad Real", LocalDateTime.now()));

        when(adminService.createAdmin(any(AltaAdminDTO.class))).thenReturn(admin);

        mockMvc.perform(post(ADMIN_CREATE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("carlos.admin@esibuy.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.hireDate").isNotEmpty());
    }

    @Test
    void ComprobarErrorPorEmailDuplicado() throws Exception {
        String json = """
                {
                    "nationalId": "12345678Z",
                    "firstName": "Carlos",
                    "lastName": "Gómez Pérez",
                    "email": "carlos.duplicado@esibuy.com",
                    "password": "Admin123456!#",
                    "confirmPassword": "Admin123456!#",
                    "headquarters": "Ciudad Real",
                    "profileImage": "https://avatar.ejemplo.com/carlos.png"
                }
                """;

        when(adminService.createAdmin(any(AltaAdminDTO.class)))
                .thenThrow(new DuplicateUserException("Ya existe un usuario con ese email"));

        mockMvc.perform(post(ADMIN_CREATE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un usuario con ese email"));
    }

    @Test
    void ComprobarErrorPorFormatoEmailInvalido() throws Exception {
        String json = """
                {
                    "nationalId": "12345678Z",
                    "firstName": "Carlos",
                    "lastName": "Gómez Pérez",
                    "email": "carlos.invalido.sin.arroba",
                    "password": "Admin123456!#",
                    "confirmPassword": "Admin123456!#",
                    "headquarters": "Ciudad Real",
                    "profileImage": "https://avatar.ejemplo.com/carlos.png"
                }
                """;

        mockMvc.perform(post(ADMIN_CREATE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value(VALIDATION_ERROR_MESSAGE))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void ComprobarErrorPorFormatoContrasenaInvalido() throws Exception {
        String json = """
                {
                    "nationalId": "12345678Z",
                    "firstName": "Carlos",
                    "lastName": "Gómez Pérez",
                    "email": "carlos.admin@esibuy.com",
                    "password": "passwordinvalida",
                    "confirmPassword": "passwordinvalida",
                    "headquarters": "Ciudad Real",
                    "profileImage": "https://avatar.ejemplo.com/carlos.png"
                }
                """;

        mockMvc.perform(post(ADMIN_CREATE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value(VALIDATION_ERROR_MESSAGE))
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void ComprobarErrorPorContrasenasNoCoincidentes() throws Exception {
        String json = """
                {
                    "nationalId": "12345678Z",
                    "firstName": "Carlos",
                    "lastName": "Gómez Pérez",
                    "email": "carlos.admin@esibuy.com",
                    "password": "Admin123456!#",
                    "confirmPassword": "Distinta123456!#",
                    "headquarters": "Ciudad Real",
                    "profileImage": "https://avatar.ejemplo.com/carlos.png"
                }
                """;

        mockMvc.perform(post(ADMIN_CREATE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value(VALIDATION_ERROR_MESSAGE))
                .andExpect(jsonPath("$.errors.passwordsMatch").exists());
    }

    @Test
    void ComprobarErrorPorDatosObligatoriosIncompletos() throws Exception {
        String json = "{}";

        mockMvc.perform(post(ADMIN_CREATE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(MESSAGE_JSON_PATH).value(VALIDATION_ERROR_MESSAGE))
                .andExpect(jsonPath("$.errors.nationalId").exists())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.lastName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists());
    }

    @Test
    void ComprobarAltaAdministradorSinCamposOpcionales() throws Exception {
        String json = """
                {
                    "nationalId": "12345678Z",
                    "firstName": "Carlos",
                    "lastName": "Gómez Pérez",
                    "email": "carlos.sinopcionales@esibuy.com",
                    "password": "Admin123456!#",
                    "confirmPassword": "Admin123456!#"
                }
                """;

        User admin = new User("carlos.sinopcionales@esibuy.com", "Carlos", "Gómez Pérez", UserRole.ADMIN);
        admin.setNationalId("12345678Z");
        admin.setActive(true);
        admin.setProfileImage("avatar-admin-default");
        admin.setAdminProfile(new AdminProfile(null, LocalDateTime.now()));

        when(adminService.createAdmin(any(AltaAdminDTO.class))).thenReturn(admin);

        mockMvc.perform(post(ADMIN_CREATE_API)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("carlos.sinopcionales@esibuy.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void shouldUnlockUser() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios/{id}/desbloquear", "user-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(adminService).unblockUser("user-1");
    }

    @Test
    void shouldActivateUser() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios/{id}/activar", "user-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(adminService).activateUser("user-1");
    }

    @Test
    void shouldDeactivateUser() throws Exception {
        mockMvc.perform(post("/api/admin/usuarios/{id}/desactivar", "user-1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        org.mockito.Mockito.verify(adminService).deactivateUser("user-1");
    }
}
