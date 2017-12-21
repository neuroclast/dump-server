package com.dump.service.controllers;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AngularController implements ErrorController {

    private static final String PATH = "/error";

    /**
     * Angular request mappings
      */
    @RequestMapping({ "/","/v/**", "/login", "/u/**", "/register", "/cp", "/archive/**", "/404" })
    public String index() {
        return "forward:/index.html";
    }

    @RequestMapping(value = PATH)
    public String error() {
        return "Oops!";
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }
}
