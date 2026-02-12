package com.pak.todo.security;

import java.util.UUID;

public record JwtPrincipal(UUID userId, String username) {
}

