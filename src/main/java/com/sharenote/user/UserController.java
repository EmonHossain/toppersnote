package com.sharenote.user;

import com.sharenote.user.dto.RegisterUserRequest;
import com.sharenote.user.dto.ThemePreferenceResponse;
import com.sharenote.user.dto.UpdateThemePreferenceRequest;
import com.sharenote.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@Tag(name = "User", description = "User registration, profile, and preference endpoints.")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Registers a user and queues verification delivery after the database commit.
    @PostMapping("/register")
    @Operation(summary = "Register a user")
    @ApiResponse(responseCode = "201", description = "User registered and verification queued")
    @SecurityRequirements
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    // Stores or replaces the authenticated user's profile picture.
    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Set my profile picture")
    public ResponseEntity<UserResponse> setupProfilePicture(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.setupProfilePicture(file));
    }

    // Returns the authenticated user's current theme preference.
    @GetMapping("/me/preferences")
    @Operation(summary = "Get my theme preference")
    public ResponseEntity<ThemePreferenceResponse> getThemePreference() {
        return ResponseEntity.ok(userService.getThemePreference());
    }

    // Updates the authenticated user's theme preference.
    @PatchMapping("/me/preferences")
    @Operation(summary = "Update my theme preference")
    public ResponseEntity<ThemePreferenceResponse> updateThemePreference(
            @Valid @RequestBody UpdateThemePreferenceRequest request
    ) {
        return ResponseEntity.ok(userService.updateThemePreference(request));
    }
}
