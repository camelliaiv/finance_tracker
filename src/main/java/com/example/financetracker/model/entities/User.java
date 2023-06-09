package com.example.financetracker.model.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column
    private String email;

    @Column
    private String password;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_verified")
    private boolean isVerified;

    @Column(name = "unique_code")
    private String uniqueCode;

    @Column(name = "validation_exp_date_time")
    private LocalDateTime expirationDate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "sms_2FA_code")
    private String sms2FACode;

    @Column(name = "sms_exp_date_time")
    private LocalDateTime smsExpirationDate;

    @OneToMany
    @JoinColumn(name = "owner_id")
    private Set<Account> accounts = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User data: " + "\n" +
                "Id: " + this.id + "\n" +
                "Email: " + this.email + "\n" +
                "First name: " + this.firstName + "\n" +
                "Last name: " + this.lastName + "\n" +
                "Date of birth: " + this.dateOfBirth + "\n" +
                "Phone number: " + phoneNumber;
    }
}
