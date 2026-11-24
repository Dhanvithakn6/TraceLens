package com.tracelens.user;

import java.util.UUID;

public record User(UUID userId, String name, String email, UserStatus status) {
}
