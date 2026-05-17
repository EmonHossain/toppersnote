package com.sharenote.common;

import com.sharenote.auth.InvalidCredentialsException;
import com.sharenote.auth.InvalidRefreshTokenException;
import com.sharenote.note.CurrentUserNotFoundException;
import com.sharenote.note.InvalidNoteQueryException;
import com.sharenote.storage.FileStorageException;
import com.sharenote.storage.InvalidFileException;
import com.sharenote.user.EmailAlreadyExistsException;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                "One or more fields are invalid",
                fieldErrors
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException exception) {
        return buildResponse(HttpStatus.CONFLICT, "Email already exists", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid upload", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(InvalidNoteQueryException.class)
    public ResponseEntity<ErrorResponse> handleInvalidNoteQuery(InvalidNoteQueryException exception) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid note query", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(CurrentUserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCurrentUserNotFound(CurrentUserNotFoundException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", exception.getMessage(), Map.of());
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorage(FileStorageException exception) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "File storage error",
                "Could not store uploaded file",
                Map.of()
        );
    }

    @ExceptionHandler(WeakKeyException.class)
    public ResponseEntity<ErrorResponse> handleWeakJwtKey(WeakKeyException exception) {
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "JWT configuration error",
                "JWT secret must be at least 256 bits for HS256",
                Map.of()
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String error,
            String message,
            Map<String, String> validationErrors
    ) {
        return ResponseEntity.status(status).body(new ErrorResponse(
                Instant.now(),
                status.value(),
                error,
                message,
                validationErrors
        ));
    }
}
