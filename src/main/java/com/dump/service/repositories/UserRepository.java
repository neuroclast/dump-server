package com.dump.service.repositories;

import com.dump.service.objects.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Repository for managing Users
 */
@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    /**
     * Finds User by username (case-insensitive)
     * @param username  username to search
     * @return  User object
     */
    User findByUsernameIgnoreCase(String username);


    /**
     * Finds User by ID
     * @param id    ID to search
     * @return  User object
     */
    User findById(Integer id);


    /**
     * Finds User by username (case-insensitive) and password
     * @param username username to search
     * @param password password to search
     * @return User object
     */
    User findByUsernameIgnoreCaseAndPassword(String username, String password);
}
