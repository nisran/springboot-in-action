package com.esmile.security.controller;

import com.esmile.security.domain.User;
import com.esmile.security.service.UserService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description TODO
 * @Date 10/7/2023
 * @Created by nisran
 */
@RestController("/user")
public class UserController {

    private UserService userService;

    @PostMapping("/register")
    void register(@RequestBody User user){
        userService.register(user);
    }
}
