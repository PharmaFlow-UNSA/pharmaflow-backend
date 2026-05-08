from fastapi import FastAPI
from pydantic import BaseModel, Field
from sentence_transformers import SentenceTransformer

# This embedding-service uses a local embedding model found on https://sbert.net

MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"

app = FastAPI(title="PharmaFlow Embedding Service")
model = SentenceTransformer(MODEL_NAME)


class EmbedRequest(BaseModel):
    texts: list[str] = Field(..., min_length=1)


class EmbedResponse(BaseModel):
    embeddings: list[list[float]]


@app.post("/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest) -> EmbedResponse:
    embeddings = model.encode(request.texts, normalize_embeddings=True)
    return EmbedResponse(embeddings=embeddings.tolist())