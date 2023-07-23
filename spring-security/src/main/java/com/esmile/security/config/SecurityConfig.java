package com.esmile.security.config;

import com.esmile.security.filter.AuthLoggingFilter;
import com.esmile.security.filter.CustomFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.filter.CorsFilter;

import javax.sql.DataSource;

import java.util.Collections;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @Description 替代 SpringBootWebSecurityConfiguration 中 defaultSecurityFilterChain方法
 *  默认是所有的方法都进行认证
 * @Date 26/6/2023
 * @Created by nisran
 */

@Configuration
public class SecurityConfig {

    @Bean
    @Order(SecurityProperties.BASIC_AUTH_ORDER)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // replace with custom config
        // http.authorizeHttpRequests().anyRequest().authenticated();

        http.authorizeHttpRequests(requests -> requests
                .requestMatchers("accounts", "balance", "cards").authenticated()
                .requestMatchers("/loans").hasAuthority("USER_CREDIT")
                .requestMatchers("/h2-console").hasAnyAuthority("DBA", "ADMIN")
                .requestMatchers("/user").authenticated()
                .requestMatchers("hello", "notice", "contact").permitAll()
        );
        http.formLogin(withDefaults());
        http.httpBasic(withDefaults());

        // CorsConfig
        http.cors().configurationSource((requests) -> {
            final CorsConfiguration corsConfig = new CorsConfiguration();
            corsConfig.setAllowedOrigins(Collections.singletonList("*"));
            corsConfig.setAllowedMethods(Collections.singletonList("*"));
            corsConfig.setAllowCredentials(true);
            corsConfig.setAllowedHeaders(Collections.singletonList("*"));
            corsConfig.setMaxAge(3600L);
            return corsConfig;
        });


        // CSRF configuration
        final CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        requestHandler.setCsrfRequestAttributeName("_csrf");
        http.csrf(csrf -> csrf.csrfTokenRequestHandler(requestHandler)
                    .ignoringRequestMatchers("/register", "/someGet")
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
        );


        //
        http.addFilterBefore(new CustomFilter(), BasicAuthenticationFilter.class);
        http.addFilterAfter(new AuthLoggingFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }


    //Dev
    // @Order(SecurityProperties.BASIC_AUTH_ORDER)
    // public SecurityFilterChain securityFilterChainDev(HttpSecurity http) throws Exception {
    //     http.authorizeHttpRequests(requests -> requests
    //             .requestMatchers("accounts", "balance", "cards", "loans").authenticated()
    //             .requestMatchers("hello", "notice", "contact").permitAll()
    //     );
    //     // http.authorizeHttpRequests().anyRequest().authenticated();
    //     http.formLogin(withDefaults());
    //     http.httpBasic(withDefaults());
    //     return http.build();
    // }


    // @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(){
        final UserDetails adminUser = User.withUsername("admin")
                .password("pass")
                .authorities("admin")
                .build();
        final UserDetails passerby = User.withUsername("pass")
                .password("pass")
                .authorities("pass")
                .build();
        return new InMemoryUserDetailsManager(adminUser, passerby);
    }

    /**
     * 需要按照默认的数据结构，创建domain以及SQL，不符合定制需求，不如自定义一个
     */
    // @Bean
    // public UserDetailsManager userDetailsManager(DataSource dataSource) {
    //     return new JdbcUserDetailsManager(dataSource);
    // }

    @Bean
    public PasswordEncoder passwordEncoder(){
        // return NoOpPasswordEncoder.getInstance();
        return new BCryptPasswordEncoder();
    }
}
