# Keycloak Gasha Mapper (ጋሻ) 🛡️

**Gasha** (*ጋሻ* — Shield) is an adaptive, zero-trust security extension for Keycloak. It acts as a protective shield between keycloak authentication flows and downstream microservices by dynamically evaluating login context (IP, posture, location, user risk profile) and injecting or suppressing JWT claims and scopes in real time.

### Key Features
* **Dynamic Claim Injection:** Enforces context-driven claim adjustments (`risk_level`, `restricted`) without mutating Keycloak's core database.
* **AI Risk Integration:** Synchronously queries an external rule/ML engine (FastAPI) during token issuance.
* **Resilient Failover:** Configurable fallback mechanisms (`FAIL_OPEN` / `FAIL_CLOSED`) with precise millisecond timeout management.
