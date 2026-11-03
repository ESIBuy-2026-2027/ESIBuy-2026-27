package es.uclm.esibuy.incidents.repository;

import es.uclm.esibuy.incidents.model.Incident;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IncidentRepository extends MongoRepository<Incident, String> {

    List<Incident> findAllByOrderByCreatedAtDesc();
}
