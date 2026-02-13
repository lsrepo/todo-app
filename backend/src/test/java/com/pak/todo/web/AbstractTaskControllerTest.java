package com.pak.todo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.pak.todo.auth.AuthorizationService;
import com.pak.todo.command.CreateTaskCommandHandler;
import com.pak.todo.command.DeleteTaskCommandHandler;
import com.pak.todo.command.UpdateTaskCommandHandler;
import com.pak.todo.security.CurrentUserService;
import com.pak.todo.security.JwtAuthenticationFilter;
import com.pak.todo.service.BoardService;
import com.pak.todo.service.TaskService;
import com.pak.todo.web.command.TaskCommandFactory;
import com.pak.todo.web.error.GlobalExceptionHandler;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(TaskController.class)
@Import(GlobalExceptionHandler.class)
abstract class AbstractTaskControllerTest {

	@Autowired
	protected MockMvc mockMvc;

	@MockBean
	protected TaskService taskService;

	@MockBean
	protected BoardService boardService;

	@MockBean
	protected TaskCommandFactory taskCommandFactory;

	@MockBean
	protected CreateTaskCommandHandler createTaskCommandHandler;

	@MockBean
	protected UpdateTaskCommandHandler updateTaskCommandHandler;

	@MockBean
	protected DeleteTaskCommandHandler deleteTaskCommandHandler;

	@MockBean
	protected AuthorizationService authorizationService;

	@MockBean
	protected CurrentUserService currentUserService;

	@MockBean
	protected JwtAuthenticationFilter jwtAuthenticationFilter;
}
