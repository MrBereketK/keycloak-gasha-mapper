from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)


def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_evaluate_low_risk():
    payload = {
        "realm": "master",
        "client_id": "test-app",
        "context": {
            "client_ip": "10.0.0.1",
            "user_agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
            "timestamp": 1710000000000,
            "headers": {},
            "user": {
                "user_id": "usr-123",
                "username": "alice",
                "assigned_roles": ["user"]
            }
        }
    }
    response = client.post("/api/v1/evaluate", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["risk_level"] == "LOW"
    assert data["risk_score"] < 0.3


def test_evaluate_high_risk_automated_tool():
    payload = {
        "realm": "master",
        "client_id": "test-app",
        "context": {
            "client_ip": "198.51.100.45",  # Flagged subnet
            "user_agent": "python-requests/2.31.0",  # Suspicious UA
            "timestamp": 1710000000000,
            "headers": {},
            "user": {
                "user_id": "usr-999",
                "username": "admin_user",
                "assigned_roles": ["realm-admin"]
            }
        }
    }
    response = client.post("/api/v1/evaluate", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["risk_level"] == "HIGH"
    assert data["risk_score"] >= 0.6
    assert len(data["reasons"]) >= 2