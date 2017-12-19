package com.dump.service.repositories;

import com.dump.service.objects.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    User findByUsernameIgnoreCase(String username);
    User findById(Integer id);
    User findByUsernameIgnoreCaseAndPassword(String username, String password);
}
