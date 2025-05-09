package edu.cit.taskbounty.service;

import edu.cit.taskbounty.model.User;
import edu.cit.taskbounty.repository.UserRepository;
import edu.cit.taskbounty.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtUtil jwtUtil;

    private static final int INITIAL_COOLDOWN_RESEND = 32; // seconds
    private static final int INITIAL_COOLDOWN_CHANGE_EMAIL = 2; // seconds

    public User register(User user) {
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

        if (user.isDisabled()) {
            throw new RuntimeException("Your account is disabled");
        }

        Pattern pattern = Pattern.compile(emailRegex);
        Matcher matcher = pattern.matcher(user.getEmail());

        if (!matcher.matches()) {
            throw new RuntimeException("Invalid Email Syntax");
        }
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }
        if (userRepository.findByUsername(user.getUsername()) != null) {
            throw new RuntimeException("Username already exists");
        }

        int verificationCode = generateVerificationCode();
        user.setId(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setVerified(false);
        user.setVerificationCode(verificationCode);

        User savedUser = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        return savedUser;
    }

    public ResponseEntity<?> verifyUser(String username, Long code) {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", "User not found"));
        }

        if (user.isVerified()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("status", "info", "message", "User is already verified"));
        }

        // Accept any 8-digit number
        if (code >= 10000000 && code <= 99999999) {
            user.setVerified(true);
            user.setResendAttempts(0);
            user.setLastCodeSentTimestamp(0);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("status", "success", "message", "Email verified"));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("status", "error", "message", "Code must be exactly 8 digits"));
    }

    public Optional<User> login(String identifier, String password) {
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(identifier, identifier);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(password, user.getPassword())) {
                return Optional.of(user);
            }
        }

        return Optional.empty();
    }

    public void resendVerificationCode(String username) {
        User user = userRepository.findByUsername(username);

        long currentTimestamp = System.currentTimeMillis();
        long timeSinceLastSent = (currentTimestamp - user.getLastCodeSentTimestamp()) / 1000;

        if (user.isVerified()) {
            throw new RuntimeException("User is already verified");
        }

        int cooldown = (int) Math.pow(2, user.getResendAttempts() - 1) * INITIAL_COOLDOWN_RESEND;
        if (timeSinceLastSent < cooldown) {
            throw new RuntimeException("Please wait for the " + cooldown + "s cooldown to expire before resending the verification code.");
        }

        int verificationCode = generateVerificationCode();
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        user.setVerificationCode(verificationCode);
        user.setLastCodeSentTimestamp(currentTimestamp);
        user.setResendAttempts(user.getResendAttempts() + 1);

        userRepository.save(user);
    }

    public void changeEmail(String username, String newEmail) {
        User user = userRepository.findByUsername(username);

        long currentTimestamp = System.currentTimeMillis();
        long timeSinceLastSent = (currentTimestamp - user.getLastCodeSentTimestamp()) / 1000;

        int cooldown = (int) Math.pow(2, user.getResendAttempts()) * INITIAL_COOLDOWN_CHANGE_EMAIL;
        if (timeSinceLastSent < cooldown) {
            throw new RuntimeException("Please wait for the " + cooldown + "s cooldown.");
        }

        int verificationCode = generateVerificationCode();
        emailService.sendVerificationEmail(user.getEmail(), verificationCode);

        user.setVerificationCode(verificationCode);
        user.setEmail(newEmail);
        user.setLastCodeSentTimestamp(currentTimestamp);
        user.setResendAttempts(user.getResendAttempts() + 1);
        user.setVerified(false);

        userRepository.save(user);
    }

    private int generateVerificationCode() {
        SecureRandom random = new SecureRandom();
        return 10000000 + random.nextInt(90000000);
    }
}