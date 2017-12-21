package com.dump.service.controllers;

import com.dump.service.repositories.UserRepository;
import com.dump.service.objects.User;
import com.dump.service.utils.Auth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Spring REST controller for handling uploads
 */
@CrossOrigin
@RestController
@RequestMapping(path="/api/upload")
public class UploadController {

    @Autowired
    private Auth authUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Environment env;


    /**
     * Handles profile uploads by user
     * @param headers   HTTP headers for authorization check
     * @param avatar    User avatar image
     * @param email     User email address
     * @param website   User website
     * @param password  User password
     * @return  HTTP status code of result and User object if successful
     */
    @PostMapping(path="/profile")
    public @ResponseBody
    ResponseEntity avatar (
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "avatar", required = false) MultipartFile avatar,
            @RequestParam(value = "email") String email,
            @RequestParam(value = "website") String website,
            @RequestParam(value = "password", required = false) String password
    ) {
        User authUser = null;

        try {
            authUser = authUtil.verifyAuthorization(headers, true);
        }
        catch(Exception e) {
            if(e.getMessage().equalsIgnoreCase("expired")) {
                return new ResponseEntity(HttpStatus.I_AM_A_TEAPOT);
            }
        }

        if(authUser == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        // store avatar if uploaded
        if(avatar != null) {
            try {
                // convert uploaded image to BufferedImages
                ByteArrayInputStream bais = new ByteArrayInputStream(avatar.getBytes());
                BufferedImage bi = ImageIO.read(bais);

                // resize to 150x150
                BufferedImage bo = new BufferedImage(150, 150, bi.getType());
                Graphics2D g2d = bo.createGraphics();
                g2d.drawImage(bi, 0, 0, 150, 150, null);
                g2d.dispose();

                // save to ByteArrayOutputStream
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bo, "png", baos);

                // save to user
                authUser.setAvatar(baos.toByteArray());
            }
            catch(Exception e) {
                // TODO: something meaningful here
            }
        }

        // store email
        authUser.setEmail(email);

        // store website
        authUser.setWebsite(website);

        // store password
        if(password != null) {
            authUser.setPassword(password);
        }

        userRepository.save(authUser);

        // return new info to user
        return new ResponseEntity<User>(authUser, HttpStatus.OK);
    }
}
