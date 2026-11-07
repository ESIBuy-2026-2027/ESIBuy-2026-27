package es.uclm.esibuy.configuration.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import es.uclm.esibuy.configuration.model.GeneralConfiguration;

public interface GeneralConfigurationRepository extends MongoRepository<GeneralConfiguration, String> {
}
