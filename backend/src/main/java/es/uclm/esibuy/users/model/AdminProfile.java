package es.uclm.esibuy.users.model;

import java.time.Instant;

public class AdminProfile {

    private String headquarters;
    private Instant hireDate;

    public AdminProfile() {
        this.hireDate = Instant.now();
    }

    public AdminProfile(String headquarters) {
        this.headquarters = headquarters;
        this.hireDate = Instant.now();
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = headquarters;
    }

    public Instant getHireDate() {
        return hireDate;
    }

    public void setHireDate(Instant hireDate) {
        this.hireDate = hireDate;
    }
}
