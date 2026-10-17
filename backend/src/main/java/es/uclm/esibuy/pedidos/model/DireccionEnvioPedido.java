package es.uclm.esibuy.pedidos.model;

import es.uclm.esibuy.users.model.ShippingAddress;

public record DireccionEnvioPedido(
        String direccionId,
        String recipientName,
        String street,
        String additionalInfo,
        String postalCode,
        String city,
        String province,
        String phone) {

    public static DireccionEnvioPedido from(ShippingAddress address) {
        return new DireccionEnvioPedido(
                address.getId(),
                address.getRecipientName(),
                address.getStreet(),
                address.getAdditionalInfo(),
                address.getPostalCode(),
                address.getCity(),
                address.getProvince(),
                address.getPhone());
    }
}
