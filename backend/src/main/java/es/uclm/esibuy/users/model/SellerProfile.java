package es.uclm.esibuy.users.model;

import org.springframework.data.mongodb.core.index.Indexed;

public class SellerProfile {

    @Indexed(unique = true, sparse = true)
    private String businessName;

    private String defaultCategory;

    public SellerProfile() {
    }

    public SellerProfile(String businessName, String defaultCategory) {
        this.businessName = businessName;
        this.defaultCategory = defaultCategory;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(String defaultCategory) {
        this.defaultCategory = defaultCategory;
    }
}
