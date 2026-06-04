# syntax=docker/dockerfile:1
FROM python:3.11-slim
WORKDIR /app

ENV PIP_NO_CACHE_DIR=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# Install CPU-only torch first so sentence-transformers does NOT pull the multi-GB
# CUDA build. This is the single biggest image/memory optimization (Zadatak 2).
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Run as a non-root user and keep the model cache in its home dir
RUN useradd --create-home appuser
ENV HF_HOME=/home/appuser/.cache/huggingface \
    SENTENCE_TRANSFORMERS_HOME=/home/appuser/.cache/sentence_transformers
USER appuser

# Pre-download the MiniLM model at build time so the container is self-contained
# and the first request is fast (no cold download).
RUN python -c "from sentence_transformers import SentenceTransformer; SentenceTransformer('sentence-transformers/all-MiniLM-L6-v2')"

COPY --chown=appuser:appuser app ./app

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
