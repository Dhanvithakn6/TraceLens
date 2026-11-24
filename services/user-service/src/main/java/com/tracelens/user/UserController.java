package com.tracelens.user;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @PostMapping
    public ResponseEntity<User> register(@Valid @RequestBody UserRequest request) {
        User user = users.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.userId())).body(user);
    }

    @GetMapping("/{id}")
    public User get(@PathVariable UUID id) {
        return users.get(id);
    }
}
