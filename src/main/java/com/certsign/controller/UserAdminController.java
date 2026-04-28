package com.certsign.controller;

import com.certsign.model.UserRole;
import com.certsign.service.MailService;
import com.certsign.service.UserManagementService;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UserAdminController {

    private final UserManagementService userManagementService;
    private final MailService mailService;

    @Value("${app.base-url:http://localhost:8000}")
    private String appBaseUrl;

    public UserAdminController(UserManagementService userManagementService, MailService mailService) {
        this.userManagementService = userManagementService;
        this.mailService = mailService;
    }

    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userManagementService.listUsers());
        model.addAttribute("roles", Arrays.asList(UserRole.values()));
        return "admin/users";
    }

    @GetMapping("/admin/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("roles", Arrays.asList(UserRole.values()));
        return "admin/user-form";
    }

    @PostMapping("/admin/users")
    public String createUser(
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") UserRole role,
            Authentication authentication,
            Model model
    ) {
        try {
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (!isAdmin && role == UserRole.ADMIN) {
                throw new IllegalArgumentException("Only admins can create another admin user.");
            }
            var created = userManagementService.createUser(username, email, role);
            String subject = "Your CertSign account has been created";
            String body = """
                    Hello %s,

                    Your account has been created in CertSign.
                    Username: %s
                    Temporary password: %s

                    Please log in and change your password immediately.
                    Login: %s/login
                    """.formatted(created.user().getUsername(), created.user().getUsername(), created.temporaryPassword(), appBaseUrl);
            boolean sent = mailService.send(created.user().getEmail(), subject, body);
            if (sent) {
                return "redirect:/admin/users?created=1";
            }
            return "redirect:/admin/users?created=1&mailError=1";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("roles", Arrays.asList(UserRole.values()));
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("selectedRole", role);
            return "admin/user-form";
        }
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateRole(@PathVariable("id") Long id, @RequestParam("role") UserRole role) {
        userManagementService.updateUserRole(id, role);
        return "redirect:/admin/users?updated=1";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editUserForm(@PathVariable("id") Long id, Model model) {
        var user = userManagementService.getUser(id);
        model.addAttribute("roles", Arrays.asList(UserRole.values()));
        model.addAttribute("userId", user.getId());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("selectedRole", user.getRole());
        model.addAttribute("error", null);
        return "admin/user-edit";
    }

    @PostMapping("/admin/users/{id}")
    public String updateUser(
            @PathVariable("id") Long id,
            @RequestParam("username") String username,
            @RequestParam("email") String email,
            @RequestParam("role") UserRole role,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            Model model
    ) {
        try {
            userManagementService.updateUser(id, username, email, role, newPassword);
            return "redirect:/admin/users?updated=1";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("roles", Arrays.asList(UserRole.values()));
            model.addAttribute("userId", id);
            model.addAttribute("username", username);
            model.addAttribute("email", email);
            model.addAttribute("selectedRole", role);
            model.addAttribute("error", ex.getMessage());
            return "admin/user-edit";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPasswordSubmit(@RequestParam("email") String email) {
        String token = userManagementService.requestPasswordReset(email);
        if (token != null) {
            String resetLink = appBaseUrl + "/reset-password?token=" + token;
            String body = """
                    You requested a password reset for your CertSign account.

                    Reset password link (valid for 30 minutes):
                    %s

                    If this was not you, please ignore this email.
                    """.formatted(resetLink);
            mailService.send(email, "CertSign password reset", body);
        }
        return "redirect:/forgot-password?sent=1";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam("token") String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("validToken", userManagementService.isValidResetToken(token));
        return "reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPasswordSubmit(
            @RequestParam("token") String token,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model
    ) {
        model.addAttribute("token", token);
        if (!password.equals(confirmPassword)) {
            model.addAttribute("validToken", true);
            model.addAttribute("error", "Passwords do not match.");
            return "reset-password";
        }

        try {
            userManagementService.resetPassword(token, password);
            return "redirect:/login?resetSuccess=1";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("validToken", userManagementService.isValidResetToken(token));
            model.addAttribute("error", ex.getMessage());
            return "reset-password";
        }
    }
}
