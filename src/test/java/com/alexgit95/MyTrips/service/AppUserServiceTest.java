package com.alexgit95.MyTrips.service;

import com.alexgit95.MyTrips.model.AppUser;
import com.alexgit95.MyTrips.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService service;

    @Test
    void createUser_shouldEncodeAndPersistUser() {
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "ENC-" + invocation.getArgument(0));

        String rawPassword = service.createUser("alice", "ADMIN");

        assertNotNull(rawPassword);
        assertEquals(12, rawPassword.length());
        assertTrue(rawPassword.matches("[ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789]{12}"));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();

        assertEquals("alice", saved.getUsername());
        assertEquals("ADMIN", saved.getRole());
        assertEquals("ENC-" + rawPassword, saved.getPassword());
    }

    @Test
    void createUserWithPassword_shouldEncodeProvidedPassword() {
        when(passwordEncoder.encode("plain-pass")).thenReturn("encoded-pass");

        service.createUserWithPassword("bob", "plain-pass", "REPORTER");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());

        AppUser saved = captor.getValue();
        assertEquals("bob", saved.getUsername());
        assertEquals("encoded-pass", saved.getPassword());
        assertEquals("REPORTER", saved.getRole());
    }

    @Test
    void importUserWithEncodedPassword_shouldNotReEncodePassword() {
        service.importUserWithEncodedPassword("carol", "already-encoded", "GUEST");

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        verifyNoInteractions(passwordEncoder);

        AppUser saved = captor.getValue();
        assertEquals("carol", saved.getUsername());
        assertEquals("already-encoded", saved.getPassword());
        assertEquals("GUEST", saved.getRole());
    }

    @Test
    void hasUsers_shouldReturnTrueWhenRepositoryCountIsPositive() {
        when(userRepository.count()).thenReturn(2L);

        assertTrue(service.hasUsers());
    }

    @Test
    void deleteUser_shouldDelegateToRepository() {
        service.deleteUser(42L);

        verify(userRepository).deleteById(42L);
    }
}
