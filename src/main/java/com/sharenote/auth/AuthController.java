package com.sharenote.auth;

import com.sharenote.auth.dto.AuthResponse;
import com.sharenote.auth.dto.LoginRequest;
import com.sharenote.auth.dto.RefreshTokenRequest;
import com.sharenote.verification.EmailVerificationService;
import com.sharenote.verification.dto.EmailVerificationResponse;
import com.sharenote.verification.dto.ResendEmailVerificationRequest;
import com.sharenote.verification.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(AuthService authService, EmailVerificationService emailVerificationService) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(emailVerificationService.verify(request));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<EmailVerificationResponse> verifyEmailFromLink(@RequestParam("token") String token) {
        return ResponseEntity.ok(emailVerificationService.verify(new VerifyEmailRequest(token)));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<EmailVerificationResponse> resendVerification(
            @Valid @RequestBody ResendEmailVerificationRequest request
    ) {
        return ResponseEntity.ok(emailVerificationService.resend(request));
    }
}
