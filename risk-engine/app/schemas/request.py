from typing import Dict, List, Optional

from pydantic import BaseModel, Field


class UserAttributes(BaseModel):

    user_id: str = Field(
        ...,
        description="Keycloak unique user ID"
    )

    username: str = Field(
        ...,
        description="Username"
    )

    email: Optional[str] = None

    assigned_roles: List[str] = Field(
        default_factory=list,
        description="Assigned realm and client roles"
    )


class ContextPayload(BaseModel):

    client_ip: str = Field(
        ...,
        description="Client remote IP address"
    )

    ip_reputation: int = Field(
        default=0,
        ge=0,
        le=1,
        description="IP reputation: 0 = normal, 1 = suspicious"
    )

    user_agent: str = Field(
        ...,
        description="User-Agent string from request header"
    )

    timestamp: int = Field(
        ...,
        description="Epoch timestamp in milliseconds"
    )

    headers: Dict[str, str] = Field(
        default_factory=dict,
        description="Captured HTTP headers"
    )

    user: UserAttributes


class EvaluationRequest(BaseModel):

    realm: str = Field(
        ...,
        description="Keycloak Realm name"
    )

    client_id: str = Field(
        ...,
        description="OIDC Client ID requesting token"
    )

    context: ContextPayload