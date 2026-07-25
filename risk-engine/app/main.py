from fastapi import FastAPI
from app.api.v1.evaluate import router as v1_router

app = FastAPI(
    title="Keycloak AI Risk Engine",
    description="Contextual and Risk-Based Authorization Microservice for Keycloak SPI",
    version="1.0.0",
)

app.include_router(v1_router, prefix="/api")


@app.get("/health", tags=["Health"])
async def health_check():
    return {"status": "UP", "service": "risk-engine", "version": "1.0.0"}