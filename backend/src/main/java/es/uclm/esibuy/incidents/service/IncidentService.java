package es.uclm.esibuy.incidents.service;

import es.uclm.esibuy.common.exception.IncidentNotFoundException;
import es.uclm.esibuy.incidents.dto.CreateIncidentRequest;
import es.uclm.esibuy.incidents.model.Incident;
import es.uclm.esibuy.incidents.model.IncidentStatus;
import es.uclm.esibuy.incidents.repository.IncidentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;

    public IncidentService(IncidentRepository incidentRepository) {
        this.incidentRepository = Objects.requireNonNull(incidentRepository, "incidentRepository must not be null");
    }

    public Incident create(CreateIncidentRequest request, String createdBy) {
        Objects.requireNonNull(request, "request must not be null");
        String author = (createdBy != null && !createdBy.isBlank()) ? createdBy.trim() : "admin";

        Incident incident = new Incident(
                request.getSubject() != null ? request.getSubject().trim() : null,
                request.getCategory() != null ? request.getCategory().trim().toUpperCase() : null,
                request.getReference() != null ? request.getReference().trim() : null,
                request.getDescription() != null ? request.getDescription().trim() : null,
                IncidentStatus.PENDIENTE,
                author,
                Instant.now(),
                null);

        return incidentRepository.save(incident);
    }

    public List<Incident> findAll() {
        return incidentRepository.findAllByOrderByCreatedAtDesc();
    }

    public Incident resolve(String id) {
        Objects.requireNonNull(id, "id must not be null");
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException(id));

        incident.setStatus(IncidentStatus.RESUELTA);
        incident.setResolvedAt(Instant.now());
        return incidentRepository.save(incident);
    }
}

