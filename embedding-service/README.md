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

## API

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
