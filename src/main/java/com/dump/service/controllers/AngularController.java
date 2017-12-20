package com.dump.service.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AngularController {

    /**
     * Angular request mappings
      */
    @RequestMapping({ "/","/v/**", "/login", "/u/**", "/register", "/cp", "/archive" })
    public String index() {
        return "forward:/index.html";
    }
}
