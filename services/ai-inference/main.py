from fastapi import FastAPI

app = FastAPI(title="Structra AI inference", version="0.0.1")


@app.get("/health")
def health():
    return {"ok": True}


@app.post("/predict/anomaly")
def predict_stub(payload: dict):
    # later: isolation forest / lstm etc
    return {"risk_score": 0.0, "note": "stub"}
