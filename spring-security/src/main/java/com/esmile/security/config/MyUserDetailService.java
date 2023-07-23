package com.esmile.security.config;

import com.esmile.security.domain.User;
import com.esmile.security.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @Description TODO
 * @Date 10/7/2023
 * @Created by nisran
 */
@Service
public class MyUserDetailService implements UserDetailsService {

    @Autowired
    private UserRepository customerRepository;

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        final Optional<User> customerOptional = customerRepository.findByUserName(name);
        final User user = customerOptional.orElseThrow(() -> new UsernameNotFoundException(""));

        final Collection<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName())).collect(Collectors.toSet());

        return new org.springframework.security.core.userdetails.User(name, user.getPassword(), authorities);
    }
}
