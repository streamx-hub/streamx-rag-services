# RAG Edge Service

Service responsible for ingestion to **EmbeddingStore** and serving Chat AI endpoint.
The **OpenAiRagSink** consumes **embed-events** channel and stores embeddings in EmbeddingStore.

## Configuration Reference

### variables
| Variable                                                 | Required    | Default   | Description                   |
|----------------------------------------------------------| ----------- | --------- |-------------------------------|
| `STREAMX_OPENAI_API_KEY`                                 | ✅ always    | —         | OpenAI API key                |
| `POSTGRES_USER`                                          | ✅ always    | —         | PostgeSQL db user             |
| `POSTGRES_PASSWORD`                                      | ✅ always    | —         | PostgeSQL db user password    |
| `streamx.hub.openai-rag-sink.chat-profile.name`          | ✅ always    | `default` | Chat profile environment name |
| `streamx.hub.openai-rag-sink.chat-profile.system-prompt` | ✅ always    | —         | Chat profile system prompt    |
| `streamx.hub.openai-rag-sink.chat-profile.display-name`  | recommended | —         | Chat profile display name     |

### Example endpoint call
git 
```bash
curl -X POST http://localhost/api/chat \
     -H "Content-Type: application/json" \
     -d '{
       "question":    "How do I return a product?"
     }'
```
