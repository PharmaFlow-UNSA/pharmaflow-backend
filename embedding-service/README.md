# PharmaFlow Embedding Service

Small FastAPI service for generating normalized sentence embeddings used by
`smart-features-service`.

## Run Locally

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

The first startup downloads `sentence-transformers/all-MiniLM-L6-v2`.

When `smart-features-service` starts locally, it can also create this `.venv`,
install `requirements.txt`, and launch this service automatically. That behavior
is controlled by `SMARTFEATURES_EMBEDDING_SERVICE_AUTO_START` and defaults to
enabled. After the model is cached, smart-features launches this service with
`EMBEDDING_MODEL_LOCAL_FILES_ONLY=true` to avoid network checks on every boot.

## API

`GET /health`

```json
{
  "status": "UP"
}
```

`POST /embed`

```json
{
  "texts": ["Can I upload my prescription as a PDF?"]
}
```

```json
{
  "embeddings": [[0.0123, -0.0456]]
}
```

Embeddings are normalized and have 384 dimensions.
