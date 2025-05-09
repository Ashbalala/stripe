package edu.cit.taskbounty.service;

import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Optional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository; // Your existing UserRepository

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Use findById to get user by ID if that's how you're passing the user identifier
        Optional<User> userOptional = Optional.ofNullable(userRepository.findByUsername(username));

        return userOptional
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        // No roles are being used, so return an empty list of authorities
                        Collections.emptyList()
                ))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

}