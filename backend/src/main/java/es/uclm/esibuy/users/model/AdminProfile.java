package es.uclm.esibuy.users.model;

import java.time.LocalDateTime;

public class AdminProfile {

    private String headquarters;
    private LocalDateTime hireDate;

    public AdminProfile() {
        this.hireDate = LocalDateTime.now();
    }

    public AdminProfile(String headquarters) {
        this.headquarters = headquarters;
        this.hireDate = LocalDateTime.now();
    }

    public AdminProfile(String headquarters, LocalDateTime hireDate) {
        this.headquarters = headquarters;
        this.hireDate = hireDate != null ? hireDate : LocalDateTime.now();
    }

    public String getHeadquarters() {
        return headquarters;
    }

    public void setHeadquarters(String headquarters) {
        this.headquarters = headquarters;
    }

    public LocalDateTime getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDateTime hireDate) {
        this.hireDate = hireDate;
    }
}
