package es.uclm.esibuy.incidents.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("incidents")
public class Incident {

    @Id
    private String id;

    private String subject;
    private String category;
    private String reference;
    private String description;
    private IncidentStatus status;
    private String createdBy;
    private Instant createdAt;
    private Instant resolvedAt;

    protected Incident() {
    }

    public Incident(
            String subject,
            String category,
            String reference,
            String description,
            IncidentStatus status,
            String createdBy,
            Instant createdAt,
            Instant resolvedAt) {
        this.subject = subject;
        this.category = category;
        this.reference = reference;
        this.description = description;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public Incident(
            String id,
            String subject,
            String category,
            String reference,
            String description,
            IncidentStatus status,
            String createdBy,
            Instant createdAt,
            Instant resolvedAt) {
        this(subject, category, reference, description, status, createdBy, createdAt, resolvedAt);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

