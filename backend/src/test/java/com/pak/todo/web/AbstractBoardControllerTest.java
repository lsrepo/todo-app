package com.pak.todo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.command.CreateBoardCommandHandler;
import com.pak.todo.command.DeleteBoardCommandHandler;
import com.pak.todo.command.UpdateBoardCommandHandler;
import com.pak.todo.security.CurrentUserService;
import com.pak.todo.security.JwtAuthenticationFilter;
import com.pak.todo.service.BoardService;
import com.pak.todo.web.command.BoardCommandFactory;
import com.pak.todo.web.error.GlobalExceptionHandler;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(BoardController.class)
@Import(GlobalExceptionHandler.class)
abstract class AbstractBoardControllerTest {

	@Autowired
	protected MockMvc mockMvc;

	@MockBean
	protected BoardService boardService;

	@MockBean
	protected BoardCommandFactory boardCommandFactory;

	@MockBean
	protected CreateBoardCommandHandler createBoardCommandHandler;

	@MockBean
	protected UpdateBoardCommandHandler updateBoardCommandHandler;

	@MockBean
	protected DeleteBoardCommandHandler deleteBoardCommandHandler;

	@MockBean
	protected AuthorizationService authorizationService;

	@MockBean
	protected CurrentUserService currentUserService;

	@MockBean
	protected JwtAuthenticationFilter jwtAuthenticationFilter;
}
