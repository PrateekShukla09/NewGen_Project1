package com.vaccination.service;

import com.vaccination.dao.UserDAO;
import com.vaccination.exception.AppException;
import com.vaccination.exception.AuthenticationException;
import com.vaccination.exception.DatabaseException;
import com.vaccination.exception.ValidationException;
import com.vaccination.model.Role;
import com.vaccination.model.Session;
import com.vaccination.model.User;
import com.vaccination.util.PasswordUtil;
import com.vaccination.util.ValidationUtil;

import java.util.Optional;

/**
 * Service orchestrating citizen registration, authentication,
 * session creation, and secure first-time administrator bootstrap.
 */
public class AuthService {

    private final UserDAO userDAO;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public AuthService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Registers a new public citizen account.
     * Enforces Role.USER automatically — public registration cannot create an ADMIN account.
     */
    public User registerUser(String fullName, String email, String phone, 
                             String password, String confirmPassword) throws AppException {
        // 1. Validation
        ValidationUtil.validateName(fullName, "Full name");
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePhone(phone);
        ValidationUtil.validatePasswordRegistration(password, confirmPassword);

        // 2. Pre-check email existence (also backed by MySQL UNIQUE constraint)
        if (userDAO.emailExists(email)) {
            throw new ValidationException("Email address '" + email + "' is already registered. Please login instead.");
        }

        // 3. Cryptographic salt and hash generation
        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        // 4. Construct and insert entity with Role.USER
        User newUser = new User(fullName.trim(), email.trim().toLowerCase(), phone.trim(), hash, salt, Role.USER);
        return userDAO.insert(newUser);
    }

    /**
     * Authenticates a user and returns an active in-memory Session.
     * Throws generic AuthenticationException on failure to prevent user enumeration.
     */
    public Session login(String email, String password) throws AppException {
        ValidationUtil.validateNotBlank(email, "Email");
        ValidationUtil.validateNotBlank(password, "Password");

        Optional<User> userOpt = userDAO.findByEmail(email.trim());

        if (userOpt.isEmpty()) {
            throw new AuthenticationException("Invalid email or password.");
        }

        User user = userOpt.get();

        // Constant-time password check
        boolean passwordMatches = PasswordUtil.verifyPassword(password, user.getPasswordHash(), user.getPasswordSalt());
        if (!passwordMatches) {
            throw new AuthenticationException("Invalid email or password.");
        }

        // Account status check
        if (!user.isActive()) {
            throw new AuthenticationException("Your account is currently disabled. Please contact the administrator.");
        }

        // Initialize and return session
        return Session.fromUser(user);
    }

    /**
     * Checks if at least one administrator account exists in the database.
     */
    public boolean hasAdminAccount() throws DatabaseException {
        return userDAO.countAdmins() > 0;
    }

    /**
     * Secure bootstrap method: Creates the first system administrator account
     * ONLY if no administrator accounts exist in the database.
     */
    public User bootstrapFirstAdmin(String fullName, String email, String phone, 
                                    String password, String confirmPassword) throws AppException {
        if (userDAO.countAdmins() > 0) {
            throw new AuthenticationException("Administrator account already exists. Bootstrap is disabled.");
        }

        ValidationUtil.validateName(fullName, "Administrator name");
        ValidationUtil.validateEmail(email);
        ValidationUtil.validatePhone(phone);
        ValidationUtil.validatePasswordRegistration(password, confirmPassword);

        if (userDAO.emailExists(email)) {
            throw new ValidationException("Email is already registered.");
        }

        String salt = PasswordUtil.generateSalt();
        String hash = PasswordUtil.hashPassword(password, salt);

        User adminUser = new User(fullName.trim(), email.trim().toLowerCase(), phone.trim(), hash, salt, Role.ADMIN);
        return userDAO.insert(adminUser);
    }
}
