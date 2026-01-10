# Technical Guide: Infrastructure & CI/CD Workflow

This guide provides a comprehensive technical overview of the infrastructure, CI/CD pipelines, and local development setup for the **Incident Management** platform. It focuses on the architectural structure, deployment workflows, and technical configurations without delving into business logic.

---

## 🏗️ 1. Project Structure

The project follows a standard Spring Boot architecture with separate configurations for different environments (dev, test, prod).

```
smartcity_incidents/
├── .github/workflows/
│   ├── ci.yml            # Continuous Integration pipeline (Build & Test)
│   └── cd.yml            # Continuous Deployment pipeline (Azure Deploy)
├── src/
│   ├── main/
│   │   ├── java/         # Application source code
│   │   └── resources/
│   │       ├── application.properties      # Base configuration
│   │       ├── application-dev.properties  # Local dev config (H2/MySQL local)
│   │       └── application-prod.properties # Production config (Azure MySQL)
│   └── test/
│       ├── java/         # Unit and Integration tests
│       └── resources/
│           └── application-test.properties # Test config (H2 In-Memory)
├── Dockerfile            # Multi-stage Docker build definition
├── docker-compose.dev.yml  # Local development orchestration
├── docker-compose.prod.yml # Production simulation orchestration
└── pom.xml               # Maven dependencies and plugins (JaCoCo, etc.)
```

---

## 🐳 2. Local Environment (Docker Compose)

We use Docker Compose to manage local environments. The difference between `dev` and `prod` configurations lies mainly in the database connection and active profiles.

### 🛠️ Development Environment (`docker-compose.dev.yml`)
*   **Purpose:** Rapid development and testing.
*   **Database:** Uses a local MySQL container.
*   **Profile:** `dev`.
*   **Features:** Hot-reload (if configured), debug ports open.

**Command to run:**
```bash
docker-compose -f docker-compose.dev.yml up --build
```

### 🚀 Production Simulation (`docker-compose.prod.yml`)
*   **Purpose:** Verify the final container behavior before deploying to Azure.
*   **Database:** Can point to a real production DB or a secured local MySQL.
*   **Profile:** `prod`.
*   **Features:** Optimized JVM settings, no debug ports.

**Command to run:**
```bash
docker-compose -f docker-compose.prod.yml up --build
```

---

## 🔄 3. Continuous Integration (CI) Workflow

**File:** `.github/workflows/ci.yml`
**Trigger:** Push/Pull Request on `dev` or `main`.

The CI pipeline ensures code integrity through the following stages:

1.  **Checkout & Setup:** Retrieves code and installs JDK 17.
2.  **Maven Build & Verify:**
    *   Compiles the application.
    *   Runs **Unit Tests** and **Integration Tests**.
    *   **Fail-Fast:** If any test fails, the pipeline stops immediately.
3.  **Code Coverage (JaCoCo):**
    *   Generates a coverage report.
    *   Uploads the report as a GitHub Artifact (`jacoco-report`) regardless of test success/failure.
4.  **Artifact Archiving:**
    *   If tests pass, the compiled `.jar` file is uploaded as an artifact (`incident-management-jar`).
5.  **Docker Build Check:**
    *   Builds the Docker image (without pushing) to verify `Dockerfile` validity.

---

## ☁️ 4. Continuous Deployment (CD) Workflow - Azure

**File:** `.github/workflows/cd.yml`
**Trigger:** Push on `main` branch (only after PR merge and successful CI).

The CD pipeline automates deployment to the Azure Cloud ecosystem.

### 🏗️ Azure Infrastructure Components
*   **Azure Container Registry (ACR):** Stores private Docker images (`smartcityincidentsacr`).
*   **Azure App Service (Web App):** Hosting platform for the Spring Boot container (`smartcity-incidents-app-2026`).
*   **Azure Database for MySQL (Flexible Server):** Managed MySQL database (`smartcity-mysql-server`).

### 🚀 Deployment Steps

1.  **Authentication:**
    *   Logs into Azure using `AZURE_CREDENTIALS`.
    *   Logs into ACR using `ACR_USERNAME` / `ACR_PASSWORD`.
2.  **Build & Push:**
    *   Builds the production Docker image.
    *   Tags it with the Git SHA for versioning.
    *   Pushes the image to the Azure Container Registry.
3.  **Configuration Injection:**
    *   Updates the Web App settings dynamically via Azure CLI.
    *   Injects sensitive environment variables:
        *   `SPRING_PROFILES_ACTIVE=prod`
        *   `SPRING_DATASOURCE_URL` (Azure MySQL connection string)
        *   `SPRING_DATASOURCE_USERNAME`
        *   `SPRING_DATASOURCE_PASSWORD`
4.  **Deploy:**
    *   Triggers the Web App to pull the new image from ACR and restart.

---

## 🧪 5. Demo Tests Files

To demonstrate the CI "Fail then Success" scenario, the following test files are included in the repository:

### 📄 `src/test/java/com/smartcity/incident_management/LogicFailTest.java`
*   **Type:** Unit Test.
*   **Scenario:** Performs a simple mathematical assertion.
*   **Fail State:** `assertEquals(100, 50 + 49)` -> Fails.
*   **Success State:** `assertEquals(100, 50 + 50)` -> Passes.

### 📄 `src/test/java/com/smartcity/incident_management/CiDemoIntegrationTest.java`
*   **Type:** Integration Test (Spring Boot).
*   **Scenario:** Loads the ApplicationContext.
*   **Configuration:** Uses `@ActiveProfiles("test")` to use H2 Database (avoiding Azure MySQL connection errors during CI).
*   **Fail State:** Contains `fail("Intentional Failure")`.
*   **Success State:** The `fail()` line is commented out or removed.

---

## 🔐 Secrets Management

For the pipelines to work, the following secrets must be configured in GitHub Repository Settings:

| Secret Name | Description |
| :--- | :--- |
| `AZURE_CREDENTIALS` | JSON Service Principal for Azure Login |
| `ACR_LOGIN_SERVER` | Registry URL (e.g., `smartcityincidentsacr.azurecr.io`) |
| `ACR_USERNAME` | ACR Admin Username |
| `ACR_PASSWORD` | ACR Admin Password |
