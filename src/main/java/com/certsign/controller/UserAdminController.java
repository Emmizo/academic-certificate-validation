package com.certsign.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Map;

import com.certsign.model.UserRole;
import com.certsign.service.MailService;
import com.certsign.service.UserManagementService;

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
    public String listUsers(Authentication authentication, Model model) {
        model.addAttribute("users", userManagementService.listUsers());
        model.addAttribute("roles", availableRoles(authentication));
        model.addAttribute("canImpersonate", isAdmin(authentication));
        model.addAttribute("currentUsername", authentication.getName());
        return "admin/users";
    }

    @GetMapping("/admin/users/new")
    public String newUserForm(Authentication authentication, Model model) {
        model.addAttribute("roles", availableRoles(authentication));
        return "admin/user-form";
    }

    @PostMapping("/admin/users")
    public String createUser(
            @RequestParam("username") String username,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("role") UserRole role,
            Authentication authentication,
            Model model
    ) {
        try {
            boolean isSuperAdmin = isSuperAdmin(authentication);
            boolean isAdmin = isAdmin(authentication);
            if (!isAdmin && (role == UserRole.ADMIN || role == UserRole.SUPER_ADMIN)) {
                throw new IllegalArgumentException("Only admins can create admin users.");
            }
            if (!isSuperAdmin && role == UserRole.SUPER_ADMIN) {
                throw new IllegalArgumentException("Only the super admin can create another super admin user.");
            }
            var created = userManagementService.createUser(username, fullName, email, role);
            String subject = "IPRC Tumba College — Your portal account has been created";
            String body = """
                    Hello %s,

                    Your account has been created in the IPRC Tumba College certificate portal.
                    Username: %s
                    Temporary password: %s

                    Please log in and change your password immediately.
                    Login: %s/login

                    Regards,
                    IPRC Tumba College IT Staff
                    """.formatted(created.user().getUsername(), created.user().getUsername(), created.temporaryPassword(), appBaseUrl);
            boolean sent = mailService.send(created.user().getEmail(), subject, body);
            if (sent) {
                return "redirect:/admin/users?created=1";
            }
            return "redirect:/admin/users?created=1&mailError=1";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("roles", availableRoles(authentication));
            model.addAttribute("username", username);
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("selectedRole", role);
            return "admin/user-form";
        }
    }

    @PostMapping("/admin/users/{id}/role")
    public String updateRole(@PathVariable("id") Long id, @RequestParam("role") UserRole role, Authentication authentication) {
        assertRoleAllowed(role, authentication);
        userManagementService.updateUserRole(id, role);
        return "redirect:/admin/users?updated=1";
    }

    @PostMapping("/admin/users/{id}/enabled")
    public String setEnabled(@PathVariable("id") Long id,
                             @RequestParam("enabled") boolean enabled,
                             Authentication authentication) {
        var target = userManagementService.getUser(id);
        if (!enabled && target.getUsername().equals(authentication.getName())) {
            return "redirect:/admin/users?statusError=self";
        }
        if (target.getRole() == UserRole.SUPER_ADMIN && !isSuperAdmin(authentication)) {
            return "redirect:/admin/users?statusError=superAdmin";
        }
        userManagementService.setEnabled(id, enabled);
        boolean mailSent = sendStatusChangeEmail(target.getEmail(), target.getUsername(), enabled);
        return "redirect:/admin/users?statusUpdated=" + (enabled ? "active" : "inactive")
                + (mailSent ? "" : "&statusMailError=1");
    }

    @PostMapping("/admin/users/{id}/delete")
    public String deleteUser(@PathVariable("id") Long id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        var target = userManagementService.getUser(id);
        if (target.getUsername().equals(authentication.getName())) {
            redirectAttributes.addFlashAttribute("error", "You cannot delete your own account while signed in.");
            return "redirect:/admin/users";
        }
        if (target.getRole() == UserRole.SUPER_ADMIN && !isSuperAdmin(authentication)) {
            redirectAttributes.addFlashAttribute("error", "Only the super admin can delete the super admin account.");
            return "redirect:/admin/users";
        }
        try {
            userManagementService.deleteUser(id);
            redirectAttributes.addFlashAttribute("success", "User " + target.getUsername() + " deleted successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/admin/users/{id}/edit")
    public String editUserForm(@PathVariable("id") Long id, Authentication authentication, Model model) {
        var user = userManagementService.getUser(id);
        model.addAttribute("roles", availableRoles(authentication));
        model.addAttribute("userId", user.getId());
        model.addAttribute("username", user.getUsername());
        model.addAttribute("fullName", user.getFullName());
        model.addAttribute("email", user.getEmail());
        model.addAttribute("selectedRole", user.getRole());
        model.addAttribute("error", null);
        return "admin/user-edit";
    }

    @PostMapping("/admin/users/{id}")
    public String updateUser(
            @PathVariable("id") Long id,
            @RequestParam("username") String username,
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("role") UserRole role,
            @RequestParam(value = "newPassword", required = false) String newPassword,
            Authentication authentication,
            Model model
    ) {
        try {
            assertRoleAllowed(role, authentication);
            userManagementService.updateUser(id, username, fullName, email, role, newPassword);
            return "redirect:/admin/users?updated=1";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("roles", availableRoles(authentication));
            model.addAttribute("userId", id);
            model.addAttribute("username", username);
            model.addAttribute("fullName", fullName);
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
                    You requested a password reset for your IPRC Tumba College portal account.

                    Reset password link (valid for 3 minutes):
                    %s

                    If this was not you, please ignore this email.

                    Regards,
                    IPRC Tumba College IT Staff
                    """.formatted(resetLink);
            mailService.send(email, "IPRC Tumba College portal — password reset", body);
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

    @PostMapping("/reset-password/verify")
    @ResponseBody
    public Map<String, Boolean> verifyNewPasswordDifferent(
            @RequestParam("token") String token,
            @RequestParam("newPassword") String newPassword
    ) {
        boolean isSame = userManagementService.isSameAsOldPassword(token, newPassword);
        return Map.of("sameAsOld", isSame);
    }

    private List<UserRole> availableRoles(Authentication authentication) {
        return List.of(
                UserRole.ADMIN,
                UserRole.SECRETARY,
                UserRole.PRINCIPAL
        );
    }

    private void assertRoleAllowed(UserRole role, Authentication authentication) {
        if (role == UserRole.SUPER_ADMIN && !isSuperAdmin(authentication)) {
            throw new IllegalArgumentException("Only the super admin can assign the super admin role.");
        }
    }

    private boolean isSuperAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                        || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private boolean sendStatusChangeEmail(String email, String username, boolean enabled) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String subject = enabled
                ? "IPRC Tumba College — Your portal account has been activated"
                : "IPRC Tumba College — Your portal account has been deactivated";
        String body = enabled
                ? """
                Hello %s,

                Your IPRC Tumba College certificate portal account has been activated.
                You can now log in again.

                Login: %s/login

                Regards,
                IPRC Tumba College IT Staff
                """.formatted(username, appBaseUrl)
                : """
                Hello %s,

                Your IPRC Tumba College certificate portal account has been set inactive.
                You are no longer able to log in unless an administrator activates the account again.

                Regards,
                IPRC Tumba College IT Staff
                """.formatted(username);
        return mailService.send(email, subject, body);
    }
}
