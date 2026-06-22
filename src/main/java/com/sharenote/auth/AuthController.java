package com.sharenote.auth;

import com.sharenote.auth.dto.AuthResponse;
import com.sharenote.auth.dto.LoginRequest;
import com.sharenote.auth.dto.RefreshTokenRequest;
import com.sharenote.verification.EmailVerificationService;
import com.sharenote.verification.dto.EmailVerificationResponse;
import com.sharenote.verification.dto.ResendEmailVerificationRequest;
import com.sharenote.verification.dto.VerifyEmailRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Public stateless authentication and email verification endpoints.")
@SecurityRequirements
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    // Authenticates credentials and returns stateless access and refresh tokens.
    @PostMapping("/login")
    @Operation(summary = "Log in")
    @ApiResponse(responseCode = "200", description = "Authentication succeeded")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // Rotates a valid refresh token and returns a new token pair.
    @PostMapping("/refresh")
    @Operation(summary = "Refresh authentication tokens")
    @ApiResponse(responseCode = "200", description = "Refresh token rotated")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    // Verifies an email address using a token supplied in a JSON request.
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with request body")
    @ApiResponse(responseCode = "200", description = "Email verified")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(emailVerificationService.verify(request));
    }

    // Verifies an email address when the user follows the link from an email.
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email from token link")
    @ApiResponse(responseCode = "200", description = "Email verified")
    public ResponseEntity<EmailVerificationResponse> verifyEmailFromLink(@RequestParam("token") String token) {
        return ResponseEntity.ok(emailVerificationService.verify(new VerifyEmailRequest(token)));
    }

    // Queues a replacement verification email without revealing whether an account exists.
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification")
    @ApiResponse(responseCode = "202", description = "Verification request accepted")
    public ResponseEntity<EmailVerificationResponse> resendVerification(
            @Valid @RequestBody ResendEmailVerificationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(emailVerificationService.resend(request));
    }
}
