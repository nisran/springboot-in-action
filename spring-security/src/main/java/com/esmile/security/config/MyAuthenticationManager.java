package com.esmile.security.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * @Description custom AuthenticationManager
 * @Date 2023/7/17
 * @Created by nisran
 */
@Component
public class MyAuthenticationManager implements AuthenticationManager {

    @Autowired
    private MyUserDetailService myUserDetailService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        final String username = authentication.getName();
        final String passwordFromUI = authentication.getCredentials().toString();

        final UserDetails userDetails = myUserDetailService.loadUserByUsername(username);
        if (Objects.isNull(userDetails))
            throw new UsernameNotFoundException("Username not found");

        final String passwordFromDB = userDetails.getPassword();
        final boolean matches = passwordEncoder.matches(passwordFromUI, passwordFromDB);
        if (!matches) {
            throw new BadCredentialsException("Invalid password");
        }

        return new UsernamePasswordAuthenticationToken(username, passwordFromUI, userDetails.getAuthorities());
    }
}
