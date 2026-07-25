from fastapi import APIRouter, HTTPException, status
from app.schemas.request import EvaluationRequest
from app.schemas.response import EvaluationResponse
from app.services.evaluator import RiskEvaluatorService

router = APIRouter(prefix="/v1", tags=["Evaluation"])


@router.post(
    "/evaluate",
    response_model=EvaluationResponse,
    status_code=status.HTTP_200_OK,
    summary="Evaluate Contextual Risk for Keycloak Authentication"
)
async def evaluate_risk(payload: EvaluationRequest) -> EvaluationResponse:
    try:
        return RiskEvaluatorService.evaluate(payload)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Internal risk evaluation error: {str(e)}"
        )