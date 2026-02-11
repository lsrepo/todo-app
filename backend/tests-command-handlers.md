## Command Handler Unit Test Conventions

### Test location and naming

- All command handler unit tests live under `src/test/java` and mirror the main package structure.
- For every handler in `src/main/java/com/pak/todo/command`, there is a corresponding test in `src/test/java/com/pak/todo/command` named `<HandlerName>Test`.
- Examples:
  - `CreateTaskCommandHandler` → `CreateTaskCommandHandlerTest`
  - `UpdateTaskCommandHandler` → `UpdateTaskCommandHandlerTest`
  - `DeleteTaskCommandHandler` → `DeleteTaskCommandHandlerTest`
  - `CreateBoardCommandHandler` → `CreateBoardCommandHandlerTest`
  - `UpdateBoardCommandHandler` → `UpdateBoardCommandHandlerTest`
  - `DeleteBoardCommandHandler` → `DeleteBoardCommandHandlerTest`

### Describing test intent

- Each test method uses a descriptive name that encodes behavior in the pattern:
  - `methodName_condition_expectedResult`
  - Example: `handle_existingBoard_createsTaskAndOutbox`
- Each test method has a short comment block immediately above it that explains the scenario in plain English:
  - Recommended structure:
    - `// Scenario: ...`
    - `// Given: ...`
    - `// When: ...`
    - `// Then: ...`
- Comments focus on business behavior (the “why” and observable outcome), not Mockito setup details.

### Annotations usage

- Tests keep annotations minimal:
  - `@Test` on test methods.
  - Mockito is used via manual `Mockito.mock(...)` calls instead of annotations.
- Additional annotations such as `@DisplayName` and `@Nested` are intentionally avoided; method names and comments are the primary way to convey intent.

