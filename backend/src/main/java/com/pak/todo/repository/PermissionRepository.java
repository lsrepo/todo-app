package com.pak.todo.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pak.todo.model.entity.Permission;
import com.pak.todo.model.enums.PermissionRole;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

	Optional<Permission> findByUserIdAndBoardId(UUID userId, UUID boardId);

	boolean existsByUserIdAndBoardIdAndRoleIn(UUID userId, UUID boardId, Collection<PermissionRole> roles);

	List<Permission> findByBoardId(UUID boardId);
}

