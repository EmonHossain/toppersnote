package com.sharenote.user;

import com.sharenote.user.dto.RegisterUserRequest;
import com.sharenote.user.dto.ThemePreferenceResponse;
import com.sharenote.user.dto.UpdateThemePreferenceRequest;
import com.sharenote.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    @PostMapping(value = "/me/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResponse> setupProfilePicture(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(userService.setupProfilePicture(file));
    }

    @GetMapping("/me/preferences")
    public ResponseEntity<ThemePreferenceResponse> getThemePreference() {
        return ResponseEntity.ok(userService.getThemePreference());
    }

    @PatchMapping("/me/preferences")
    public ResponseEntity<ThemePreferenceResponse> updateThemePreference(
            @Valid @RequestBody UpdateThemePreferenceRequest request
    ) {
        return ResponseEntity.ok(userService.updateThemePreference(request));
    }
}
