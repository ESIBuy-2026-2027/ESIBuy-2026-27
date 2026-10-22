package es.uclm.esibuy.users.dto;

import jakarta.validation.constraints.NotNull;

public record EmailOtpSettingsRequest(
        @NotNull Boolean enabled) {
}