# Multi Drive API Project Rules

These rules apply to the whole repository. Every code change in this Spring
Boot REST API must be checked against the checklist below before it is treated
as complete.

## Core Workflow

- Preserve the existing Java/Spring Boot layered architecture.
- Keep controllers thin. Put business logic in services and data access in
  repositories.
- Use request and response DTOs at API boundaries. Do not expose JPA entities
  directly from controllers.
- Keep changes scoped to the requested behavior. Avoid unrelated refactors.
- Use Flyway migrations for schema changes. Do not rely on Hibernate schema
  auto-generation for persistent changes.
- Protect secrets. Do not commit tokens, passwords, OAuth client secrets,
  refresh tokens, private keys, or local credential values.
- Prefer clear multiline Java formatting consistent with the current codebase.
- Use Lombok only where it improves readability without hiding important
  behavior.
- Treat premium code quality as a baseline: clear names, small cohesive methods,
  explicit validation, predictable error handling, focused tests, no hidden side
  effects, no speculative abstractions, and no duplicated business rules.
- Run project verification before finishing backend work:

```bash
./mvnw test
git diff --check
```

If a database migration is added, also verify Flyway has applied it locally.

## Requirements And Low-Level Design Rules

Before implementing a non-trivial feature, bug fix, workflow, or API change,
identify the relevant functional requirements, non-functional requirements, and
low-level design details. Keep this lightweight for small changes, but do not
skip requirements and design when behavior, data, security, or operations are
affected.

### Functional Requirements Checklist

- [ ] User goal / business outcome
- [ ] Primary user roles and permissions
- [ ] In-scope behavior
- [ ] Out-of-scope behavior
- [ ] API endpoints and actions
- [ ] Request inputs
- [ ] Response outputs
- [ ] Validation rules
- [ ] Business rules
- [ ] State transitions
- [ ] Success scenarios
- [ ] Failure scenarios
- [ ] Edge cases
- [ ] Idempotency expectations
- [ ] Authentication requirements
- [ ] Authorization requirements
- [ ] Data ownership and access rules
- [ ] Database reads and writes
- [ ] External API calls
- [ ] File upload/download behavior
- [ ] Background jobs or async behavior
- [ ] Events, webhooks, or notifications
- [ ] Pagination, filtering, and sorting behavior
- [ ] Backward compatibility expectations
- [ ] Acceptance criteria

### Non-Functional Requirements Checklist

- [ ] Security
- [ ] Privacy and PII protection
- [ ] Performance targets
- [ ] Latency expectations
- [ ] Throughput expectations
- [ ] Scalability
- [ ] Availability
- [ ] Reliability
- [ ] Fault tolerance
- [ ] Timeout behavior
- [ ] Retry and backoff behavior
- [ ] Rate limiting
- [ ] Concurrency handling
- [ ] Transaction safety
- [ ] Data consistency
- [ ] Observability
- [ ] Logging
- [ ] Metrics
- [ ] Tracing
- [ ] Auditability
- [ ] Maintainability
- [ ] Testability
- [ ] Deployability
- [ ] Configuration and environment separation
- [ ] Resource usage
- [ ] Database indexes and query efficiency
- [ ] Backup/recovery impact
- [ ] Compliance or governance constraints

### Low-Level Design Checklist

- [ ] Class responsibilities
- [ ] Method responsibilities
- [ ] Package placement
- [ ] Controller/service/repository boundaries
- [ ] DTO design
- [ ] Entity design
- [ ] Mapper design
- [ ] Interface contracts
- [ ] Dependency direction
- [ ] Transaction boundaries
- [ ] Locking or concurrency strategy
- [ ] Error handling strategy
- [ ] Exception types
- [ ] Error response mapping
- [ ] Validation placement
- [ ] Repository query shape
- [ ] Index requirements
- [ ] Migration design
- [ ] Configuration properties
- [ ] Security checks
- [ ] Logging points
- [ ] Metrics/tracing points
- [ ] Retry policy
- [ ] Timeout policy
- [ ] Cache strategy, if needed
- [ ] Async/job execution strategy, if needed
- [ ] Test plan
- [ ] Unit test targets
- [ ] Controller/API test targets
- [ ] Integration test targets
- [ ] Rollback or recovery plan

## Lombok Rules

- Lombok is allowed in this project.
- Prefer Lombok for simple DTOs, immutable value objects, response models, and
  boilerplate reduction.
- Do not use Lombok to hide non-trivial behavior, validation, security checks,
  transaction logic, or external API logic.
