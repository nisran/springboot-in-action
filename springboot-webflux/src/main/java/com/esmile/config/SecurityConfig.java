package com.esmile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {


    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
        // disable csrf
        httpSecurity.csrf(ServerHttpSecurity.CsrfSpec::disable);

        // permit all requests
        httpSecurity.authorizeExchange(authorize -> {
            // Permit access to actuator health
            authorize.pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
            authorize.pathMatchers("/actuator/health").permitAll();

            // Permit access to Swagger apis

            // others need authentication
            authorize.anyExchange().authenticated();
        });

        return httpSecurity.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails userDetails = User.withDefaultPasswordEncoder()
                .username("user")
                .password("password")
                .roles("USER")
                .build();

        return new InMemoryUserDetailsManager(userDetails);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
