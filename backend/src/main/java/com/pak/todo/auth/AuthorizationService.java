package com.pak.todo.auth;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pak.todo.model.entity.Board;
import com.pak.todo.model.entity.Permission;
import com.pak.todo.model.entity.User;
import com.pak.todo.model.enums.PermissionRole;
import com.pak.todo.repository.PermissionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthorizationService {

	private final PermissionRepository permissionRepository;

	@Transactional(readOnly = true)
	public boolean canViewBoard(User user, Board board) {
		return permissionRepository.findByUserIdAndBoardId(user.getId(), board.getId()).isPresent();
	}

	@Transactional(readOnly = true)
	public boolean canEditBoard(User user, Board board) {
		return hasAnyRole(user.getId(), board.getId(), EnumSet.of(PermissionRole.OWNER, PermissionRole.EDITOR));
	}

	@Transactional(readOnly = true)
	public boolean canDeleteBoard(User user, Board board) {
		return hasAnyRole(user.getId(), board.getId(), EnumSet.of(PermissionRole.OWNER));
	}

	@Transactional(readOnly = true)
	public boolean canModifyTasks(User user, Board board) {
		return canEditBoard(user, board);
	}

	@Transactional
	public void grantOwnerIfMissing(User user, Board board) {
		if (hasAnyRole(user.getId(), board.getId(), EnumSet.of(PermissionRole.OWNER))) {
			return;
		}
		Permission permission = Permission.create(
				UUID.randomUUID(),
				user,
				board,
				PermissionRole.OWNER
		);
		permissionRepository.save(permission);
	}

	private boolean hasAnyRole(UUID userId, UUID boardId, Set<PermissionRole> roles) {
		return permissionRepository.existsByUserIdAndBoardIdAndRoleIn(userId, boardId, roles);
	}
}

