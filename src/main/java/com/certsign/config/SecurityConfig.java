// SDLC Phase: Implementation
// Component: SecurityConfig
// Requirements covered: NFR-02, NFR-04
// Description: Configures session-based authentication and route protection
package com.certsign.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/login",
                                "/verify",
                                "/verify/result",
                                "/forgot-password",
                                "/reset-password"
                        ).permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/dashboard").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/users").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/admin/users/*/edit").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/role").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        // Admin-only actions
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/enabled").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/impersonate").hasRole("SUPER_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/programs").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/activate").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/deactivate").hasAnyRole("SUPER_ADMIN", "ADMIN")
                        .requestMatchers("/admin/users/new").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/admin/users").hasAnyRole("SUPER_ADMIN", "ADMIN", "USER_MANAGER")
                        // Approve / reject / bulk — PRINCIPAL or ADMIN
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/bulk-approve").hasAnyRole("SUPER_ADMIN", "ADMIN", "PRINCIPAL")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/approve").hasAnyRole("SUPER_ADMIN", "ADMIN", "PRINCIPAL")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/reject").hasAnyRole("SUPER_ADMIN", "ADMIN", "PRINCIPAL")
                        // Secretary sends certificates to students (with or without principal signature)
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/bulk-send").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/send").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/resend-email").hasAnyRole("SUPER_ADMIN", "ADMIN", "SECRETARY", "PRINCIPAL")
                        // Secretary can issue (access issue form and POST)
                        .requestMatchers(HttpMethod.GET, "/admin/issue").hasAnyRole("SUPER_ADMIN", "ADMIN", "SIGNER", "SECRETARY")
                        .requestMatchers(HttpMethod.POST, "/admin/issue").hasAnyRole("SUPER_ADMIN", "ADMIN", "SIGNER", "SECRETARY")
                        // All authenticated staff can reach admin area
                        .requestMatchers("/admin/**").hasAnyRole("SUPER_ADMIN", "ADMIN", "SIGNER", "PRINCIPAL", "SECRETARY", "USER_MANAGER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isSuperAdminOrAdmin = authentication.getAuthorities().stream()
                                    .anyMatch(a -> "ROLE_SUPER_ADMIN".equals(a.getAuthority())
                                            || "ROLE_ADMIN".equals(a.getAuthority()));
                            if (isSuperAdminOrAdmin) {
                                response.sendRedirect("/admin/dashboard");
                                return;
                            }
                            boolean isUserManager = authentication.getAuthorities().stream()
                                    .anyMatch(a -> "ROLE_USER_MANAGER".equals(a.getAuthority()));
                            if (isUserManager) {
                                response.sendRedirect("/admin/users/new");
                                return;
                            }
                            response.sendRedirect("/admin/certificates");
                        })
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new RequestMatcher() {
                            @Override
                            public boolean matches(HttpServletRequest request) {
                                return "GET".equalsIgnoreCase(request.getMethod())
                                        && "/logout".equals(request.getRequestURI());
                            }
                        })
                        .logoutSuccessUrl("/login")
                )
                ;

        return http.build();
    }
}
