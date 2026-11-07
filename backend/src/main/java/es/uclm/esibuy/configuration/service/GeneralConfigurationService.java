package es.uclm.esibuy.configuration.service;

import org.springframework.stereotype.Service;

import es.uclm.esibuy.configuration.dto.GeneralConfigurationDTO;
import es.uclm.esibuy.configuration.model.GeneralConfiguration;
import es.uclm.esibuy.configuration.repository.GeneralConfigurationRepository;

@Service
public class GeneralConfigurationService {

    private final GeneralConfigurationRepository configurationRepository;

    public GeneralConfigurationService(GeneralConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public GeneralConfiguration getCurrent() {
        return configurationRepository.findById(GeneralConfiguration.DOCUMENT_ID)
                .orElseGet(() -> configurationRepository.save(new GeneralConfiguration()));
    }

    public GeneralConfiguration update(GeneralConfigurationDTO request) {
        GeneralConfiguration configuration = getCurrent();
        configuration.setIvaPorcentaje(request.ivaPorcentaje());
        configuration.setGastosEnvio(request.gastosEnvio());
        configuration.setEnvioGratuitoDesde(request.envioGratuitoDesde());
        configuration.setRegistroHabilitado(request.registroHabilitado());
        return configurationRepository.save(configuration);
    }

    public boolean isRegistrationEnabled() {
        return getCurrent().isRegistroHabilitado();
    }
}
