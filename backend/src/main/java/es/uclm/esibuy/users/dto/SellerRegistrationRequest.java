package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SellerRegistrationRequest extends BaseRegistrationRequest {

    @NotBlank(message = "El nombre comercial es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre comercial debe tener entre 2 y 100 caracteres")
    private String businessName;

    @NotBlank(message = "La categoria por defecto es obligatoria")
    @Size(min = 2, max = 50, message = "La categoria por defecto debe tener entre 2 y 50 caracteres")
    private String defaultCategory;

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }
}
