## Description
<!-- Provide a brief description of the changes introduced in this PR. -->

## Microservice Affected
- [ ] `footprint-service` (Java 21 / Spring Boot 3)
- [ ] `ml-service` (Python 3.12 / FastAPI)
- [ ] Database / Flyway migrations
- [ ] Docker / CI/CD orchestration
- [ ] Documentation / Benchmarks

## Type of Change
- [ ] `feat`: New feature or capability
- [ ] `fix`: Bug fix
- [ ] `perf`: Performance improvement (e.g. caching, indexing)
- [ ] `test`: Unit, integration, or regression tests
- [ ] `refactor`: Code reorganization with no behavior change
- [ ] `docs`: Documentation updates
- [ ] `ci`: CI/CD pipeline or configuration

## Verification Checklist
- [ ] All Java tests pass (`mvn verify`) with Spotless formatting verified (`mvn spotless:check`)
- [ ] JaCoCo code coverage report generated
- [ ] Python ruff linter and formatter pass (`ruff check . && ruff format --check .`)
- [ ] All Pytest tests pass (`pytest -v`)
- [ ] Docker Compose builds and passes health checks (`docker compose up --build -d`)
- [ ] OpenAPI documentation updated (if endpoints modified)
- [ ] Follows conventional commit message format

## Relevant Issues / Notes
<!-- Link to related issue or describe manual testing steps. -->
