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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Random;

/**
 * Spring REST controller for Dump management
 */
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


    /**
     * Handler for returning Dump data to client
     * @param id    Public ID of Dump to query
     * @param download  TRUE causes server to generate a download MIME type
     * @return  HTTP Response entity on error, Dump object on success, Dump contents on success if download=TRUE
     */
    @GetMapping(path="/view/{id}")
    public @ResponseBody ResponseEntity view(
            @PathVariable("id") String id,
            @RequestParam(value = "download", required = false) Boolean download
    ) {
        // TODO: add protection for private dumps
        Dump dump = dumpRepository.findByPublicId(id);

        if (dump == null) {
            return new ResponseEntity(HttpStatus.NOT_FOUND);
        }

        // delete and return 404 if dump exists but is expired
        if(dump.getExpiration().before(new Date()) && dump.getExpiration().after(new Date(3600))) {
            dumpRepository.delete(dump);
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

        if(download != null && download) {
            // TODO: add content types
            String cd = String.format("attachment; filename=\"%s.txt\"", dump.getPublicId());

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add("Content-Type", "text/plain");
            headers.add("Content-disposition", cd);

            return new ResponseEntity<>(dump.getContents(), headers, HttpStatus.OK);
        }

        ObjectMapper mapper = new ObjectMapper();

        try {
            return new ResponseEntity<>(mapper.writeValueAsString(dump), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    /**
     * Deletes a Dump from the database
     * @param headers   HTTP headers for authorization check
     * @param publicId  Dump public ID to delete
     * @return  HTTP status code of result
     */
    @DeleteMapping(path="/delete")
    public @ResponseBody ResponseEntity delete (
            @RequestHeader HttpHeaders headers,
            @RequestParam("publicId") String publicId
    ) {
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

        // acquire target
        Dump target = dumpRepository.findByPublicId(publicId);
        if(target == null) {
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        // verify dump is owned by deleting user
        if(!target.getUsername().equalsIgnoreCase(authUser.getUsername())) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        dumpRepository.delete(target);

        return new ResponseEntity(HttpStatus.OK);
    }


    /**
     * Updates existing Dump
     * @param headers   HTTP headers for authorization check
     * @param dump      Dump to update
     * @return  HTTP status code of result
     */
    @PostMapping(path="/update")
    public @ResponseBody ResponseEntity update (
            @RequestHeader HttpHeaders headers,
            @RequestBody Dump dump
    ) {
        User authUser = null;

        try {
            authUser = authUtil.verifyAuthorization(headers);
        }
        catch(Exception e) {
            if(e.getMessage().equalsIgnoreCase("expired")) {
                return new ResponseEntity(HttpStatus.I_AM_A_TEAPOT);
            }
        }

        // verify user
        if (authUser == null || !authUser.getUsername().equalsIgnoreCase(dump.getUsername())) {
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }

        // make sure title is <= 250 characters
        if(dump.getTitle().length() > 250) {
            dump.setTitle(dump.getTitle().substring(0, 250));
        }

        dumpRepository.save(dump);

        return new ResponseEntity<>(dump.getPublicId(), HttpStatus.OK);
    }


    /**
     * Add new Dump to database
     * @param headers   HTTP headers for authorization check
     * @param dump      Dump to add
     * @return HTTP status code of result
     */
    @PostMapping(path="/add")
    public @ResponseBody ResponseEntity add (
            @RequestHeader HttpHeaders headers,
            @RequestBody Dump dump
    ) {
        // verify user if not Anonymous
        if(!dump.getUsername().equalsIgnoreCase("anonymous")) {
            User authUser = null;

            try {
                authUser = authUtil.verifyAuthorization(headers);
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


    /**
     * Retrieves all Dumps associated with a user
     * @param headers   HTTP headers for authorization check
     * @param username  username to query
     * @param viewAll   TRUE returns all Dumps, FALSE only returns PUBLIC Dumps
     * @return  Array of Dumps
     */
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


    /**
     * Retrieves list of most recent new Dumps committed to DB
     * @param headers   HTTP headers for authorization check
     * @param mine      TRUE if we are asking for users own Dumps
     * @return  Array of Dumps
     */
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


    /**
     * Retrieves a range of Dumps from the database in pageable format
     * @param page  Page to return to user
     * @param limit Number of items per page
     * @param type  Exposure to retrieve (Public, Private, Unlisted)
     * @return  Array of Dumps
     */
    @GetMapping(path="/range")
    public @ResponseBody ResponseEntity recent(
            @RequestParam("page") Integer page,
            @RequestParam("limit") Integer limit,
            @RequestParam(value = "type", required = false) String type
    ) {
        // ensure limit is not over 20
        limit = Math.max(limit, 20);

        Sort sorter = new Sort(Sort.Direction.DESC, "id");

        if(type != null) {
            Page<Dump[]> retPage = dumpRepository.findByExposureAndTypeOrderByIdDesc(new PageRequest(page, limit, sorter), Enumerations.Exposure.PUBLIC, type);
            return new ResponseEntity<>(retPage.getContent(), HttpStatus.OK);
        }

        Page<Dump[]> retPage = dumpRepository.findByExposureOrderByIdDesc(new PageRequest(page, limit, sorter), Enumerations.Exposure.PUBLIC);
        return new ResponseEntity<>(retPage.getContent(), HttpStatus.OK);
    }


    /**
     * Generates a public ID string to associate with a Dump
     * @return  Generated public ID
     */
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