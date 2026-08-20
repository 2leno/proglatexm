# AGENTS.md

POJA-generated Spring Boot 3.2 / Java 21 academic-management API ("proglatexm"). Deploy targets AWS Lambda via POJA. Remote: `github.com/2leno/proglatexm`.

## Commands

- Test (CI runs this on every branch): `.\gradlew.bat test` (Windows) / `./gradlew test`. Java 21 + Gradle 8.5 wrapper.
- Tests need Docker: IT classes extend `FacadeIT`, which spins up a shared Testcontainers Postgres 13.9 container and fakes S3/SQS/EventBridge/Email.
- Formatting is CI-enforced (`format.bat` / `format.sh` on `src/**/*.java` + `git diff --exit-code`) with `google-java-format-1.23.0-all-deps.jar` at repo root. Run `format.bat` before committing; non-conformant code fails CI.
- JaCoCo coverage runs automatically as part of `test` (LINE min **0.8**, `**/gen/**` excluded) — **it gates CI**, mirroring the POJA deploy threshold. `maxParallelForks = 2` (a higher fork count × Hikari pool size can exhaust the shared Testcontainers Postgres `max_connections`), and `testLogging` prints failed/skipped test names in the build log. CI uploads `jacoco-report` and `test-report` GitHub artifacts (uploaded even on failure).
- Run a single test class with `--tests`: `.\gradlew.bat test --tests "*SecurityIT"`.

## Testing

- Integration tests are `*IT.java` classes extending `FacadeIT` (package `api.poja.app.conf`). They boot the full Spring context on a random port and need a running Docker daemon.
- Tests run in parallel (`maxParallelForks` = CPUs/2). The Testcontainers Postgres container is static and shared across all IT classes — never stop it in `@AfterAll` (see comment in `FacadeIT`).
- Use `TestRestTemplate` (autowired) for HTTP assertions; use `@MockBean` to fake AWS-bound beans such as `EventProducer` (see `SecurityIT`).

## Architecture

- Everything lives under `api.poja.app`. POJA template files are annotated `@PojaGenerated` — do NOT edit them (event/SQS, mail, bucket, concurrency, handler, health controllers, `Dummy*` scaffolding).
- Hand-written code carries no `@PojaGenerated`:
  - `model/` — immutable `record` DTOs with Lombok `@Builder` (Course, Student, Exam, Transcript, ...), written from `doc/api.yml`.
  - `repository/model/` — JPA entities prefixed `J` (`JCourse` maps `course`); `repository/` — Spring Data repos also prefixed `J` (`JCourseRepository extends JpaRepository<JCourse, UUID>`).
  - `endpoint/rest/controller/` — REST controllers: only `/hello` + health + JWT security are implemented. The endpoints declared in `doc/api.yml` are NOT yet implemented.
  - `security/` — stateless JWT (jjwt 0.13). Everything except `/ping`, `/health/**`, `/swagger-ui/**`, `/v3/api-docs/**` requires a `Bearer` token. No `/auth/login` controller exists yet.
- `doc/api.yml` is the source of truth for the API contract (Auth, Transcripts, Graduates, Promotions, Courses, Groups, Grades, Students). Keep domain records and controllers in sync with it.
- Flyway migrations in `src/main/resources/db/migration/`, prefixed `V42_*` (POJA baseline). Never edit an applied migration — add a new one with the next number.
- `secrets/` is gitignored: holds `api.yml` (local spec copy) and `default-users.json` (local dev creds). Default users are also seeded by `V42_15` (BCrypt only): `admin/admin123`, `teacher/teacher123`, `student/student123`.
- `org.openapi.generator` plugin is applied but NOT configured — no generated sources exist. Ignore `build/gen` and `.shell/publish_gen_to_maven_local.*`.

## Conventions

- No comments in code, and never write comments or strings in French. All code, messages, and commits are in English.
- Never push directly to `preprod`. Every feature, fix, or refactor goes through a `feat/*` branch with a PR targeting `preprod`.
- Group `api.poja.app`, Java 21 (source/target). Lombok on entities; `lombok.addLombokGeneratedAnnotation=true`.
- DTOs (`model/`): immutable `record` with Lombok `@Builder`; entity ids are `UUID` in JPA entities but `String` in DTOs (see `Course` vs `JCourse`).
- Entities (`repository/model/`): `@Getter/@Setter/@Builder/@NoArgsConstructor/@AllArgsConstructor`, `@Id` + `@GeneratedValue(strategy = GenerationType.UUID)`, `@Enumerated(EnumType.STRING)` for enums, `@Builder.Default` for collections, `@Column(nullable = false)` when required.
- Repos (`repository/`): Spring Data interfaces extending `JpaRepository<JX, UUID>` with method names for lookups (e.g. `findAllByParcours`).
- Controllers (`endpoint/rest/controller/`): `@RestController` + `@AllArgsConstructor` constructor injection.
- JWT: token carries a `roles` claim; `JwtAuthFilter` maps it to `ROLE_`-prefixed authorities. `@EnableMethodSecurity` is on, so use `@PreAuthorize("hasRole('...')")` for role-based access (roles: `ADMIN`, `TEACHER`, `STUDENT`).
- Deploy is fully automated by POJA on push to `preprod`/`prod` (`cd-compute.yml`) — never deploy manually. Bot commits resemble `poja: deployment ID: ...`; leave them untouched.