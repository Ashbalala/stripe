package edu.cit.taskbounty.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "users")
public class User {
    @Id
    private String id;
    private String username;
    private String email;
    private String password; // Hash
    private Date birthDate;
    @CreatedDate
    private Date dateCreated;
    private long lastCodeSentTimestamp; // Timestamp of when the last verification code was sent
    private int resendAttempts; // Tracks the number of resend attempts
    private int verificationCode;
    private String countryCode;
    private boolean verified;
    private boolean disabled;
    private String stripeAccountId;

    public User() {
    }

    public User(String id, String username, String email, String password, Date birthDate, Date dateCreated, long lastCodeSentTimestamp, int resendAttempts, int verificationCode, String countryCode, boolean verified, boolean disabled, String stripeAccountId) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.birthDate = birthDate;
        this.dateCreated = dateCreated;
        this.lastCodeSentTimestamp = lastCodeSentTimestamp;
        this.resendAttempts = resendAttempts;
        this.verificationCode = verificationCode;
        this.countryCode = countryCode;
        this.verified = verified;
        this.disabled = disabled;
        this.stripeAccountId = stripeAccountId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public Date getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    public long getLastCodeSentTimestamp() {
        return lastCodeSentTimestamp;
    }

    public void setLastCodeSentTimestamp(long lastCodeSentTimestamp) {
        this.lastCodeSentTimestamp = lastCodeSentTimestamp;
    }

    public int getResendAttempts() {
        return resendAttempts;
    }

    public void setResendAttempts(int resendAttempts) {
        this.resendAttempts = resendAttempts;
    }

    public int getVerificationCode() {
        return verificationCode;
    }

    public void setVerificationCode(int verificationCode) {
        this.verificationCode = verificationCode;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public String getStripeAccountId() {
        return stripeAccountId;
    }

    public void setStripeAccountId(String stripeAccountId) {
        this.stripeAccountId = stripeAccountId;
    }
}
