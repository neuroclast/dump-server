package com.dump.service.controllers;

import com.dump.service.Enumerations;
import com.dump.service.objects.User;
import com.dump.service.repositories.DumpRepository;
import com.dump.service.repositories.UserRepository;
import com.dump.service.objects.Dump;
import com.dump.service.utils.Auth;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Random;

@CrossOrigin
@RestController
@RequestMapping(path="/api/dumps")
public class DumpAPIController {
    @Autowired
    private DumpRepository dumpRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Auth authUtil;

    @GetMapping(path="/view/{id}")
    public @ResponseBody ResponseEntity view(@PathVariable("id") String id) {
        Dump dump = dumpRepository.findByPublicId(id);

        if (dump == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        dump.setViews(dump.getViews() + 1);
        dumpRepository.save(dump);

        if(dump.getUsername().length() > 0) {
            User user = userRepository.findByUsernameIgnoreCase(dump.getUsername());

            if(user != null) {
                user.setViews(user.getViews() + 1);
                userRepository.save(user);
            }
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            return new ResponseEntity<>(mapper.writeValueAsString(dump), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(path="/add")
    public @ResponseBody ResponseEntity add (
            @RequestHeader HttpHeaders headers,
            @RequestBody Dump dump
    ) {
        // verify user if not Anonymous
        if(!dump.getUsername().equalsIgnoreCase("anonymous")) {
            User authUser = null;

            try {
                authUser = authUtil.verifyAuthorization(headers, true);
            }
            catch(Exception e) {
                if(e.getMessage().equalsIgnoreCase("expired")) {
                    return new ResponseEntity(HttpStatus.I_AM_A_TEAPOT);
                }
            }

            if (authUser == null) {
                return new ResponseEntity(HttpStatus.FORBIDDEN);
            }
        }

        // generate dump public id
        String newId = genPublicId();
        while(dumpRepository.findByPublicId(newId) != null) {
            newId = genPublicId();
        }
        dump.setPublicId(newId);

        // make sure title is <= 250 characters
        if(dump.getTitle().length() > 250) {
            dump.setTitle(dump.getTitle().substring(0, 250));
        }

        dumpRepository.save(dump);

        return new ResponseEntity<>(newId, HttpStatus.OK);
    }

    @GetMapping(path="/user")
    public @ResponseBody ResponseEntity recent(
            @RequestHeader HttpHeaders headers,
            @RequestParam("username") String username,
            @RequestParam("viewAll") boolean viewAll
    ) {
        Dump[] dumps;

        if(viewAll) {
            User authUser = null;

            try {
                authUser = authUtil.verifyAuthorization(headers);
            }
            catch(Exception e) {
                if(e.getMessage().equalsIgnoreCase("expired")) {
                    return new ResponseEntity(HttpStatus.I_AM_A_TEAPOT);
                }
            }

            if (authUser == null || !authUser.getUsername().equalsIgnoreCase(username)) {
                return new ResponseEntity(HttpStatus.FORBIDDEN);
            }

            dumps = dumpRepository.findFirst100ByUsernameIgnoreCaseOrderByIdDesc(username);
        }
        else {
            dumps = dumpRepository.findFirst100ByUsernameIgnoreCaseAndExposureOrderByIdDesc(username, Enumerations.Exposure.PUBLIC);
        }

        return new ResponseEntity<>(dumps, HttpStatus.OK);
    }

    @GetMapping(path="/recent")
    public @ResponseBody ResponseEntity recent(
            @RequestHeader HttpHeaders headers,
            @RequestParam(value = "mine", required = false) boolean mine
    ) {
        if(mine) {
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

            return  new ResponseEntity<>(dumpRepository.findFirst10ByUsernameIgnoreCaseOrderByIdDesc(authUser.getUsername()), HttpStatus.OK);
        }

        return new ResponseEntity<>(dumpRepository.findFirst10ByExposureOrderByIdDesc(Enumerations.Exposure.PUBLIC), HttpStatus.OK);
    }

    @GetMapping(path="/range")
    public @ResponseBody ResponseEntity recent(
            @RequestParam("page") Integer page,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "type", required = false) String type
    ) {
        Sort sorter = new Sort(Sort.Direction.DESC, "id");

        if(type != null) {
            Page<Dump[]> retPage = dumpRepository.findByExposureAndTypeOrderByIdDesc(new PageRequest(page, limit, sorter), Enumerations.Exposure.PUBLIC, type);
            return new ResponseEntity<>(retPage.getContent(), HttpStatus.OK);
        }

        Page<Dump[]> retPage = dumpRepository.findByExposureOrderByIdDesc(new PageRequest(page, limit, sorter), Enumerations.Exposure.PUBLIC);
        return new ResponseEntity<>(retPage.getContent(), HttpStatus.OK);
    }

    private String genPublicId() {
        String pidChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder publicId = new StringBuilder();

        Random rnd = new Random();
        while (publicId.length() < 6) {
            int index = (int) (rnd.nextFloat() * pidChars.length());
            publicId.append(pidChars.charAt(index));
        }

        return publicId.toString();

    }
}