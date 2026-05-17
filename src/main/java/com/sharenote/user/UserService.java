package com.sharenote.user;

import com.sharenote.user.dto.RegisterUserRequest;
import com.sharenote.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException(normalizedEmail);
        }

        User user = new User(
                request.firstName().trim(),
                normalizeOptional(request.middleName()),
                request.lastName().trim(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.institution().trim(),
                request.currentSemesterOrYear().trim(),
                request.phoneNumber().trim(),
                request.country().trim(),
                Set.of(Role.USER)
        );

        return toResponse(userRepository.save(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getLastName(),
                user.getEmail(),
                user.getInstitution(),
                user.getCurrentSemesterOrYear(),
                user.getPhoneNumber(),
                user.getCountry(),
                user.getRoles().stream().map(Enum::name).collect(java.util.stream.Collectors.toSet())
        );
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
