package com.esmile.security.service.impl;

import com.esmile.common.enums.ActiveIndicator;
import com.esmile.security.domain.User;
import com.esmile.security.repository.UserRepository;
import com.esmile.security.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * @Description TODO
 * @Date 10/7/2023
 * @Created by nisran
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void register(User user) {
        final String encodedPass = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPass);
        user.setActiveIndicator(ActiveIndicator.A.name());
        final User save = userRepository.save(user);
    }
}
