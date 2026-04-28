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
                        .requestMatchers(HttpMethod.GET, "/admin/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/users/*/role").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/certificates/*/approve").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/programs").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/activate").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/programs/*/deactivate").hasRole("ADMIN")
                        .requestMatchers("/admin/users/new").hasAnyRole("ADMIN", "USER_MANAGER")
                        .requestMatchers(HttpMethod.POST, "/admin/users").hasAnyRole("ADMIN", "USER_MANAGER")
                        // Allow both ADMIN and SIGNER roles to access certificate admin console
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SIGNER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler((request, response, authentication) -> {
                            boolean isUserManager = authentication.getAuthorities().stream()
                                    .anyMatch(a -> "ROLE_USER_MANAGER".equals(a.getAuthority()));
                            if (isUserManager) {
                                response.sendRedirect("/admin/users/new");
                                return;
                            }
                            response.sendRedirect("/admin/dashboard");
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