- For JPA entities, avoid broad annotations such as `@Data`.
- For JPA entities, be careful with generated `toString`, `equals`, and
  `hashCode` because lazy relationships can cause recursion, unexpected
  database queries, or incorrect identity behavior.
- Prefer explicit constructors when Spring injection, JPA requirements, or
  domain invariants are clearer with plain Java.
- Keep Lombok annotations consistent and minimal. Do not stack annotations that
  produce unclear generated code.
- Ensure annotation processing works in the IDE and Maven build before relying
  on Lombok-generated members.

## Java 21 And Modern Spring Rules

For this project, follow Java 21 and modern Spring Boot practices. When a
version is named below, treat it as a minimum modern baseline and keep the
project's current configured version unless a migration is explicitly requested.

### Java 21 Concepts To Follow

- [ ] Records
- [ ] Sealed classes/interfaces
- [ ] Pattern matching for `instanceof`
- [ ] Pattern matching for `switch`
- [ ] Text blocks
- [ ] Switch expressions
- [ ] Local variable type inference (`var`) - use appropriately
- [ ] Lambda expressions
- [ ] Stream API
- [ ] Functional interfaces
- [ ] `Optional` - avoid abusing it
- [ ] Immutable objects
- [ ] `final` where appropriate
- [ ] Generics
- [ ] Collections Framework
- [ ] `equals()` / `hashCode()` / `toString()`
- [ ] Exception handling
- [ ] Try-with-resources
- [ ] Annotations
- [ ] Reflection - minimize where possible
- [ ] Interfaces and default methods
- [ ] Enums
- [ ] Nested/inner classes
- [ ] Concurrency
- [ ] Virtual threads
- [ ] `CompletableFuture`
- [ ] Executors
- [ ] Synchronization / locks
- [ ] Java Memory Model basics
- [ ] Garbage collection awareness
- [ ] JVM memory management
- [ ] Modern Date/Time API (`Instant`, `LocalDate`, etc.)
- [ ] `HttpClient`
- [ ] Modules - when applicable
- [ ] Pattern matching / type-safe design
- [ ] Avoid legacy APIs where modern alternatives exist

### Modern Spring Boot / Spring Concepts

- [ ] Spring Boot 3.x or newer
- [ ] Spring Framework 6.x or newer
- [ ] Java 21 baseline
- [ ] Jakarta packages (`jakarta.*`, not `javax.*`)
- [ ] Constructor injection
- [ ] Dependency Injection
- [ ] Spring IoC
- [ ] `@Configuration`
- [ ] `@Bean`
- [ ] `@Component`
- [ ] `@Service`
- [ ] `@Repository`
- [ ] `@RestController`
- [ ] Spring MVC
- [ ] Spring Web
- [ ] Spring Validation
- [ ] `@RestControllerAdvice`
- [ ] `ProblemDetail` for API errors
- [ ] Spring Security 6+
- [ ] Lambda-style Spring Security configuration
- [ ] OAuth2/OIDC
- [ ] Resource Server
- [ ] JWT where appropriate
- [ ] Method-level security
- [ ] Spring Data JPA
- [ ] Hibernate 6+
- [ ] Jakarta Persistence
- [ ] `@Transactional`
- [ ] Projections
- [ ] Entity graphs / fetch strategies
- [ ] Database migrations
- [ ] Flyway/Liquibase
- [ ] Spring Boot Actuator
- [ ] Micrometer
- [ ] Observability
- [ ] Distributed tracing
- [ ] Structured logging
- [ ] Configuration properties
- [ ] Profiles
- [ ] Externalized configuration
- [ ] Health/readiness/liveness
- [ ] Graceful shutdown
- [ ] Spring Cache
- [ ] Spring Integration / Messaging when needed
- [ ] Spring for Apache Kafka when needed
- [ ] Testcontainers
- [ ] Spring Boot Test
- [ ] MockMvc
- [ ] WebTestClient where appropriate
- [ ] Integration testing
- [ ] Contract testing where appropriate

### Modern REST API Principles

- [ ] Resource-oriented URLs
- [ ] Correct HTTP methods
- [ ] Correct HTTP status codes
- [ ] Stateless API
- [ ] DTO-based API contracts
- [ ] Records for immutable DTOs where appropriate
- [ ] Request validation
- [ ] Consistent error responses
- [ ] RFC 9457 / `ProblemDetail`
- [ ] API versioning strategy
- [ ] Pagination
- [ ] Filtering
- [ ] Sorting
- [ ] Idempotency
- [ ] Optimistic concurrency control
- [ ] Backward compatibility
- [ ] OpenAPI documentation
- [ ] Content negotiation where needed

