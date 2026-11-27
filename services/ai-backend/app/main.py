from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel, Field, field_validator

from app.agents.workflow import run_investigation


class InvestigationRequest(BaseModel):
    incident_id: str = Field(min_length=1)
    service_name: str = Field(min_length=1)
    severity: str = Field(min_length=1)
    description: str = Field(min_length=1)

    @field_validator("incident_id", "service_name", "severity", "description")
    @classmethod
    def reject_whitespace_only(cls, value: str) -> str:
        value = value.strip()
        if not value:
            raise ValueError("must not be blank")
        return value


class InvestigationResponse(BaseModel):
    incident_details: dict[str, Any]
    logs: list[Any]
    metrics: list[Any]
    traces: list[Any]
    findings: list[str]
    probable_root_cause: str | None
    recommendations: list[str]

app = FastAPI(
    title="TraceLens AI Backend",
    description="AI-powered production incident investigation",
    version="1.0.0"
)

@app.get("/")
def root():
    return {"message": "TraceLens AI Backend is running"}

@app.get("/health")
def health():
    return {"status": "healthy"}


@app.post("/api/investigations", response_model=InvestigationResponse)
def create_investigation(request: InvestigationRequest) -> InvestigationResponse:
    """Run the investigation workflow for the supplied incident."""

    state = run_investigation(
        {
            "incident_details": request.model_dump(),
            "logs": [],
            "metrics": [],
            "traces": [],
            "findings": [],
            "probable_root_cause": None,
            "recommendations": [],
        }
    )
    return InvestigationResponse.model_validate(state)
