package es.uclm.esibuy.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import es.uclm.esibuy.users.model.ShippingAddress;

public record ShippingAddressResponse(
        String id,
        String recipientName,
        String street,
        String additionalInfo,
        String postalCode,
        String city,
        String province,
        String phone,
        @JsonProperty("isDefault") boolean defaultAddress) {

    public static ShippingAddressResponse from(ShippingAddress address) {
        return new ShippingAddressResponse(
                address.getId(),
                address.getRecipientName(),
                address.getStreet(),
                address.getAdditionalInfo(),
                address.getPostalCode(),
                address.getCity(),
                address.getProvince(),
                address.getPhone(),
                address.isDefaultAddress());
    }
}
