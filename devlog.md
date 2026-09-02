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

---

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

---

## Chapter 7: V2 Machine Learning Integration & Anomaly Detection (August 2026)

### 🎯 Objective
Upgrade the backend Risk Engine from a V1 deterministic rule-based system to a V2 Machine Learning model capable of unsupervised anomaly detection.

### 👣 Steps Taken
1. Wrote `train_mock_model.py` to generate 2,000 samples of realistic synthetic login data. 
2. Trained an `IsolationForest` model via `scikit-learn` and serialized it to `risk_model_v2.pkl`.
3. Upgraded `features.py` to act as a data pipeline, converting Keycloak JSON requests into a 5-dimensional numerical NumPy array.
4. Replaced the `if/then` evaluation logic in `evaluator.py` with the ML model's `predict()` method. 
5. Updated `requirements.txt` and successfully rebuilt the Docker container with the new data science dependencies.
6. Conducted end-to-end verification of the ML engine:
   * **LOW Risk Verified:** Generated a token via the Keycloak UI Simulator (Standard Browser). The ML model recognized the behavior as normal, outputted `v2-ml-isolation-forest`, and successfully retained the `admin` role.
   * **HIGH Risk Verified:** Simulated a cyberattack using Postman and a spoofed `sqlmap/1.5.8#dev` User-Agent via browser DevTools. The ML model detected the anomaly, flagged it as HIGH risk, and successfully stripped the `admin` role from the token payload in real-time.

### 🐛 Errors & Solutions
* **Error:** `ValueError: Probabilities do not sum to 1` during ML model training.
* **Cause:** The hardcoded probability array used in `numpy.random.choice` to generate synthetic login hours totaled `1.075` instead of exactly `1.0`.
* **Solution/Fix:** Implemented a dynamic mathematical normalization step (`normalized_p = hour_weights / hour_weights.sum()`) to ensure the array sum is mathematically perfect before generating the distribution.

### 📌 Architecture & Decision Notes
* **Decision:** Utilized cyclic encoding (`np.sin` and `np.cos`) for time-based feature extraction.
* **Reasoning:** Standard ML models interpret raw hours (0-23) linearly, failing to recognize that 23:00 and 01:00 are chronologically adjacent. Cyclic encoding resolves this, preventing false anomalies at midnight.
* **Decision:** Selected Isolation Forest for the V2 ML architecture.
* **Reasoning:** Anomaly detection in cybersecurity is heavily imbalanced (most logins are normal, few are malicious). Isolation Forest excels at identifying outliers in primarily "normal" datasets without needing extensive labels for every possible attack vector.

## Chapter 8: Final Integration, UI Overhaul, & Live Presentation Defenses (September 2026)

### 🎯 Objective
Integrate the AI-secured Keycloak instance with a modern React/Tailwind frontend, enforce Zero-Trust middleware on the Express backend, and resolve live configuration drifts during the final presentation defense.

### 👣 Steps Taken
1. Developed the **Campus Gate Pass Portal**, a React/Vite Single Page Application using Tailwind CSS to visualize token risk levels and execute API requests.
2. Implemented Zero-Trust Express.js middleware on port 3000 to intercept incoming JWTs, decode the payload, and return a `403 Forbidden` if the `security_admin` role was missing.
3. Connected the React UI to Keycloak to force fresh authentication handshakes, allowing the Java SPI to evaluate spoofed `sqlmap` User-Agents in real-time.
4. Finalized the dynamic role suppression by configuring the `claimsToRemoveStr` parameter in the Keycloak Admin Console to target the custom `security_admin` role.

### 🐛 Errors & Solutions
* **Error:** Express backend failed to start with `EACCES: permission denied 0.0.0.0:3000`.
* **Cause:** Windows NAT (WinNAT) dynamically reserved and locked port 3000.
* **Solution/Fix:** Restarted the Windows NAT service via PowerShell (`net stop winnat` followed by `net start winnat`) to release the port block.
* **Error:** The `security_admin` role was not stripped during the live threat simulation, even though the ML engine correctly flagged the risk as HIGH (returning a `200 OK`).
* **Cause:** Configuration drift. The Keycloak Mapper UI field for "Roles/Scopes to Suppress" was left at its default value (`admin,realm-admin`), causing the dynamic Java code to ignore the `security_admin` role.
* **Solution/Fix:** Updated the custom configuration field in the `gasha-ai-evaluator` mapper settings to explicitly target `security_admin`, immediately resolving the issue without requiring Java code recompilation.

### 📌 Architecture & Decision Notes
* **Decision:** Designed a "Fail-Secure" fallback logic for the SPI's 500ms timeout threshold.
* **Reasoning:** In a production environment, if the Python AI Engine crashes or experiences a Denial-of-Service (DoS) attack, the system must degrade gracefully (Fail-Closed) by stripping administrative privileges rather than Failing-Open and granting unchecked access to potential threats.