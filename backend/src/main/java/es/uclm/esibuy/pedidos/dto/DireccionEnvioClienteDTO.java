package es.uclm.esibuy.pedidos.dto;

public record DireccionEnvioClienteDTO(
        String recipientName,
        String street,
        String additionalInfo,
        String postalCode,
        String city,
        String province,
        String phone) {
}
