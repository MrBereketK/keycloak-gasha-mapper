from datetime import datetime, timezone
import ipaddress
from app.schemas.request import EvaluationRequest
from app.schemas.response import EvaluationResponse, RiskLevel


# Configurable rule defaults for V1
SUSPICIOUS_USER_AGENTS = ["curl", "python-requests", "postmanruntime", "sqlmap", "nikto"]
BLOCKED_IP_SUBNETS = [
    ipaddress.ip_network("198.51.100.0/24"),  # Example TEST-NET-2 range
    ipaddress.ip_network("203.0.113.0/24"),   # Example TEST-NET-3 range
]
CRITICAL_ROLES = ["admin", "realm-admin", "superuser", "finance-admin"]


class RiskEvaluatorService:
    @staticmethod
    # def evaluate(request: EvaluationRequest) -> EvaluationResponse:
    #     score = 0.0
    #     reasons = []

    #     ctx = request.context
    #     user = ctx.user

    #     # Rule 1: Suspicious / Automated User-Agent Detection
    #     ua_lower = ctx.user_agent.lower()
    #     if any(tool in ua_lower for tool in SUSPICIOUS_USER_AGENTS):
    #         score += 0.4
    #         reasons.append(f"Automated or testing User-Agent detected: '{ctx.user_agent}'")

    #     # Rule 2: IP Range Risk Checks
    #     try:
    #         client_ip_obj = ipaddress.ip_address(ctx.client_ip)
    #         for subnet in BLOCKED_IP_SUBNETS:
    #             if client_ip_obj in subnet:
    #                 score += 0.5
    #                 reasons.append(f"Client IP {ctx.client_ip} belongs to a flagged subnet ({subnet})")
    #                 break
    #     except ValueError:
    #         score += 0.3
    #         reasons.append(f"Malformed IP address format: '{ctx.client_ip}'")

    #     # Rule 3: Off-Hours Login Attempt (e.g., 01:00 AM - 04:00 AM UTC)
    #     login_dt = datetime.fromtimestamp(ctx.timestamp / 1000.0, tz=timezone.utc)
    #     if 1 <= login_dt.hour <= 4:
    #         score += 0.2
    #         reasons.append(f"Authentication during off-peak hours ({login_dt.strftime('%H:%M UTC')})")

    #     # Rule 4: Critical Role Elevation Check
    #     has_critical_role = any(role in user.assigned_roles for role in CRITICAL_ROLES)
    #     if has_critical_role and score > 0.0:
    #         score += 0.2
    #         reasons.append("High-privilege role holder flagged during risk assessment")

    #     # Normalize Risk Level
    #     score = min(round(score, 2), 1.0)

    #     if score >= 0.6:
    #         risk_level = RiskLevel.HIGH
    #     elif score >= 0.3:
    #         risk_level = RiskLevel.MEDIUM
    #     else:
    #         risk_level = RiskLevel.LOW
    #         if not reasons:
    #             reasons.append("Standard contextual parameters validated.")

    #     return EvaluationResponse(
    #         risk_level=risk_level,
    #         risk_score=score,
    #         reasons=reasons,
    #         evaluator_version="v1-rules"
    #     )

    def evaluate(request: EvaluationRequest) -> EvaluationResponse:
        # ... (all your commented out logic) ...

        # TEMPORARY TEST CODE: Force a HIGH risk response
        return EvaluationResponse(
            risk_level=RiskLevel.HIGH,
            risk_score=1.0,
            reasons=["Testing Keycloak role suppression"],
            evaluator_version="test-override"
        )