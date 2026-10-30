package es.uclm.esibuy.orders.dto;

public record CustomerShippingAddressDTO(
        String recipientName,
        String street,
        String additionalInfo,
        String postalCode,
        String city,
        String province,
        String phone) {
}