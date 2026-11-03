# Contributing to Carbonstitch

Thank you for your interest in contributing to **Carbonstitch**! This project follows a branch-and-pull-request workflow to maintain code quality, reliable builds, and comprehensive test coverage.

---

## 1. Development Environment Setup

### Prerequisites
- **Java 21** (Eclipse Temurin recommended) & **Maven 3.9+**
- **Python 3.11+** & `pip`
- **Docker** & **Docker Compose**
- **Git**

### Microservices Overview
- **`footprint-service`**: Spring Boot 3 REST API on port `8080`
- **`ml-service`**: FastAPI inference microservice on port `8000`
- **`postgres`**: PostgreSQL 16 on port `5432`

---

## 2. Branching & Git Workflow

1. Fork or branch off `main`:
   ```bash
   git checkout -b feat/your-feature-name
   # or fix/your-bugfix-name
   ```
2. Make granular, focused commits adhering to the [Conventional Commits](https://www.conventionalcommits.org/) specification:
   - `feat(footprint-service): add bulk asynchronous export endpoint`
   - `fix(ml-service): handle zero-division edge case in feature encoding`
   - `perf(cache): optimize country emission factor cache eviction`
   - `test(footprint-service): add integration test for invalid ISO country code`
   - `docs(api): update OpenAPI spec examples`

---

## 3. Code Quality Standards

### Java (`footprint-service`)
- **Code Style**: Google Java Format enforced via Spotless.
- **Check formatting**:
  ```bash
  mvn spotless:check
  ```
- **Auto-apply formatting**:
  ```bash
  mvn spotless:apply
  ```
- **Run tests & generate JaCoCo report**:
  ```bash
  mvn clean verify
  ```

### Python (`ml-service`)
- **Linter & Formatter**: Ruff.
- **Check code quality**:
  ```bash
  ruff check .
  ruff format --check .
  ```
- **Auto-apply formatting**:
  ```bash
  ruff format .
  ruff check --fix .
  ```
- **Run tests**:
  ```bash
  pytest -v tests/
  ```

---

## 4. Submitting a Pull Request

1. Push your branch to GitHub.
2. Open a Pull Request targeting `main`.
3. Fill out the PR template completely, noting the affected services and verification commands run.
4. Ensure all GitHub Actions CI checks are passing green:
   - `footprint-service` build, Spotless check, and JUnit 5 test suite
   - `ml-service` Ruff lint, formatting check, and Pytest test suite
5. Request a review from the repository maintainer. Once approved, squash-and-merge or rebase onto `main`.
