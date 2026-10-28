package es.uclm.esibuy.incidents.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import es.uclm.esibuy.incidents.dto.CreateIncidentRequest;
import es.uclm.esibuy.incidents.model.Incident;
import es.uclm.esibuy.incidents.model.IncidentStatus;
import es.uclm.esibuy.incidents.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(incidentRepository);
    }

    @Test
    @DisplayName("Debe registrar una incidencia con estado PENDIENTE, fecha actual y autor")
    void createIncident_success() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "  Error en pasarela de pago  ",
                "pago",
                "PED-12345",
                "El cliente reporta cobro duplicado");

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> {
            Incident inc = invocation.getArgument(0);
            inc.setId("inc-test-1");
            return inc;
        });

        Incident created = incidentService.create(request, "admin@esibuy.com");

        assertThat(created).isNotNull();
        assertThat(created.getId()).isEqualTo("inc-test-1");
        assertThat(created.getSubject()).isEqualTo("Error en pasarela de pago");
        assertThat(created.getCategory()).isEqualTo("PAGO");
        assertThat(created.getReference()).isEqualTo("PED-12345");
        assertThat(created.getDescription()).isEqualTo("El cliente reporta cobro duplicado");
        assertThat(created.getStatus()).isEqualTo(IncidentStatus.PENDIENTE);
        assertThat(created.getCreatedBy()).isEqualTo("admin@esibuy.com");
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getResolvedAt()).isNull();

        ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).save(captor.capture());
        Incident saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(IncidentStatus.PENDIENTE);
        assertThat(saved.getSubject()).isEqualTo("Error en pasarela de pago");
    }

    @Test
    @DisplayName("Debe asignar 'admin' como autor si no se proporciona autor")
    void createIncident_defaultAuthor() {
        CreateIncidentRequest request = new CreateIncidentRequest(
                "Fallo en stock",
                "PRODUCTO",
                null,
                "Stock negativo en producto 42");

        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Incident created = incidentService.create(request, "   ");

        assertThat(created.getCreatedBy()).isEqualTo("admin");
    }

    @Test
    @DisplayName("Debe lanzar excepción si el request es nulo")
    void createIncident_nullRequest_throwsException() {
        assertThatThrownBy(() -> incidentService.create(null, "admin@esibuy.com"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("Debe consultar todas las incidencias ordenadas por fecha de creación descendente")
    void findAll_returnsIncidentsOrderedDesc() {
        Incident i1 = new Incident("Asunto 1", "PEDIDO", null, "Desc 1", IncidentStatus.PENDIENTE, "admin", null, null);
        Incident i2 = new Incident("Asunto 2", "PAGO", null, "Desc 2", IncidentStatus.RESUELTA, "admin", null, null);
        when(incidentRepository.findAllByOrderByCreatedAtDesc()).thenReturn(java.util.List.of(i1, i2));

        java.util.List<Incident> results = incidentService.findAll();

        assertThat(results).hasSize(2).containsExactly(i1, i2);
        verify(incidentRepository).findAllByOrderByCreatedAtDesc();
    }
}

