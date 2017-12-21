package com.dump.service.controllers;

import com.dump.service.repositories.UserRepository;
import com.dump.service.objects.User;
import com.dump.service.utils.Auth;
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.*;


/**
 * Spring REST controller to handle User management
 */
@CrossOrigin
@RestController
@RequestMapping(path="/api/users")
public class UserAPIController {

    @Autowired
    private Environment env;

    @Autowired
    private Auth authUtil;

    @Autowired
    private UserRepository userRepository;

    @Value("classpath:t.png")
    private Resource res;


    /**
     * Checks to see is a username already exists in the database
     * @param username  Username to check
     * @return  HTTP status of result
     */
    @GetMapping(path="/exists/{username}")
    public @ResponseBody
    ResponseEntity exists(@PathVariable("username") String username) {
        User user = userRepository.findByUsernameIgnoreCase(username);

        if(user == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity(HttpStatus.OK);
    }


    /**
     * Gets avatar image for user
     * @param username  User to retrieve avatar for
     * @return  Image w/appropriate png content type
     */
    @GetMapping(path="/avatar/{username}.png")
    public @ResponseBody
    ResponseEntity avatar(@PathVariable("username") String username) {
        User user = userRepository.findByUsernameIgnoreCase(username);

        if(user == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type","image/png");

        if(user.getAvatar() == null) {
            try {
                // TODO: don't load this every time
                InputStream is = res.getInputStream();
                byte[] byteArr = new byte[4096];
                is.read(byteArr);
                return new ResponseEntity<>(byteArr, headers, HttpStatus.OK);
            }
            catch(Exception e) {
                return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        User justAvatar = new User();
        justAvatar.setAvatar(user.getAvatar());

        return new ResponseEntity<>(justAvatar.getAvatar(), headers, HttpStatus.OK);
    }


    /**
     * Retrieves users profile information
     * @param username  Username to retrieve
     * @return  User object
     */
    @GetMapping(path="/profile")
    public @ResponseBody ResponseEntity profile(@RequestParam("username") String username) {
        User user  = userRepository.findByUsernameIgnoreCase(username);

        // bail if we can't find the user
        if(user == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        // clear password before sending to user
        user.setPassword("");

        return new ResponseEntity<>(user, HttpStatus.OK);
    }


    /**
     * Retrieves users private profile
     * @param headers   HTTP headers for authorization check
     * @return  User object
     */
    @GetMapping(path="/myprofile")
    public @ResponseBody ResponseEntity myProfile(@RequestHeader HttpHeaders headers) {
        User authUser = null;

        try {
            authUser = authUtil.verifyAuthorization(headers);
        }
        catch(Exception e) {
            if(e.getMessage().equalsIgnoreCase("expired")) {
                return new ResponseEntity(HttpStatus.I_AM_A_TEAPOT);
            }
        }

        if(authUser == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(authUser, HttpStatus.OK);
    }


    /**
     * Creates a new user
     * @param user User object to add to database
     * @return HTTP status code of result, and user ID if success
     */
    @PostMapping(path="/add")
    public @ResponseBody ResponseEntity add(@RequestBody User user) {
        // sanity check
        if(user == null) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        // save to DB
        userRepository.save(user);

        // get saved user ID
        user = userRepository.findByUsernameIgnoreCase(user.getUsername());

        return new ResponseEntity<>(user.getId(), HttpStatus.OK);
    }


    /**
     * Retrieves username associated with an ID
     * @param id    ID of user
     * @return  Username string
     */
    @GetMapping(path="/username")
    public @ResponseBody ResponseEntity username(@RequestParam Integer id) {
        User user = userRepository.findById(id);

        if(user == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(user.getUsername(), HttpStatus.OK);
    }


    /**
     * Establishes a login JWT for user
     * @param username  Username
     * @param password  Password
     * @param remember  Remember user?
     * @return  JSON containing JWT on success, Forbidden on failure
     */
    @GetMapping(path="/login")
    public @ResponseBody ResponseEntity login(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) boolean remember
            ) {
        User user =  userRepository.findByUsernameIgnoreCaseAndPassword(username, password);

        if(user == null) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        // generate expiration dates
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        cal.add(Calendar.DATE, remember ? 365 : 3);

        Map<String, Object> claimMap = new HashMap<>();
        claimMap.put("username", user.getUsername());

        String compactJws = Jwts.builder()
                .setClaims(claimMap)
                .setSubject(user.getId().toString())
                .setExpiration(cal.getTime())
                .signWith(SignatureAlgorithm.HS256, env.getProperty("dump.jwt-key"))
                .compact();

        String returnStr = String.format("{\"jwt\":\"%s\"}", compactJws);

        return new ResponseEntity<>(returnStr, HttpStatus.OK);
    }
}
