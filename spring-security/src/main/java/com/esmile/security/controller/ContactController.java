package com.esmile.security.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description TODO
 * @Date 26/6/2023 下午10:11
 * @Created by nisran
 */
@RestController
public class ContactController {

    @GetMapping("contacts")
    public String account(){
        return "contacts";
    }

}
