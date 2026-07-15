package com.certsign.controller;

import com.certsign.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/admin/profile")
    public String profile(Authentication authentication, Model model) {
        var user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        model.addAttribute("currentUser", user);
        return "admin/profile";
    }

    @PostMapping("/admin/profile/password/verify")
    @ResponseBody
    public Map<String, Boolean> verifyCurrentPassword(
            @RequestParam("currentPassword") String currentPassword,
            Authentication authentication
    ) {
        var user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));
        boolean match = passwordEncoder.matches(currentPassword, user.getPasswordHash());
        return Map.of("match", match);
    }

    @PostMapping("/admin/profile/password")
    public String updatePassword(
            @RequestParam("currentPassword") String currentPassword,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("confirmPassword") String confirmPassword,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        var user = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            redirectAttributes.addFlashAttribute("error", "Current password does not match.");
            return "redirect:/admin/profile";
        }

        if (newPassword == null || newPassword.length() < 8 || !newPassword.matches(".*[A-Za-z].*") || !newPassword.matches(".*\\d.*") || !newPassword.matches(".*[^A-Za-z0-9].*")) {
            redirectAttributes.addFlashAttribute("error", "New password must be at least 8 characters long, and contain at least one letter, one number, and one special character.");
            return "redirect:/admin/profile";
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            redirectAttributes.addFlashAttribute("error", "New password cannot be the same as your old password.");
            return "redirect:/admin/profile";
        }

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "New password confirmation does not match.");
            return "redirect:/admin/profile";
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success", "Password updated successfully.");
        return "redirect:/admin/profile";
    }
}
