package com.sharenote.user;

import com.sharenote.user.dto.RegisterUserRequest;
import com.sharenote.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerHashesPasswordAndReturnsUserResponse() {
        RegisterUserRequest request = validRequest();
        when(userRepository.existsByEmailIgnoreCase("amina@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPass123")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getPassword()).isEqualTo("hashed-password");
        assertThat(savedUser.getRoles()).containsExactly(Role.USER);
        assertThat(response.email()).isEqualTo("amina@example.com");
        assertThat(response.roles()).containsExactly("USER");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterUserRequest request = validRequest();
        when(userRepository.existsByEmailIgnoreCase("amina@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("amina@example.com");
    }

    private RegisterUserRequest validRequest() {
        return new RegisterUserRequest(
                "Amina",
                null,
                "Rahman",
                "AMINA@example.com",
                "StrongPass123",
                "university",
                "3",
                "+491234567890",
                "Germany"
        );
    }
}
