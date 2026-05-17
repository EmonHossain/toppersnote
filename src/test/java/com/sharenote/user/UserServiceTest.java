package com.sharenote.user;

import com.sharenote.storage.ProfilePictureFileStorage;
import com.sharenote.storage.StoredFile;
import com.sharenote.user.dto.RegisterUserRequest;
import com.sharenote.user.dto.UserResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    @Mock
    private ProfilePictureFileStorage profilePictureFileStorage;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

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
    void setupProfilePictureStoresImageAndUpdatesCurrentUser() {
        User user = user();
        MockMultipartFile file = profilePictureFile();
        StoredFile storedFile = new StoredFile(
                "avatar.png",
                "stored-avatar.png",
                "image/png",
                25,
                "stored-avatar.png",
                "uploads/profile-pictures/stored-avatar.png"
        );
        authenticate();

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(profilePictureFileStorage.store(file)).thenReturn(storedFile);
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = userService.setupProfilePicture(file);

        assertThat(user.getProfilePictureOriginalFileName()).isEqualTo("avatar.png");
        assertThat(user.getProfilePictureStoredFileName()).isEqualTo("stored-avatar.png");
        assertThat(user.getProfilePictureContentType()).isEqualTo("image/png");
        assertThat(user.getProfilePictureFileSize()).isEqualTo(25);
        assertThat(response.profilePictureOriginalFileName()).isEqualTo("avatar.png");
        assertThat(response.profilePictureContentType()).isEqualTo("image/png");
        assertThat(response.profilePictureFileSize()).isEqualTo(25);
    }

    @Test
    void setupProfilePictureDeletesPreviousFileAfterMetadataUpdate() {
        User user = user();
        user.updateProfilePicture(
                "old-avatar.png",
                "old-stored-avatar.png",
                "image/png",
                10,
                "old-stored-avatar.png",
                "uploads/profile-pictures/old-stored-avatar.png"
        );
        MockMultipartFile file = profilePictureFile();
        StoredFile newStoredFile = new StoredFile(
                "avatar.png",
                "new-stored-avatar.png",
                "image/png",
                25,
                "new-stored-avatar.png",
                "uploads/profile-pictures/new-stored-avatar.png"
        );
        StoredFile previousStoredFile = new StoredFile(
                "old-avatar.png",
                "old-stored-avatar.png",
                "image/png",
                10,
                "old-stored-avatar.png",
                "uploads/profile-pictures/old-stored-avatar.png"
        );
        authenticate();

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(profilePictureFileStorage.store(file)).thenReturn(newStoredFile);
        when(userRepository.save(user)).thenReturn(user);

        userService.setupProfilePicture(file);

        verify(profilePictureFileStorage).deleteIfExists(previousStoredFile);
    }

    @Test
    void setupProfilePictureDeletesNewFileWhenMetadataSaveFails() {
        User user = user();
        MockMultipartFile file = profilePictureFile();
        StoredFile storedFile = new StoredFile(
                "avatar.png",
                "stored-avatar.png",
                "image/png",
                25,
                "stored-avatar.png",
                "uploads/profile-pictures/stored-avatar.png"
        );
        authenticate();

        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        when(profilePictureFileStorage.store(file)).thenReturn(storedFile);
        when(userRepository.save(user)).thenThrow(new RuntimeException("database failure"));

        assertThatThrownBy(() -> userService.setupProfilePicture(file))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("database failure");

        verify(profilePictureFileStorage).deleteIfExists(storedFile);
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

    private void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "amina@example.com",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        ));
    }

    private MockMultipartFile profilePictureFile() {
        return new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}
        );
    }

    private User user() {
        return new User(
                "Amina",
                null,
                "Rahman",
                "amina@example.com",
                "hashed-password",
                "university",
                "3",
                "+491234567890",
                "Germany",
                Set.of(Role.USER)
        );
    }
}
