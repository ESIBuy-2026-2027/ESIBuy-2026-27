package es.uclm.esibuy.configuration.model;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("configuracion-general")
public class GeneralConfiguration {

    public static final String DOCUMENT_ID = "general";

    @Id
    private String id = DOCUMENT_ID;

    private BigDecimal ivaPorcentaje = new BigDecimal("21.00");
    private BigDecimal gastosEnvio = new BigDecimal("4.99");
    private BigDecimal envioGratuitoDesde = new BigDecimal("50.00");
    private boolean registroHabilitado = true;

    public GeneralConfiguration() {
    }

    public GeneralConfiguration(
            BigDecimal ivaPorcentaje,
            BigDecimal gastosEnvio,
            BigDecimal envioGratuitoDesde,
            boolean registroHabilitado) {
        this.ivaPorcentaje = ivaPorcentaje;
        this.gastosEnvio = gastosEnvio;
        this.envioGratuitoDesde = envioGratuitoDesde;
        this.registroHabilitado = registroHabilitado;
    }

    public String getId() {
        return id;
    }

    public BigDecimal getIvaPorcentaje() {
        return ivaPorcentaje;
    }

    public void setIvaPorcentaje(BigDecimal ivaPorcentaje) {
        this.ivaPorcentaje = ivaPorcentaje;
    }

    public BigDecimal getGastosEnvio() {
        return gastosEnvio;
    }

    public void setGastosEnvio(BigDecimal gastosEnvio) {
        this.gastosEnvio = gastosEnvio;
    }

    public BigDecimal getEnvioGratuitoDesde() {
        return envioGratuitoDesde;
    }

    public void setEnvioGratuitoDesde(BigDecimal envioGratuitoDesde) {
        this.envioGratuitoDesde = envioGratuitoDesde;
    }

    public boolean isRegistroHabilitado() {
        return registroHabilitado;
    }

    public void setRegistroHabilitado(boolean registroHabilitado) {
        this.registroHabilitado = registroHabilitado;
    }
}
