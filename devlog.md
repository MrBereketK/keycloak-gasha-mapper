# Development Log: Keycloak Context-Aware Risk Mapper

## Chapter 1: Concept, Architecture, and SRS Phase

### 🎯 Objective
Define the core problem, establish the system architecture, and write the Software Requirements Specification (SRS) for a dynamic, AI-driven Keycloak authentication flow.

### 👣 Steps Taken
1. Identified the limitation in standard Keycloak roles (static assignment).
2. Designed a dual-system architecture: 
   * A Java-based Keycloak Service Provider Interface (SPI) to intercept tokens.
   * A Python (FastAPI) AI Engine to evaluate session context and return risk scores.
3. Drafted the SRS document detailing the REST API contract between the Java SPI and the Python Engine.
4. Submitted the SRS and Concept Note to the project advisor for approval.

### 📌 Architecture & Decision Notes
* **Decision:** Decided against hardcoding risk logic inside Keycloak. 
* **Reasoning:** Decoupling the AI logic into a separate Python microservice allows for heavy data science libraries to be used later without bloating the Keycloak Java environment.

---

## Chapter 2: Infrastructure & Boilerplate Scaffolding

### 🎯 Objective
Set up the local development infrastructure using Docker to ensure all microservices can communicate on the same network.

### 👣 Steps Taken
1. Initialized the project repository.
2. Wrote the local infrastructure configuration in `docker-compose.yml` to spin up Keycloak and a PostgreSQL database.
3. Created a custom Docker network so the future Python API container can securely communicate with Keycloak.
4. Scaffolded the Maven `pom.xml` file to import the necessary Keycloak Core and Server SPI dependencies.

### 🐛 Errors & Solutions
* **Error:** Container data wiping upon restart.
* **Cause:** Running `docker compose down` removes non-persistent containers, wiping out UI configurations and test users.
* **Solution/Fix:** (Pending) Need to map a persistent Docker volume to the PostgreSQL database in the compose file to save state between sessions.

---

## Chapter 3: The Java SPI Plugin Development

### 🎯 Objective
Write the Keycloak Protocol Mapper that intercepts the token generation process, pauses it, and makes an HTTP call to the AI Engine.

### 👣 Steps Taken
1. Implemented the `ProtocolMapper` and `ProtocolMapperFactory` Java interfaces.
2. Wrote the logic to extract user context (IP address, time of day, user ID) from the active Keycloak session.
3. Added the HTTP client logic to send a POST request with the context payload to the external Python API.
4. Configured the mapper to parse the returning JSON (e.g., `{"risk_level": "HIGH"}`) and dynamically strip high-privilege roles (like `admin`) from the JWT token if the risk is deemed too high.
5. Packaged the SPI into a JAR file and deployed it to the Keycloak container's `/opt/keycloak/providers` directory.

---

## Chapter 4: The Python API (Risk Engine) Setup

### 🎯 Objective
Establish the FastAPI backend that will receive Keycloak's webhooks and return the dynamic risk assessment.

### 👣 Steps Taken
1. Scaffolded a standard Python FastAPI application.
2. Created the `/api/v1/evaluate` POST endpoint.
3. Defined the Pydantic data models to strictly validate the incoming JSON payload from Keycloak.
4. Implemented a temporary mock logic system returning `HIGH` risk by default for testing purposes before implementing the actual AI algorithms.

---

## Chapter 5: OIDC Vulnerability & UserInfo Endpoint Security (August 9, 2026)

### 🎯 Objective
Address the advisor's security concern: Ensure that suppressed roles are not leaked via the OpenID Connect `/userinfo` endpoint.

### 👣 Steps Taken
1. Configured the custom AI Risk Mapper in the Keycloak Admin Console UI under the `admin-cli` client scopes.
2. Toggled the "Add to userinfo" switch to force the endpoint to trigger the custom Java SPI.
3. Executed an automated PowerShell script to bypass the 60-second token expiration window and verify the endpoint via CLI:
    ```powershell
    $TOKEN = (curl.exe -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=admin-cli" -d "username=testuser" -d "password=12345678" -d "grant_type=password" -d "scope=openid" | ConvertFrom-Json).access_token

    curl.exe -v -X GET "http://localhost:8080/realms/master/protocol/openid-connect/userinfo" -H "Authorization: Bearer $TOKEN"
    ```
