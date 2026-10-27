
package es.uclm.esibuy.auth.passwordreset.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.uclm.esibuy.auth.passwordreset.service.PasswordResetService;
import es.uclm.esibuy.auth.passwordreset.service.PasswordResetService.InvalidPasswordResetTokenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    private static final String BASE_URL = "/api/auth/password-reset";

    private static final String REQUEST_MESSAGE = "Si el correo está registrado, recibirás instrucciones para recuperar tu contraseña.";

    @Mock
    private PasswordResetService passwordResetService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void requestShouldReturnGenericMessageAndDelegateToService()
            throws Exception {

        mockMvc.perform(post(BASE_URL + "/request")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "email": "usuario@example.com"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(REQUEST_MESSAGE));

        verify(passwordResetService).requestReset(
                "usuario@example.com",
                "127.0.0.1");
    }

    @Test
    void validateShouldReturnSuccessWhenTokenIsValid()
            throws Exception {

        mockMvc.perform(post(BASE_URL + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "token": "token-valido"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "El código es válido. Ya puedes establecer una nueva contraseña."));

        verify(passwordResetService).validateResetToken("token-valido");
    }

    @Test
    void validateShouldReturnBadRequestWhenTokenIsInvalid()
            throws Exception {

        doThrow(new InvalidPasswordResetTokenException())
                .when(passwordResetService)
                .validateResetToken("token-invalido");

        mockMvc.perform(post(BASE_URL + "/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "token": "token-invalido"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El token no es válido, ha caducado o ya se ha utilizado."));

        verify(passwordResetService).validateResetToken("token-invalido");
    }

    @Test
    void confirmShouldReturnSuccessWhenPasswordIsUpdated()
            throws Exception {

        mockMvc.perform(post(BASE_URL + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "token": "token-valido",
                            "newPassword": "NuevaPassword123!"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(
                        "La contraseña se ha actualizado correctamente. Ya puedes iniciar sesión."));

        verify(passwordResetService).confirmReset(
                "token-valido",
                "NuevaPassword123!");
    }

    @Test
    void confirmShouldReturnBadRequestWhenTokenIsInvalid()
            throws Exception {

        doThrow(new InvalidPasswordResetTokenException())
                .when(passwordResetService)
                .confirmReset("token-invalido", "NuevaPassword123!");

        mockMvc.perform(post(BASE_URL + "/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                            "token": "token-invalido",
                            "newPassword": "NuevaPassword123!"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "El token no es válido, ha caducado o ya se ha utilizado."));

        verify(passwordResetService).confirmReset(
                "token-invalido",
                "NuevaPassword123!");
    }
}
