package es.uclm.esibuy.wishlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WishlistRequest(
        @NotBlank(message = "El nombre de la lista de deseos es obligatorio")
        @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
        String name
) {
}
