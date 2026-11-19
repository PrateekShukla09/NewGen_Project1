package com.vaccination.model;

import java.time.LocalDateTime;

/**
 * Represents the in-memory authenticated user session.
 * 
 * Stored in application memory for the duration of the CLI session.
 * Strictly avoids storing plaintext or hashed passwords.
 */
public class Session {

    private int userId;
    private String email;
    private String fullName;
    private Role role;
    private LocalDateTime loginTime;

    public Session() {
        clear();
    }

    public Session(int userId, String email, String fullName, Role role) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.loginTime = LocalDateTime.now();
    }

    public static Session fromUser(User user) {
        if (user == null) {
            return new Session();
        }
        return new Session(user.getUserId(), user.getEmail(), user.getFullName(), user.getRole());
    }

    public void copyFrom(Session other) {
        if (other != null && other.isLoggedIn()) {
            this.userId = other.userId;
            this.email = other.email;
            this.fullName = other.fullName;
            this.role = other.role;
            this.loginTime = other.loginTime;
        } else {
            clear();
        }
    }

    public void login(int userId, String email, String fullName, Role role) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.loginTime = LocalDateTime.now();
    }

    public boolean isLoggedIn() {
        return this.userId > 0;
    }

    public boolean isAdmin() {
        return isLoggedIn() && this.role == Role.ADMIN;
    }

    public boolean isUser() {
        return isLoggedIn() && this.role == Role.USER;
    }

    public void clear() {
        this.userId = 0;
        this.email = null;
        this.fullName = null;
        this.role = null;
        this.loginTime = null;
    }

    public int getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    @Override
    public String toString() {
        if (!isLoggedIn()) {
            return "Session[Unauthenticated]";
        }
        return String.format("Session[%s (%s) | Role: %s | Logged in at: %s]",
                fullName, email, role, loginTime);
    }
}