### Modern Architecture Principles

- [ ] SOLID
- [ ] DRY
- [ ] KISS
- [ ] YAGNI
- [ ] Separation of Concerns
- [ ] High cohesion
- [ ] Low coupling
- [ ] Composition over inheritance
- [ ] Dependency inversion
- [ ] Clean Architecture
- [ ] Hexagonal Architecture
- [ ] Domain-Driven Design when appropriate
- [ ] Package-by-feature
- [ ] Domain/business logic independent of controllers
- [ ] Domain/business logic independent of database concerns where practical

### Java 21 And Spring-Specific Emphasis

Virtual threads are an important Java 21 concept for Spring Boot:

```properties
spring.threads.virtual.enabled=true
```

Do not treat virtual threads as a magic performance switch. Before enabling or
depending on them, review:

- [ ] Blocking vs non-blocking I/O
- [ ] Database connection pool limits
- [ ] Connection pool sizing
- [ ] Thread-local usage
- [ ] `synchronized` behavior
- [ ] Pinning
- [ ] Concurrency limits
- [ ] Backpressure
- [ ] External-service timeouts

### Preferred Production API Baseline

Prefer this baseline for new production API work unless the existing project
configuration or an explicit migration plan says otherwise:

Java 21 -> Spring Boot 3.x or newer -> Spring Framework 6.x or newer -> Spring
Security 6.x or newer -> Spring Data JPA/Hibernate 6 or newer -> PostgreSQL ->
Flyway -> OpenAPI -> Actuator/Micrometer -> Testcontainers -> Docker.

Architect production APIs around REST + DTOs/records + validation +
service/domain layer + repositories + transactions + centralized `ProblemDetail`
errors + Spring Security + observability + integration tests.

## Design Principles

For this Spring Boot REST API, follow these design principles.

### Core Design Principles

- [ ] SOLID
  - Single Responsibility Principle
  - Open/Closed Principle
  - Liskov Substitution Principle
  - Interface Segregation Principle
  - Dependency Inversion Principle
- [ ] DRY - Don't Repeat Yourself
- [ ] KISS - Keep It Simple
- [ ] YAGNI - You Aren't Gonna Need It
- [ ] Separation of Concerns
- [ ] High Cohesion
- [ ] Low Coupling
- [ ] Composition over Inheritance
- [ ] Dependency Injection
- [ ] Program to interfaces, not implementations
- [ ] Encapsulation
- [ ] Immutability where practical
- [ ] Fail fast
- [ ] Least privilege

### REST/API Design Principles

- [ ] Statelessness
- [ ] Resource-oriented design
- [ ] Consistent URL conventions
- [ ] Correct HTTP semantics
- [ ] Idempotency
- [ ] Backward compatibility
- [ ] Explicit API contracts
- [ ] Consistent request/response formats
- [ ] Consistent error handling
- [ ] Pagination for collections
- [ ] Filtering/sorting conventions
- [ ] API versioning
- [ ] HATEOAS - optional; use only when there is a real requirement.

### Architecture Principles

- [ ] Layered architecture
- [ ] Clean Architecture
- [ ] Hexagonal Architecture / Ports & Adapters
- [ ] Domain-Driven Design (DDD) where domain complexity justifies it
- [ ] Dependency Rule - inner/domain layers should not depend on infrastructure
- [ ] Package by feature for larger applications
- [ ] Keep business logic independent of HTTP
- [ ] Keep business logic independent of persistence where practical

### Database/JPA Principles

- [ ] Keep transactions around business operations
- [ ] Use appropriate transaction boundaries
- [ ] Avoid N+1 queries
- [ ] Do not expose JPA entities as API contracts
- [ ] Use optimistic locking when appropriate
- [ ] Use database constraints for data integrity
- [ ] Do not use `EAGER` fetching as a general solution
- [ ] Optimize queries based on actual access patterns
- [ ] Use migrations instead of manually modifying production schemas

### Security Principles

- [ ] Defense in depth
- [ ] Least privilege
- [ ] Zero trust
- [ ] Never trust client input
- [ ] Secure by default
- [ ] Authentication is not authorization
- [ ] Do not expose sensitive information
- [ ] Do not put secrets in source code
- [ ] Validate at system boundaries
- [ ] Use parameterized queries
- [ ] Apply rate limiting where appropriate

