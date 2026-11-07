package es.uclm.esibuy.configuration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import es.uclm.esibuy.common.exception.ApiExceptionHandler;
import es.uclm.esibuy.configuration.model.GeneralConfiguration;
import es.uclm.esibuy.configuration.service.GeneralConfigurationService;
import es.uclm.esibuy.security.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GeneralConfigurationController.class)
@Import({ ApiExceptionHandler.class, SecurityConfig.class })
class GeneralConfigurationControllerTest {

    private static final String API = "/api/configuracion";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeneralConfigurationService configurationService;

    @Test
    void allowsAdminToReadAndUpdateConfiguration() throws Exception {
        when(configurationService.getCurrent()).thenReturn(defaultConfiguration());
        when(configurationService.update(any())).thenReturn(new GeneralConfiguration(
                new BigDecimal("22"),
                new BigDecimal("5.50"),
                new BigDecimal("60"),
                false));

        mockMvc.perform(get(API).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ivaPorcentaje").value(21))
                .andExpect(jsonPath("$.registroHabilitado").value(true));

        mockMvc.perform(put(API)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "ivaPorcentaje": 22,
                          "gastosEnvio": 5.50,
                          "envioGratuitoDesde": 60,
                          "registroHabilitado": false
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ivaPorcentaje").value(22))
                .andExpect(jsonPath("$.registroHabilitado").value(false));
    }

    @Test
    void rejectsNonAdminAndAnonymousAccess() throws Exception {
        mockMvc.perform(get(API).with(user("client").roles("CLIENT")))
                .andExpect(status().isForbidden());

        mockMvc.perform(put(API)
                .with(user("seller").roles("SELLER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(API)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidConfigurationValues() throws Exception {
        mockMvc.perform(put(API)
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "ivaPorcentaje": -1,
                          "gastosEnvio": -1,
                          "envioGratuitoDesde": null,
                          "registroHabilitado": true
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.ivaPorcentaje").exists())
                .andExpect(jsonPath("$.errors.gastosEnvio").exists())
                .andExpect(jsonPath("$.errors.envioGratuitoDesde").exists());
    }

    private GeneralConfiguration defaultConfiguration() {
        return new GeneralConfiguration(
                new BigDecimal("21.00"),
                new BigDecimal("4.99"),
                new BigDecimal("50.00"),
                true);
    }
}
