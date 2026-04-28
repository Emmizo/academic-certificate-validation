package com.certsign.service;

import com.certsign.dto.CreatedUserResult;
import com.certsign.model.User;
import com.certsign.model.UserRole;
import com.certsign.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private static final int RESET_EXPIRY_MINUTES = 30;
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserManagementService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> listUsers() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional
    public CreatedUserResult createUser(String username, String email, UserRole role) {
        if (isBlank(username)) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email is required.");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role is required.");
        }
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();
        if (userRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username already exists.");
        }
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already exists.");
        }

        String tempPassword = generateTemporaryPassword(12);
        User user = User.builder()
                .username(normalizedUsername)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(tempPassword))
                .role(role)
                .build();
        User saved = userRepository.save(user);
        return new CreatedUserResult(saved, tempPassword);
    }

    @Transactional
    public void updateUserRole(Long userId, UserRole role) {
        if (userId == null || role == null) {
            throw new IllegalArgumentException("User and role are required.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setRole(role);
        userRepository.save(user);
    }

    @Transactional
    public String requestPasswordReset(String email) {
        if (isBlank(email)) {
            return null;
        }
        var userOpt = userRepository.findByEmail(email.trim().toLowerCase());
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes());
        user.setPasswordResetToken(token);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_EXPIRY_MINUTES));
        userRepository.save(user);
        return token;
    }

    public boolean isValidResetToken(String token) {
        return findUserByValidToken(token) != null;
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (isBlank(newPassword) || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }
        User user = findUserByValidToken(token);
        if (user == null) {
            throw new IllegalArgumentException("Reset link is invalid or expired.");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        userRepository.save(user);
    }

    private User findUserByValidToken(String token) {
        if (isBlank(token)) {
            return null;
        }
        var userOpt = userRepository.findByPasswordResetToken(token);
        if (userOpt.isEmpty()) {
            return null;
        }
        User user = userOpt.get();
        LocalDateTime expiry = user.getPasswordResetTokenExpiry();
        if (expiry == null || expiry.isBefore(LocalDateTime.now())) {
            return null;
        }
        return user;
    }

    private String generateTemporaryPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(RANDOM_CHARS.length());
            sb.append(RANDOM_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
