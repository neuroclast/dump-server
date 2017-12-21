package com.dump.service.controllers;

import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Spring Controller for handling requests for Angular pages.
 */
@Controller
public class AngularController implements ErrorController {

    private static final String PATH = "/error";

    /**
     * Angular request mappings
     * @return  redirects all pages to angular root
     */
    @RequestMapping({ "/","/v/**", "/login", "/u/**", "/register", "/cp", "/archive/**", "/404" })
    public String index() {
        return "forward:/index.html";
    }

    /**
     * Handler for Spring error page. Should never be visible to user.
     * @return "Oops!"
     */
    @RequestMapping(value = PATH)
    public String error() {
        return "Oops!";
    }

    /**
     * Handler for Spring error page. Should never be visible to user.
     * @return Error path
     */
    @Override
    public String getErrorPath() {
        return PATH;
    }
}
