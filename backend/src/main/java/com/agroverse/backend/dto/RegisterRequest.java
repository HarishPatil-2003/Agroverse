package com.agroverse.backend.dto;

public class RegisterRequest {
    private String firstName;
    private String lastName;
    private String name; // Legacy support
    private String email;
    private String password;
    private String phone;
    private String location;
    private String address;
    private String farmType;
    private String landHolding;
    private String preferredLanguage;
    private String role;

    public RegisterRequest() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getFarmType() { return farmType; }
    public void setFarmType(String farmType) { this.farmType = farmType; }
    public String getLandHolding() { return landHolding; }
    public void setLandHolding(String landHolding) { this.landHolding = landHolding; }
    public String getPreferredLanguage() { return preferredLanguage; }
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
