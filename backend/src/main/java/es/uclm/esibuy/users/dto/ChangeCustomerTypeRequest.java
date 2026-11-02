package es.uclm.esibuy.users.dto;

import es.uclm.esibuy.users.model.CustomerType;
import jakarta.validation.constraints.NotNull;

public record ChangeCustomerTypeRequest(
        @NotNull(message = "El tipo de cliente es obligatorio") CustomerType customerType) {
}