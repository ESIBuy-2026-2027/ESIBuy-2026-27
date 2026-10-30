package es.uclm.esibuy.orders.model;

import org.springframework.data.mongodb.core.mapping.Field;

import es.uclm.esibuy.users.model.ShippingAddress;

public record OrderShippingAddress(
        @Field("direccionId") String shippingAddressId,
        String recipientName,
        String street,
        String additionalInfo,
        String postalCode,
        String city,
        String province,
        String phone) {

    public static OrderShippingAddress from(ShippingAddress address) {
        return new OrderShippingAddress(
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