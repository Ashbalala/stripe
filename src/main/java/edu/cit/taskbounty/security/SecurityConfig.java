package edu.cit.taskbounty.security;

import edu.cit.taskbounty.util.JwtAuthFilter;
import edu.cit.taskbounty.util.RateLimitingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {


    private final RateLimitingFilter rateLimitingFilter;
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(RateLimitingFilter rateLimitingFilter, JwtAuthFilter jwtAuthFilter) {
        this.rateLimitingFilter = rateLimitingFilter;
        this.jwtAuthFilter = jwtAuthFilter;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // Disable CSRF for stateless API
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Stateless authentication
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/bounty_post/**", "/solutions/**", "/stripe-account/**").authenticated() // Secure specific routes
                        .requestMatchers("/bounty_post/{id}/donate", "/bounty_post/{id}/payment-success", "/bounty_post/{id}/donation-success").permitAll() // Allow Stripe callback
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class) // Use JWT authentication
                .addFilterBefore(rateLimitingFilter, JwtAuthFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
