package com.esmile.security;

import com.esmile.security.config.MyUserDetailService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.access.intercept.FilterSecurityInterceptor;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.ui.DefaultLoginPageGeneratingFilter;
import org.springframework.security.web.authentication.ui.DefaultLogoutPageGeneratingFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.savedrequest.RequestCacheAwareFilter;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestFilter;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.filter.GenericFilterBean;
import org.springframework.web.filter.OncePerRequestFilter;

import java.security.Principal;

// 可以看到注册的security filter
//@EnableWebSecurity(debug = true)


@SpringBootApplication
public class SecurityAPP {

    public static void main(String[] args) {
        SpringApplication.run(SecurityAPP.class, args);
    }

    {
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken;
        DaoAuthenticationProvider authenticationProvider;
        UserDetailsManager userDetailsManager;
        InMemoryUserDetailsManager inMemoryUserDetailsManager;
        JdbcUserDetailsManager jdbcUserDetailsManager;

        PasswordEncoder passwordEncoder;
        BCryptPasswordEncoder bCryptPasswordEncoder; // suanli
        SCryptPasswordEncoder sCryptPasswordEncoder;
        Argon2PasswordEncoder argon2PasswordEncoder;
    }

    {
        AuthenticationProvider authenticationProvider;
        AuthenticationManager authenticationManager;
        /**
         * for (AuthenticationProvider provider : getProviders()) {
         * 			if (!provider.supports(toTest)) {
         * 				continue;
         *                        }
         */

        Authentication authentication;
        Principal principal;  // java security API
        //     可以自定义authentication，
    }

    {
        // 此接口太简单了？？？
        // 只有getter
        GrantedAuthority grantedAuthority;
        SimpleGrantedAuthority simpleGrantedAuthority;

        Authentication authentication; // getAuthorities
        UserDetails userDetails; // getAuthorities

    }

    // Filter
    {
        // Security filter chain:
        DisableEncodeUrlFilter disableEncodeUrlFilter;
        WebAsyncManagerIntegrationFilter webAsyncManagerIntegrationFilter;
        SecurityContextHolderFilter securityContextHolderFilter;
        HeaderWriterFilter headerWriterFilter;
        CorsFilter corsFilter;
        CsrfFilter csrfFilter;
        LogoutFilter logoutFilter;
        UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter;
        DefaultLoginPageGeneratingFilter defaultLoginPageGeneratingFilter;
        DefaultLogoutPageGeneratingFilter defaultLogoutPageGeneratingFilter;
        BasicAuthenticationFilter basicAuthenticationFilter;
        RequestCacheAwareFilter requestCacheAwareFilter;
        SecurityContextHolderAwareRequestFilter securityContextHolderAwareRequestFilter;
        AnonymousAuthenticationFilter anonymousAuthenticationFilter;
        SessionManagementFilter sessionManagementFilter;
        ExceptionTranslationFilter exceptionTranslationFilter;
        FilterSecurityInterceptor filterSecurityInterceptor;


        FilterChainProxy filterChainProxy;

        GenericFilterBean genericFilterBean;
        // only once
        // doFilterInternal
        OncePerRequestFilter oncePerRequestFilter;
    }

}

