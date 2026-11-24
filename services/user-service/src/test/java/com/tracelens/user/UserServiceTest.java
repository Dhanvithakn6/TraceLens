package com.tracelens.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class UserServiceTest {
    private UserRequest request(String email) {
        return new UserRequest("Ada Lovelace", email);
    }

    @Test
    void registersAndRetrievesUser() {
        UserService service = new UserService(0, false);
        User created = service.register(request("ada@example.com"));

        assertNotNull(created.userId());
        assertEquals("Ada Lovelace", created.name());
        assertEquals("ada@example.com", created.email());
        assertEquals(UserStatus.ACTIVE, created.status());
        assertEquals(created, service.get(created.userId()));
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        UserService service = new UserService(0, false);
        service.register(request("ada@example.com"));

        assertThrows(DuplicateEmailException.class, () -> service.register(request("ADA@example.com")));
    }

    @Test
    void marksUserFailedWhenFailureIsSimulated() {
        UserService service = new UserService(0, true);

        assertEquals(UserStatus.FAILED, service.register(request("failed@example.com")).status());
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        UserService service = new UserService(0, false);

        assertThrows(UserNotFoundException.class, () -> service.get(UUID.randomUUID()));
    }
}
