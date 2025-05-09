package edu.cit.taskbounty.repository;

import edu.cit.taskbounty.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    User findByUsername(String username);
    Optional<User> findByUsernameOrEmail(String username, String email);
}
