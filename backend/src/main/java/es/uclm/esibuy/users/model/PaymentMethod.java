package es.uclm.esibuy.users.model;

public class PaymentMethod {

    private String id;
    private PaymentMethodType type;
    private String label;
    private String cardholderName;
    private String cardBrand;
    private String lastFourDigits;
    private Integer expirationMonth;
    private Integer expirationYear;
    private String paypalEmail;
    private String bizumPhone;
    private boolean defaultMethod;

    protected PaymentMethod() {
    }

    private PaymentMethod(
            String id,
            PaymentMethodType type,
            String label,
            boolean defaultMethod) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.defaultMethod = defaultMethod;
    }

    public static PaymentMethod card(
            String id,
            String label,
            String cardholderName,
            String cardBrand,
            String lastFourDigits,
            Integer expirationMonth,
            Integer expirationYear,
            boolean defaultMethod) {

        PaymentMethod method = new PaymentMethod(
                id, PaymentMethodType.CARD, label, defaultMethod);
        method.cardholderName = cardholderName;
        method.cardBrand = cardBrand;
        method.lastFourDigits = lastFourDigits;
        method.expirationMonth = expirationMonth;
        method.expirationYear = expirationYear;
        return method;
    }

    public static PaymentMethod paypal(
            String id,
            String label,
            String paypalEmail,
            boolean defaultMethod) {

        PaymentMethod method = new PaymentMethod(
                id, PaymentMethodType.PAYPAL, label, defaultMethod);
        method.paypalEmail = paypalEmail;
        return method;
    }

    public static PaymentMethod bizum(
            String id,
            String label,
            String bizumPhone,
            boolean defaultMethod) {

        PaymentMethod method = new PaymentMethod(
                id, PaymentMethodType.BIZUM, label, defaultMethod);
        method.bizumPhone = bizumPhone;
        return method;
    }

    public String getId() {
        return id;
    }

    public PaymentMethodType getType() {
        return type;
    }

    public String getLabel() {
        return label;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public String getCardBrand() {
        return cardBrand;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public Integer getExpirationMonth() {
        return expirationMonth;
    }

    public Integer getExpirationYear() {
        return expirationYear;
    }

    public String getPaypalEmail() {
        return paypalEmail;
    }

    public String getBizumPhone() {
        return bizumPhone;
    }

    public boolean isDefaultMethod() {
        return defaultMethod;
    }

    public void setDefaultMethod(boolean defaultMethod) {
        this.defaultMethod = defaultMethod;
    }
}