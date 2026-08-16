package com.plr.aduaja.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    private OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;

    @Autowired
    private com.plr.aduaja.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // ===== PUBLIC ASSETS =====
                        .requestMatchers("/css/**", "/js/**", "/img/**", "/static/**", "/favicon.ico").permitAll()

                        // ===== PUBLIC PAGES =====
                        .requestMatchers("/", "/index", "/layouts/**", "/error").permitAll()

                        // ===== H2 CONSOLE (DEV ONLY) =====
                        .requestMatchers("/h2-console/**").permitAll()

                        // ===== AUTH ENDPOINTS (public) =====
                        .requestMatchers("/admin/login", "/admin/verify-otp", "/admin/change-password", "/petugas/login", "/petugas/change-password", "/warga/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/login", "/admin/verify-otp", "/admin/change-password", "/petugas/login", "/petugas/change-password", "/warga/login").permitAll()
                        .requestMatchers("/warga/register", "/warga/verify-otp").permitAll()
                        .requestMatchers(HttpMethod.POST, "/warga/register", "/warga/verify-otp").permitAll()
                        // FIX SCN-14: Lupa password harus bisa diakses tanpa login
                        .requestMatchers("/warga/forgot-password", "/warga/forgot-password/verify", "/petugas/forgot-password", "/petugas/forgot-password/verify").permitAll()
                        .requestMatchers(HttpMethod.POST, "/warga/forgot-password", "/warga/forgot-password/verify", "/petugas/forgot-password", "/petugas/forgot-password/verify").permitAll()

                        // ===== LOGOUT (permitAll agar bisa POST dari form) =====
                        .requestMatchers(HttpMethod.POST, "/admin/logout", "/petugas/logout", "/warga/logout").permitAll()

                        // ===== REST API (public — akan dibatasi dengan token nanti) =====
                        .requestMatchers("/api/**").permitAll()

                        // ===== OAUTH2 =====
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // ===== ROLE-BASED PROTECTION =====
                        .requestMatchers("/petugas/**").hasRole("PETUGAS")
                        .requestMatchers("/admin/dinas/**").hasAnyRole("ADMIN_DINAS", "ADMIN_PUSAT")
                        .requestMatchers("/admin/**").hasRole("ADMIN_PUSAT")
                        .requestMatchers("/warga/**").hasRole("WARGA")

                        // ===== FALLBACK =====
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/warga/login")
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            String path = request.getRequestURI();
                            if (path.startsWith("/oauth2/") || path.startsWith("/login/oauth2/")) {
                                response.sendRedirect("/warga/login?error=oauth2_failed");
                            } else if (path.startsWith("/petugas/")) {
                                response.sendRedirect("/petugas/login");
                            } else if (path.startsWith("/admin/")) {
                                response.sendRedirect("/admin/login");
                            } else if (path.startsWith("/warga/")) {
                                response.sendRedirect("/warga/login");
                            } else if (path.startsWith("/error")) {
                                response.sendRedirect("/admin/login");
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                )
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .headers(headers -> headers
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                )
                .anonymous(anon -> anon.disable())
                .addFilterBefore(new SessionAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
