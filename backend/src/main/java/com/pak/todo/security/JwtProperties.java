package com.pak.todo.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "security.jwt")
@Getter
@Setter
public class JwtProperties {

	/**
	 * Secret key used to sign JWTs. In production this should be a strong, private value.
	 */
	private String secret;

	/**
	 * Token lifetime in seconds.
	 */
	private long expirationSeconds;

	/**
	 * Issuer identifier to embed in and validate against tokens.
	 */
	private String issuer;
}

