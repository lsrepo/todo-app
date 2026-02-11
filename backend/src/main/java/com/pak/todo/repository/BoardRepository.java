package com.pak.todo.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pak.todo.model.entity.Board;

public interface BoardRepository extends JpaRepository<Board, UUID> {
}
