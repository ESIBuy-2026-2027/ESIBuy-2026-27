package es.uclm.esibuy.incidents.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import es.uclm.esibuy.common.exception.IncidentNotFoundException;
import es.uclm.esibuy.incidents.dto.CreateIncidentRequest;
import es.uclm.esibuy.incidents.model.Incident;
import es.uclm.esibuy.incidents.model.IncidentStatus;
import es.uclm.esibuy.incidents.service.IncidentService;
import es.uclm.esibuy.security.config.SecurityConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminIncidentController.class)
@Import(SecurityConfig.class)
class AdminIncidentControllerTest {

        private static final String API = "/api/admin/incidents";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private IncidentService incidentService;

        @Test
        @DisplayName("Admin puede registrar una incidencia correctamente (201 Created)")
        void adminCanCreateIncident() throws Exception {
                Incident incident = new Incident(
                                "inc-123",
                                "Problema con pedido",
                                "PEDIDO",
                                "PED-999",
                                "El envío no llega al destino",
                                IncidentStatus.PENDIENTE,
                                "admin@esibuy.com",
                                Instant.parse("2026-10-28T10:00:00Z"),
                                null);

                when(incidentService.create(any(CreateIncidentRequest.class), any())).thenReturn(incident);

                mockMvc.perform(post(API)
                                .with(user("admin@esibuy.com").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "subject": "Problema con pedido",
                                                    "category": "PEDIDO",
                                                    "reference": "PED-999",
                                                    "description": "El envío no llega al destino"
                                                }
                                                """))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value("inc-123"))
                                .andExpect(jsonPath("$.subject").value("Problema con pedido"))
                                .andExpect(jsonPath("$.category").value("PEDIDO"))
                                .andExpect(jsonPath("$.reference").value("PED-999"))
                                .andExpect(jsonPath("$.status").value("PENDIENTE"))
                                .andExpect(jsonPath("$.createdBy").value("admin@esibuy.com"))
                                .andExpect(jsonPath("$.resolvedAt").doesNotExist());

                verify(incidentService).create(any(CreateIncidentRequest.class), eq("admin@esibuy.com"));
        }

        @Test
        @DisplayName("Error de validación (400) si faltan campos obligatorios como asunto o descripción")
        void createIncident_validationError_returns400() throws Exception {
                mockMvc.perform(post(API)
                                .with(user("admin@esibuy.com").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "subject": "",
                                                    "category": "PEDIDO",
                                                    "description": ""
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Error de validación (400) si la categoría está vacía")
        void createIncident_emptyCategory_returns400() throws Exception {
                mockMvc.perform(post(API)
                                .with(user("admin@esibuy.com").roles("ADMIN"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "subject": "Asunto de prueba",
                                                    "category": "",
                                                    "description": "Descripción válida"
                                                }
                                                """))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("RBAC: Usuario con rol CLIENT recibe 403 Forbidden")
        void clientRole_isForbidden() throws Exception {
                mockMvc.perform(post(API)
                                .with(user("cliente@test.com").roles("CLIENT"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "subject": "Intento de registro",
                                                    "category": "PEDIDO",
                                                    "description": "No tengo permisos"
                                                }
                                                """))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("RBAC: Usuario con rol SELLER recibe 403 Forbidden")
        void sellerRole_isForbidden() throws Exception {
                mockMvc.perform(post(API)
                                .with(user("vendedor@test.com").roles("SELLER"))
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "subject": "Intento de registro",
                                                    "category": "PEDIDO",
                                                    "description": "No tengo permisos"
                                                }
                                                """))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("RBAC: Petición anónima no autenticada recibe 401 Unauthorized")
        void unauthenticated_isUnauthorized() throws Exception {
                mockMvc.perform(post(API)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                    "subject": "Intento anónimo",
                                                    "category": "PEDIDO",
                                                    "description": "Sin autenticar"
                                                }
                                                """))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Admin puede consultar todas las incidencias (200 OK)")
        void adminCanGetAllIncidents() throws Exception {
                Incident i1 = new Incident(
                                "inc-1",
                                "Problema de pago",
                                "PAGO",
                                "PED-101",
                                "Cobro no procesado",
                                IncidentStatus.PENDIENTE,
                                "admin@esibuy.com",
                                Instant.parse("2026-10-28T11:00:00Z"),
                                null);

                Incident i2 = new Incident(
                                "inc-2",
                                "Fallo en catálogo",
                                "PRODUCTO",
                                null,
                                "Imagen no carga",
                                IncidentStatus.RESUELTA,
                                "admin@esibuy.com",
                                Instant.parse("2026-10-28T09:00:00Z"),
                                Instant.parse("2026-10-28T10:00:00Z"));

                when(incidentService.findAll()).thenReturn(List.of(i1, i2));

                mockMvc.perform(get(API).with(user("admin@esibuy.com").roles("ADMIN")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").value("inc-1"))
                                .andExpect(jsonPath("$[0].subject").value("Problema de pago"))
                                .andExpect(jsonPath("$[0].status").value("PENDIENTE"))
                                .andExpect(jsonPath("$[1].id").value("inc-2"))
                                .andExpect(jsonPath("$[1].status").value("RESUELTA"));

                verify(incidentService).findAll();
        }

        @Test
        @DisplayName("RBAC GET: Usuario con rol CLIENT recibe 403 Forbidden")
        void clientRole_cannotGetIncidents() throws Exception {
                mockMvc.perform(get(API).with(user("cliente@test.com").roles("CLIENT")))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("RBAC GET: Usuario con rol SELLER recibe 403 Forbidden")
        void sellerRole_cannotGetIncidents() throws Exception {
                mockMvc.perform(get(API).with(user("vendedor@test.com").roles("SELLER")))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("RBAC GET: Petición anónima recibe 401 Unauthorized")
        void unauthenticated_cannotGetIncidents() throws Exception {
                mockMvc.perform(get(API))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Admin puede marcar una incidencia como resuelta (200 OK)")
        void adminCanResolveIncident() throws Exception {
                Incident resolved = new Incident(
                                "inc-1",
                                "Problema resuelto",
                                "PEDIDO",
                                "PED-123",
                                "Descripción solucionada",
                                IncidentStatus.RESUELTA,
                                "admin@esibuy.com",
                                Instant.parse("2026-10-28T10:00:00Z"),
                                Instant.parse("2026-10-28T10:30:00Z"));

                when(incidentService.resolve("inc-1")).thenReturn(resolved);

                mockMvc.perform(patch(API + "/inc-1/resolve")
                                .with(user("admin@esibuy.com").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value("inc-1"))
                                .andExpect(jsonPath("$.status").value("RESUELTA"))
                                .andExpect(jsonPath("$.resolvedAt").value("2026-10-28T10:30:00Z"));

                verify(incidentService).resolve("inc-1");
        }

        @Test
        @DisplayName("Resolver incidencia devuelve 404 si el ID no existe")
        void resolveIncident_notFound_returns404() throws Exception {
                when(incidentService.resolve("inc-inexistente"))
                                .thenThrow(new IncidentNotFoundException("inc-inexistente"));

                mockMvc.perform(patch(API + "/inc-inexistente/resolve")
                                .with(user("admin@esibuy.com").roles("ADMIN"))
                                .with(csrf()))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("RBAC RESOLVE: Usuario con rol CLIENT recibe 403 Forbidden")
        void clientRole_cannotResolveIncident() throws Exception {
                mockMvc.perform(patch(API + "/inc-1/resolve")
                                .with(user("cliente@test.com").roles("CLIENT"))
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("RBAC RESOLVE: Usuario con rol SELLER recibe 403 Forbidden")
        void sellerRole_cannotResolveIncident() throws Exception {
                mockMvc.perform(patch(API + "/inc-1/resolve")
                                .with(user("vendedor@test.com").roles("SELLER"))
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("RBAC RESOLVE: Petición anónima recibe 401 Unauthorized")
        void unauthenticated_cannotResolveIncident() throws Exception {
                mockMvc.perform(patch(API + "/inc-1/resolve")
                                .with(csrf()))
                                .andExpect(status().isUnauthorized());
        }
}
