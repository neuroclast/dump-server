package com.dump.service.utils;

import com.dump.service.objects.User;
import com.dump.service.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.management.OperationsException;
import java.util.Date;


/**
 * Logic to aid in verification of user authorization
 */
@Service
public class Auth  {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private Environment env;

    /**
     * Verifies a users authorization headers
     * @param headers   HTTP headers containing JWT
     * @return  User object on success, null on failure
     * @throws Exception    Throws "exception" if session is expired
     */
    public User verifyAuthorization(HttpHeaders headers) throws Exception {
        return verifyAuthorization(headers, false);
    }


    /**
     * Verifies a users authorization headers
     * @param headers   HTTP headers containing JWT
     * @param preservePassword  If TRUE, User password will be returned with the authorization.
     * @return  User object on success, null on failure
     * @throws Exception    Throws "exception" if session is expired
     */
    public User verifyAuthorization(HttpHeaders headers, boolean preservePassword) throws Exception {
        // sanity check
        if(!headers.containsKey("authorization")) {
            return null;
        }

        // should only be one...
        String authStr = headers.get("authorization").get(0);

        // another sanity check...
        if(authStr.length() < 20) {
            return null;
        }

        // crop off "Bearer "
        authStr = authStr.substring(7);

        try {
            Jws<Claims> claims = Jwts.parser().setSigningKey(env.getProperty("dump.jwt-key")).parseClaimsJws(authStr);

            Integer userId = Integer.valueOf(claims.getBody().getSubject());

            User user  = userRepository.findById(userId);

            // bail if we can't find the user
            if(user == null) {
                return null;
            }

            // create copy of User object
            User retUser = new User(user);

            // clear password before sending to user if necessary
            if(!preservePassword) {
                retUser.setPassword(null);
            }

            return retUser;

        } catch (ExpiredJwtException e) {
            throw new Exception("expired");
        } catch (Exception e) {
            return null;
        }
    }

}
