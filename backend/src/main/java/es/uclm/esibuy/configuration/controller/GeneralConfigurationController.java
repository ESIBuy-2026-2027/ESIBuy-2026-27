package es.uclm.esibuy.configuration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uclm.esibuy.configuration.dto.GeneralConfigurationDTO;
import es.uclm.esibuy.configuration.model.GeneralConfiguration;
import es.uclm.esibuy.configuration.service.GeneralConfigurationService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/configuracion")
@PreAuthorize("hasRole('ADMIN')")
public class GeneralConfigurationController {

    private final GeneralConfigurationService configurationService;

    public GeneralConfigurationController(GeneralConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    public ResponseEntity<GeneralConfigurationDTO> getConfiguration() {
        return ResponseEntity.ok(toDto(configurationService.getCurrent()));
    }

    @PutMapping
    public ResponseEntity<GeneralConfigurationDTO> updateConfiguration(
            @Valid @RequestBody GeneralConfigurationDTO request) {
        return ResponseEntity.ok(toDto(configurationService.update(request)));
    }

    private GeneralConfigurationDTO toDto(GeneralConfiguration configuration) {
        return new GeneralConfigurationDTO(
                configuration.getIvaPorcentaje(),
                configuration.getGastosEnvio(),
                configuration.getEnvioGratuitoDesde(),
                configuration.isRegistroHabilitado());
    }
}
