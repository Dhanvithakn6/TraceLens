package com.tracelens.user;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final ConcurrentMap<UUID, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> userIdsByEmail = new ConcurrentHashMap<>();
    private final long delayMs;
    private final boolean simulateFailure;

    public UserService(
            @Value("${user.simulation.delay-ms:0}") long delayMs,
            @Value("${user.simulation.failure:false}") boolean simulateFailure) {
        if (delayMs < 0) {
            throw new IllegalArgumentException("user.simulation.delay-ms must be non-negative");
        }
        this.delayMs = delayMs;
        this.simulateFailure = simulateFailure;
    }

    public User register(UserRequest request) {
        delayIfConfigured();
        String emailKey = request.email().trim().toLowerCase(Locale.ROOT);
        UUID id = UUID.randomUUID();
        if (userIdsByEmail.putIfAbsent(emailKey, id) != null) {
            throw new DuplicateEmailException(request.email());
        }

        User user = new User(id, request.name(), request.email().trim(),
                simulateFailure ? UserStatus.FAILED : UserStatus.ACTIVE);
        users.put(id, user);
        return user;
    }

    public User get(UUID id) {
        User user = users.get(id);
        if (user == null) {
            throw new UserNotFoundException(id);
        }
        return user;
    }

    private void delayIfConfigured() {
        if (delayMs == 0) return;
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("User registration was interrupted", exception);
        }
    }
}
