# RAG Service

A production-grade **Retrieval-Augmented Generation (RAG)** microservice.
Responds in any language, streams answers token-by-token, and embeds as a one-liner Web Component on any website.

1. *streamx-rag-processing-service* - Processing service responsible for consuming data from **resources** channel and producing embeddings on **embed-events** channel.
2. *streamx-rag-edge-service* - Edge service responsible for ingestion to **EmbeddingStore** and serving Chat AI endpoint.