package com.pak.todo.auth;

import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Permission;
import com.pak.todo.model.entity.User;
import com.pak.todo.model.enums.PermissionRole;
import com.pak.todo.repository.BoardRepository;
import com.pak.todo.repository.PermissionRepository;
import com.pak.todo.repository.UserRepository;
import com.pak.todo.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthIT {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BoardRepository boardRepository;

	@Autowired
	private PermissionRepository permissionRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private JwtService jwtService;

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"))
			.withDatabaseName("todo")
			.withUsername("postgres")
			.withPassword("postgres");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	// Scenario: accessing a board without any JWT
	// Given: an existing board and no Authorization header
	// When: GET /api/boards/{id} is called
	// Then: the request is rejected as forbidden
	@Test
	void getBoard_withoutToken_returnsForbidden() {
		Board board = boardRepository.save(Board.create(UUID.randomUUID(), "Board", "Desc"));

		ResponseEntity<BoardResponse> response = restTemplate.getForEntity(
				"/api/boards/{id}",
				BoardResponse.class,
				board.getId()
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	// Scenario: accessing a board with a JWT for a user without permissions
	// Given: a board owned by one user and a JWT for a different user
	// When: GET /api/boards/{id} is called with that JWT
	// Then: the request is rejected with 403 Forbidden
	@Test
	void getBoard_withTokenWithoutPermission_returnsForbidden() {
		User owner = saveUser("owner-user", "password1");
		User other = saveUser("other-user", "password2");

		Board board = boardRepository.save(Board.create(UUID.randomUUID(), "Board", "Desc"));
		permissionRepository.save(Permission.create(UUID.randomUUID(), owner, board, PermissionRole.OWNER));

		String otherToken = loginAndGetToken("other-user", "password2");

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(otherToken);
		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ResponseEntity<BoardResponse> response = restTemplate.exchange(
				"/api/boards/{id}",
				HttpMethod.GET,
				entity,
				BoardResponse.class,
				board.getId()
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	// Scenario: accessing a board with a JWT for the owner
	// Given: a board and a JWT for the owning user
	// When: GET /api/boards/{id} is called with that JWT
	// Then: the request succeeds with 200 OK and the board payload
	@Test
	void getBoard_withOwnerToken_returnsOk() {
		User owner = saveUser("board-owner", "password3");
		Board board = boardRepository.save(Board.create(UUID.randomUUID(), "Board", "Desc"));
		permissionRepository.save(Permission.create(UUID.randomUUID(), owner, board, PermissionRole.OWNER));

		String token = loginAndGetToken("board-owner", "password3");

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ResponseEntity<BoardResponse> response = restTemplate.exchange(
				"/api/boards/{id}",
				HttpMethod.GET,
				entity,
				BoardResponse.class,
				board.getId()
		);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().getId()).isEqualTo(board.getId());
	}

	private User saveUser(String username, String rawPassword) {
		User user = User.create(UUID.randomUUID(), username, passwordEncoder.encode(rawPassword));
		return userRepository.save(user);
	}

	private String loginAndGetToken(String username, String password) {
		User user = userRepository.findByUsername(username).orElseThrow();
		return jwtService.generateToken(user);
	}
}

