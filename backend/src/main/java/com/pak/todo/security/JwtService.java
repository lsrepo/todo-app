package com.pak.todo.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.pak.todo.model.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

	private final JwtProperties properties;

	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant expiry = now.plusSeconds(properties.getExpirationSeconds());

		byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);

		return Jwts.builder()
				.subject(user.getId().toString())
				.issuer(properties.getIssuer())
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiry))
				.claim("username", user.getUsername())
				.signWith(Keys.hmacShaKeyFor(keyBytes), Jwts.SIG.HS256)
				.compact();
	}

	public Optional<JwtPrincipal> parseAndValidate(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}

		try {
			byte[] keyBytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);

			Jws<Claims> jws = Jwts.parser()
					.verifyWith(Keys.hmacShaKeyFor(keyBytes))
					.requireIssuer(properties.getIssuer())
					.build()
					.parseSignedClaims(token);

			Claims claims = jws.getPayload();
			String subject = claims.getSubject();
			String username = claims.get("username", String.class);

			if (subject == null || username == null) {
				return Optional.empty();
			}

			UUID userId = UUID.fromString(subject);
			return Optional.of(new JwtPrincipal(userId, username));
		}
		catch (JwtException | IllegalArgumentException ex) {
			log.warn("Failed to parse or validate JWT", ex);
			return Optional.empty();
		}
	}
}

