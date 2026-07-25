from enum import Enum
from typing import List, Optional
from pydantic import BaseModel, Field


class RiskLevel(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"


class EvaluationResponse(BaseModel):
    risk_level: RiskLevel = Field(..., description="Evaluated risk categorization")
    risk_score: float = Field(..., ge=0.0, le=1.0, description="Normalized risk score [0.0 - 1.0]")
    reasons: List[str] = Field(default_factory=list, description="Audit reasons triggering risk level")
    evaluator_version: str = Field(default="v1-rules", description="Engine version used for decision")