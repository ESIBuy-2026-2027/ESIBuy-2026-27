package es.uclm.esibuy.configuration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import es.uclm.esibuy.configuration.dto.GeneralConfigurationDTO;
import es.uclm.esibuy.configuration.model.GeneralConfiguration;
import es.uclm.esibuy.configuration.repository.GeneralConfigurationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneralConfigurationServiceTest {

    @Mock
    private GeneralConfigurationRepository configurationRepository;

    private GeneralConfigurationService configurationService;

    @BeforeEach
    void setUp() {
        configurationService = new GeneralConfigurationService(configurationRepository);
    }

    @Test
    void createsAndPersistsDefaultsWhenConfigurationDoesNotExist() {
        when(configurationRepository.findById(GeneralConfiguration.DOCUMENT_ID))
                .thenReturn(Optional.empty());
        when(configurationRepository.save(any(GeneralConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeneralConfiguration configuration = configurationService.getCurrent();

        assertEquals(GeneralConfiguration.DOCUMENT_ID, configuration.getId());
        assertEquals(new BigDecimal("21.00"), configuration.getIvaPorcentaje());
        assertEquals(new BigDecimal("4.99"), configuration.getGastosEnvio());
        assertEquals(new BigDecimal("50.00"), configuration.getEnvioGratuitoDesde());
        assertEquals(true, configuration.isRegistroHabilitado());
        verify(configurationRepository).save(any(GeneralConfiguration.class));
    }

    @Test
    void persistsUpdatedValuesInTheSingletonDocument() {
        GeneralConfiguration existing = new GeneralConfiguration();
        when(configurationRepository.findById(GeneralConfiguration.DOCUMENT_ID))
                .thenReturn(Optional.of(existing));
        when(configurationRepository.save(any(GeneralConfiguration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        GeneralConfiguration updated = configurationService.update(new GeneralConfigurationDTO(
                new BigDecimal("22.00"),
                new BigDecimal("5.50"),
                new BigDecimal("60.00"),
                false));

        assertEquals(GeneralConfiguration.DOCUMENT_ID, updated.getId());
        assertEquals(new BigDecimal("22.00"), updated.getIvaPorcentaje());
        assertEquals(new BigDecimal("5.50"), updated.getGastosEnvio());
        assertEquals(new BigDecimal("60.00"), updated.getEnvioGratuitoDesde());
        assertEquals(false, updated.isRegistroHabilitado());

        ArgumentCaptor<GeneralConfiguration> saved = ArgumentCaptor.forClass(GeneralConfiguration.class);
        verify(configurationRepository).save(saved.capture());
        assertEquals(GeneralConfiguration.DOCUMENT_ID, saved.getValue().getId());
        assertEquals(new BigDecimal("22.00"), saved.getValue().getIvaPorcentaje());
    }

    @Test
    void reloadsConfigurationInsteadOfCachingValues() {
        GeneralConfiguration original = new GeneralConfiguration();
        GeneralConfiguration updated = new GeneralConfiguration(
                new BigDecimal("22.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                true);
        when(configurationRepository.findById(GeneralConfiguration.DOCUMENT_ID))
                .thenReturn(Optional.of(original), Optional.of(updated));

        assertEquals(new BigDecimal("21.00"), configurationService.getCurrent().getIvaPorcentaje());
        assertEquals(new BigDecimal("22.00"), configurationService.getCurrent().getIvaPorcentaje());
    }
}