4. Replicated the endpoint verification using Postman for clean, visual JSON proofs:
    * **Token Request:** Created a `POST` request to `http://localhost:8080/realms/master/protocol/openid-connect/token`.
    * **Body Config:** Selected the `Body` tab -> `x-www-form-urlencoded` and added the following keys:
      * `client_id` : `admin-cli`
      * `username` : `testuser`
      * `password` : `12345678`
      * `grant_type` : `password`
      * `scope` : `openid`
    * **Endpoint Request:** Created a `GET` request to `http://localhost:8080/realms/master/protocol/openid-connect/userinfo`, selecting the `Auth` tab and passing the retrieved string as a `Bearer Token`.
5. Successfully verified that the `admin` role is stripped from the endpoint response:
    ```json
    {
        "sub": "3be3bf5d-eba8-4722-934f-00a95e3fc3cb",
        "email_verified": false,
        "preferred_username": "testuser"
    }
    ```

### 🐛 Errors & Solutions
* **Error:** `/userinfo` endpoint returned `401 Unauthorized` during manual terminal testing.
* **Cause:** OpenID tokens have a strict 60-second lifespan. The token expired while manually copying and pasting it into the next command.
* **Solution/Fix:** Chained the token generation and endpoint request into the single automated PowerShell script above.
* **Error:** Postman returned `Missing form parameter: grant_type` on the token request.
* **Cause:** Parameters were accidentally placed in the URL query ("Params" tab) instead of the request body.
* **Solution/Fix:** Moved the payload to the "Body" tab and selected the `x-www-form-urlencoded` format per OIDC specifications.

### 📌 Architecture & Decision Notes
* Successfully verified that Keycloak's dynamic mapper architecture correctly intercepts endpoint requests, confirming the security flaw is patched.

### ⏭️ Next Steps
* Scaffold the Next.js frontend to visually demonstrate the dynamic token modification and conditional UI rendering based on the active risk level.
* Add persistent database volumes to `docker-compose.yml`.

## Chapter 6: Execution Priority, Scope Configuration & Final Verification (August 2026)

### 🎯 Objective
Finalize the end-to-end integration, resolve scope and priority bugs in Keycloak, and definitively prove that the dynamic role suppression works accurately in both LOW and HIGH risk states.

### 👣 Steps Taken
1. Rebuilt the Python Risk Engine container using `docker compose up -d --build --no-deps risk-engine` to safely update Python logic without destroying the Keycloak database state.
2. Configured the Keycloak UI to properly inject roles by toggling "Full scope allowed" to ON within the `admin-cli-dedicated` client scope.
3. Modified `AiRiskProtocolMapper.java` to override the `getPriority()` method, returning `1000`. This forces the custom AI mapper to execute *after* Keycloak's default role mappers.
4. Recompiled the Java plugin using Maven (`mvn clean package`) and restarted the Keycloak container (`docker compose restart keycloak`) to load the updated JAR.
5. Evaluated tokens via the Keycloak Admin Console UI to verify final states:
   * **LOW Risk:** Token successfully retains the `"admin"` role in the `realm_access` array.
   * **HIGH Risk:** Token successfully drops the `"admin"` role and sets `"restricted": true`.

### 🐛 Errors & Solutions
* **Error:** Docker build stalled for an extended time while downloading the `uvloop` dependency during `pip install`.
* **Cause:** Severe network latency connecting to the PyPI registry.
* **Solution/Fix:** Allowed the download to finish without interruption and utilized `docker compose stop` to safely pause the environment between sessions without losing the non-persistent Keycloak UI configurations.
* **Error:** The `admin` role was completely missing from the token even when the risk was LOW and the mapper set `restricted: false`.
* **Cause:** The `roles` client scope was restricted from passing through the `admin-cli` client.
* **Solution/Fix:** Navigated to Client Scopes -> `admin-cli-dedicated` -> Scope tab, and turned on the "Full scope allowed" toggle.
* **Error:** The `admin` role was still present in the token when the risk was explicitly HIGH.
* **Cause:** An execution order (priority) conflict. Keycloak's default role mapper ran *after* the custom SPI, overwriting the AI engine's decision and re-injecting the role.
* **Solution/Fix:** Overrode the `getPriority()` method in the Java SPI to return `1000`, guaranteeing the custom mapper has the final authority to strip the roles just before the token is signed.

### 📌 Architecture & Decision Notes
* **Decision:** Explicitly classified the V1 backend as a "Rule-Based Risk Engine" (Heuristic Risk Assessor) rather than a true Machine Learning model.
* **Reasoning:** The current logic is strictly deterministic (hardcoded `if/then` conditions). This establishes a predictable, reliable baseline for V1. Because of the decoupled architecture, swapping this out for a trained classification model (e.g., an Isolation Forest via `scikit-learn`) in V2 will require zero modifications to the Java SPI or Docker network.