### Reliability Principles

- [ ] Timeouts
- [ ] Retries only when safe
- [ ] Exponential backoff
- [ ] Circuit breaker
- [ ] Bulkhead isolation
- [ ] Idempotent operations
- [ ] Graceful degradation
- [ ] Graceful shutdown
- [ ] Health/readiness checks

### Maintainability Principles

- [ ] Explicit over implicit
- [ ] Readable code over clever code
- [ ] Small focused classes/methods
- [ ] Meaningful naming
- [ ] Consistent conventions
- [ ] Avoid premature abstraction
- [ ] Avoid premature optimization
- [ ] Test business rules
- [ ] Automate quality checks

### Core Set To Remember

SOLID + DRY + KISS + YAGNI + Separation of Concerns + High Cohesion/Low
Coupling + Dependency Injection + Composition over Inheritance + Stateless REST
+ Explicit API contracts + Least Privilege + Defense in Depth + Idempotency +
Fail Fast.

## Spring Boot REST API Checklist

- [ ] REST API design
- [ ] Resource naming
- [ ] HTTP methods
- [ ] HTTP status codes
- [ ] API versioning
- [ ] Request/response DTOs
- [ ] Input validation
- [ ] Error response format
- [ ] Global exception handling
- [ ] Controller / Service / Repository separation
- [ ] Business logic placement
- [ ] JPA entity design
- [ ] Don't expose entities directly
- [ ] Database schema design
- [ ] Transactions
- [ ] Transaction boundaries
- [ ] Lazy vs eager loading
- [ ] N+1 query problem
- [ ] Query optimization
- [ ] Database indexes
- [ ] Pagination
- [ ] Sorting
- [ ] Filtering/search
- [ ] Optimistic/pessimistic locking
- [ ] Concurrency handling
- [ ] Idempotency
- [ ] Authentication
- [ ] Authorization
- [ ] Spring Security
- [ ] JWT/OAuth2
- [ ] Password hashing
- [ ] CORS
- [ ] CSRF
- [ ] HTTPS
- [ ] Rate limiting
- [ ] SQL injection prevention
- [ ] Sensitive-data protection
- [ ] Secrets management
- [ ] Configuration management
- [ ] Environment-specific configuration
- [ ] Logging
- [ ] Structured logging
- [ ] Correlation/request IDs
- [ ] Metrics
- [ ] Distributed tracing
- [ ] Health checks
- [ ] Readiness/liveness probes
- [ ] API documentation / OpenAPI
- [ ] Unit testing
- [ ] Controller/API testing
- [ ] Integration testing
- [ ] Testcontainers
- [ ] Contract testing
- [ ] Dependency management
- [ ] Database migration tools
- [ ] Flyway/Liquibase
- [ ] Connection pool configuration
- [ ] Timeouts
- [ ] Retry handling
- [ ] Circuit breakers
- [ ] External API failure handling
- [ ] Caching
- [ ] Cache invalidation
- [ ] File upload/download security
- [ ] Request size limits
- [ ] Response size limits
- [ ] Serialization/deserialization
- [ ] JSON compatibility
- [ ] Backward compatibility
- [ ] Graceful error handling
- [ ] Graceful shutdown
- [ ] Performance testing
- [ ] Load testing
- [ ] Docker/containerization
- [ ] CI/CD
- [ ] Environment separation
- [ ] Production monitoring
- [ ] Security scanning
- [ ] Dependency vulnerability scanning
- [ ] Audit logging
- [ ] API access logging
- [ ] Data privacy
- [ ] PII protection
- [ ] Database backup/recovery
- [ ] Disaster recovery
- [ ] Production configuration
- [ ] Scalability
- [ ] Horizontal scaling
- [ ] Stateless API design
- [ ] Message queues/events where appropriate
- [ ] Async processing where appropriate
- [ ] API governance/standards
- [ ] Consistent naming conventions
- [ ] Consistent response formats
- [ ] Consistent error codes
- [ ] Code quality
- [ ] SOLID principles
- [ ] Clean architecture / layered architecture
- [ ] Maintainability
- [ ] Observability
- [ ] Documentation
- [ ] Security review
- [ ] Performance review
- [ ] Production readiness review

## Completion Rule

For each implementation task, review the checklist and mark the relevant items
mentally or in the task notes. A task is not complete if it knowingly violates a
relevant checklist item without explicitly calling out the reason and risk.
