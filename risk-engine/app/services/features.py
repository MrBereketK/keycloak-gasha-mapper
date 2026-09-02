from datetime import datetime, timezone
import numpy as np

from app.schemas.request import EvaluationRequest


# Security / automation tools
SUSPICIOUS_USER_AGENTS = [
    "sqlmap",
    "nikto",
    "nmap",
    "masscan",
    "wpscan",
]


# Programmatic clients.
# These are not necessarily malicious,
# but they can be suspicious in an authentication context.
AUTOMATION_USER_AGENTS = [
    "curl",
    "python-requests",
    "wget",
    "postmanruntime",
]


CRITICAL_ROLES = [
    "admin",
    "realm-admin",
    "superuser",
    "finance-admin",
]


class FeatureExtractor:

    @staticmethod
    def extract(request: EvaluationRequest) -> np.ndarray:

        ctx = request.context

        # --------------------------------------------------
        # Feature 1 & 2: Login hour as cyclic values
        # --------------------------------------------------

        login_dt = datetime.fromtimestamp(
            ctx.timestamp / 1000.0,
            tz=timezone.utc
        )

        hour = login_dt.hour

        hour_sin = np.sin(2 * np.pi * hour / 24)
        hour_cos = np.cos(2 * np.pi * hour / 24)

        # --------------------------------------------------
        # Feature 3: Suspicious User-Agent
        # --------------------------------------------------

        ua_lower = ctx.user_agent.lower()

        suspicious_ua = 0

        if any(
            tool in ua_lower
            for tool in SUSPICIOUS_USER_AGENTS
        ):
            suspicious_ua = 1

        elif any(
            tool in ua_lower
            for tool in AUTOMATION_USER_AGENTS
        ):
            # Automation is suspicious but not automatically malicious.
            suspicious_ua = 1

        # --------------------------------------------------
        # Feature 4: IP Reputation
        # --------------------------------------------------

        # This should ideally come from a real reputation
        # service or your organization's threat-intelligence system.
        #
        # For now, the request is expected to provide:
        #
        # ctx.ip_reputation
        #
        # 0 = normal
        # 1 = suspicious / flagged

        ip_reputation = int(
            getattr(ctx, "ip_reputation", 0)
        )

        # --------------------------------------------------
        # Feature 5: Privileged Target
        # --------------------------------------------------

        has_critical_role = 0

        if any(
            role in CRITICAL_ROLES
            for role in ctx.user.assigned_roles
        ):
            has_critical_role = 1

        # --------------------------------------------------
        # Final feature vector
        # --------------------------------------------------

        features = np.array([[
            hour_sin,
            hour_cos,
            suspicious_ua,
            ip_reputation,
            has_critical_role
        ]])

        return features