package es.uclm.esibuy.users.model;

/**
 * Dirección de envío. Se guarda embebida dentro de {@link ClientProfile}, no en una
 * colección aparte: son pocas, se leen siempre con el cliente y se borran con él.
 */
public class ShippingAddress {

    private String id;
    private String recipientName;
    private String street;
    private String additionalInfo;
    private String postalCode;
    private String city;
    private String province;
    private String phone;
    private boolean defaultAddress;

    protected ShippingAddress() {
    }

    @SuppressWarnings("java:S107")
    public ShippingAddress(String id, String recipientName, String street, String additionalInfo,
            String postalCode, String city, String province, String phone, boolean defaultAddress) {
        this.id = id;
        this.recipientName = recipientName;
        this.street = street;
        this.additionalInfo = additionalInfo;
        this.postalCode = postalCode;
        this.city = city;
        this.province = province;
        this.phone = phone;
        this.defaultAddress = defaultAddress;
    }

    public String getId() {
        return id;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getStreet() {
        return street;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCity() {
        return city;
    }

    public String getProvince() {
        return province;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}
