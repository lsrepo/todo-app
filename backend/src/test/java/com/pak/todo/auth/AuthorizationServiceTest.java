package com.pak.todo.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Permission;
import com.pak.todo.model.entity.User;
import com.pak.todo.model.enums.PermissionRole;
import com.pak.todo.repository.PermissionRepository;

class AuthorizationServiceTest {

	// Scenario: canViewBoard returns true when user has any permission on the board
	// Given: the repository returns a permission for the user and board
	// When: canViewBoard(user, board) is called
	// Then: true is returned
	@Test
	void canViewBoard_hasPermission_returnsTrue() {
		PermissionRepository permissionRepository = mock(PermissionRepository.class);
		User user = User.create(UUID.randomUUID(), "u", "hash");
		Board board = Board.create(UUID.randomUUID(), "Board", null);
		Permission permission = Permission.create(UUID.randomUUID(), user, board, PermissionRole.EDITOR);
		when(permissionRepository.findByUserIdAndBoardId(user.getId(), board.getId())).thenReturn(Optional.of(permission));

		AuthorizationService service = new AuthorizationService(permissionRepository);
		boolean result = service.canViewBoard(user, board);

		assertThat(result).isTrue();
	}

	// Scenario: canViewBoard returns false when user has no permission on the board
	// Given: the repository returns empty for the user and board
	// When: canViewBoard(user, board) is called
	// Then: false is returned
	@Test
	void canViewBoard_noPermission_returnsFalse() {
		PermissionRepository permissionRepository = mock(PermissionRepository.class);
		User user = User.create(UUID.randomUUID(), "u", "hash");
		Board board = Board.create(UUID.randomUUID(), "Board", null);
		when(permissionRepository.findByUserIdAndBoardId(user.getId(), board.getId())).thenReturn(Optional.empty());

		AuthorizationService service = new AuthorizationService(permissionRepository);
		boolean result = service.canViewBoard(user, board);

		assertThat(result).isFalse();
	}

	// Scenario: canEditBoard returns true when user has OWNER or EDITOR role
	// Given: the repository reports user has one of OWNER, EDITOR for the board
	// When: canEditBoard(user, board) is called
	// Then: true is returned
	@Test
	void canEditBoard_hasEditorRole_returnsTrue() {
		PermissionRepository permissionRepository = mock(PermissionRepository.class);
		User user = User.create(UUID.randomUUID(), "u", "hash");
		Board board = Board.create(UUID.randomUUID(), "Board", null);
		when(permissionRepository.existsByUserIdAndBoardIdAndRoleIn(eq(user.getId()), eq(board.getId()), any()))
				.thenReturn(true);

		AuthorizationService service = new AuthorizationService(permissionRepository);
		boolean result = service.canEditBoard(user, board);

		assertThat(result).isTrue();
	}

	// Scenario: canEditBoard returns false when user has no edit role
	// Given: the repository reports no OWNER/EDITOR for the user and board
	// When: canEditBoard(user, board) is called
	// Then: false is returned
	@Test
	void canEditBoard_noEditRole_returnsFalse() {
		PermissionRepository permissionRepository = mock(PermissionRepository.class);
		User user = User.create(UUID.randomUUID(), "u", "hash");
		Board board = Board.create(UUID.randomUUID(), "Board", null);
		when(permissionRepository.existsByUserIdAndBoardIdAndRoleIn(eq(user.getId()), eq(board.getId()), any()))
				.thenReturn(false);

		AuthorizationService service = new AuthorizationService(permissionRepository);
		boolean result = service.canEditBoard(user, board);

		assertThat(result).isFalse();
	}

	// Scenario: canDeleteBoard returns true when user is OWNER
	// Given: the repository reports user has OWNER for the board
	// When: canDeleteBoard(user, board) is called
	// Then: true is returned
	@Test
	void canDeleteBoard_hasOwnerRole_returnsTrue() {
		PermissionRepository permissionRepository = mock(PermissionRepository.class);
		User user = User.create(UUID.randomUUID(), "u", "hash");
		Board board = Board.create(UUID.randomUUID(), "Board", null);
		when(permissionRepository.existsByUserIdAndBoardIdAndRoleIn(eq(user.getId()), eq(board.getId()), any()))
				.thenReturn(true);

		AuthorizationService service = new AuthorizationService(permissionRepository);
		boolean result = service.canDeleteBoard(user, board);

		assertThat(result).isTrue();
	}

	// Scenario: grantOwnerIfMissing does nothing when user already has OWNER
	// Given: the repository reports user already has OWNER for the board
	// When: grantOwnerIfMissing(user, board) is called
	// Then: save is not called
	@Test
	void grantOwnerIfMissing_alreadyOwner_doesNotSave() {
		PermissionRepository permissionRepository = mock(PermissionRepository.class);
		User user = User.create(UUID.randomUUID(), "u", "hash");
		Board board = Board.create(UUID.randomUUID(), "Board", null);
		when(permissionRepository.existsByUserIdAndBoardIdAndRoleIn(eq(user.getId()), eq(board.getId()), any()))
				.thenReturn(true);

		AuthorizationService service = new AuthorizationService(permissionRepository);
		service.grantOwnerIfMissing(user, board);

		verify(permissionRepository, org.mockito.Mockito.never()).save(any());
	}

	// Scenario: grantOwnerIfMissing creates and saves OWNER permission when user has no role
	// Given: the repository reports user has no OWNER for the board
	// When: grantOwnerIfMissing(user, board) is called
	// Then: save is called once with a new Permission with role OWNER
	@Test
	void grantOwnerIfMissing_noOwner_savesNewPermission() {
		PermissionRepository permissionRepository = mock(PermissionRepository.class);
		User user = User.create(UUID.randomUUID(), "u", "hash");
		Board board = Board.create(UUID.randomUUID(), "Board", null);
		when(permissionRepository.existsByUserIdAndBoardIdAndRoleIn(eq(user.getId()), eq(board.getId()), any()))
				.thenReturn(false);

		AuthorizationService service = new AuthorizationService(permissionRepository);
		service.grantOwnerIfMissing(user, board);

		org.mockito.ArgumentCaptor<Permission> captor = org.mockito.ArgumentCaptor.forClass(Permission.class);
		verify(permissionRepository).save(captor.capture());
		Permission saved = captor.getValue();
		assertThat(saved.getUser()).isEqualTo(user);
		assertThat(saved.getBoard()).isEqualTo(board);
		assertThat(saved.getRole()).isEqualTo(PermissionRole.OWNER);
	}
}
