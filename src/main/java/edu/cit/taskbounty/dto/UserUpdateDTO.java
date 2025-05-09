package edu.cit.taskbounty.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Date;

public class UserUpdateDTO {

    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    private String username;

    @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must be exactly 2 uppercase letters")
    private String countryCode;

    @Past(message = "Birth date must be in the past")
    private Date birthDate;

    public UserUpdateDTO() {
    }

    public UserUpdateDTO(String password, String username, String countryCode, Date birthDate) {
        this.password = password;
        this.username = username;
        this.countryCode = countryCode;
        this.birthDate = birthDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    // Method to check if at least one field is provided
    public boolean hasAtLeastOneField() {
        return password != null || username != null || countryCode != null || birthDate != null;
    }
}