package com.pak.todo.service;

import com.pak.todo.model.dto.BoardResponse;
import com.pak.todo.model.entity.Board;
import com.pak.todo.repository.BoardRepository;
import com.pak.todo.model.mapper.BoardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardService {

	private final BoardRepository boardRepository;
	private final BoardMapper boardMapper;

	@Transactional(readOnly = true)
	public Page<BoardResponse> findAll(Pageable pageable) {
		return boardRepository.findAll(pageable).map(boardMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public BoardResponse findById(UUID id) {
		return boardRepository.findById(id)
				.map(boardMapper::toResponse)
				.orElse(null);
	}

	@Transactional(readOnly = true)
	public Board getEntityById(UUID id) {
		return boardRepository.findById(id).orElse(null);
	}
}
