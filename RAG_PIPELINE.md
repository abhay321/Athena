# Project Athena: Retrieval-Augmented Generation (RAG) Architecture

This document outlines the architectural plan, data processing flows, and pipeline stages for the **Retrieval-Augmented Generation (RAG)** engine powering Project Athena. The pipeline is designed for a hybrid (on-device local first / cloud-augmented) context-aware chat system.

---

## High-Level RAG Architecture

```
                  [ Capture Screen / Ingestion ]
                                |
                                v
                     [ 1. Document Chunking ]
                    (Sliding Window / Paragraphs)
                                |
                                v
                     [ 2. Embedding Generator ]
                 (Local Nomic / Remote Gemini Embed)
                                |
                                v
                      [ 3. Vector Database ]
                  (Local SQLite Vector / Room DB)
                                |
                     +----------+----------+
                     |                     | (User Query)
                     v                     v
            [ Semantic Index ]    [ Lexical FTS Index ]
                     |                     |
                     +----------+----------+
                                |
                                v
                     [ 4. Reciprocal Rank Fusion ]
                     (Context Chunk De-duplication)
                                |
                                v
                     [ 5. Prompt Formatter ]
                     (Injected System Guidelines)
                                |
                                v
                   [ 6. Chat Generation Model ]
                  (Gemini 3.5 Flash Inference)
                                |
                                v
                       [ High-Contrast UI ]
```

---

## 1. Document Chunking & Ingestion Strategy

Raw text extracted from whiteboard sessions, book pages, slide scans, or meeting minutes is often too large to be fed directly into standard LLM prompts due to cost, latency, and attentional dilution (the "lost in the middle" effect).

*   **Boundary Detection:** Text is segmented by semantic breaks (paragraphs `\n\n` or structural headers).
*   **Chunk Parameters:**
    *   **Chunk Size:** 512 characters (~100-150 words) to preserve fine-grained facts.
    *   **Overlap Window:** 128 characters (~25 words) to ensure sentence fragments spanning boundaries are not semantically truncated.
*   **Metadata Enrichment:** Every generated chunk inherits critical context properties to prevent semantic drift:
    ```json
    {
      "chunk_id": "doc_123_chunk_02",
      "parent_doc_id": "doc_123",
      "source_title": "Distributed Chat Architecture",
      "category": "Whiteboard",
      "created_at": 1783818582.42,
      "text": "[Chunk Content text...]"
    }
    ```

---

## 2. Dynamic Embedding Layer

Project Athena supports swappable embedding generators based on the network connection and latency requirements:

*   **On-Device Offline Mode:** Employs a lightweight, quantized **Nomic Embed Text v1.5** model executing locally, generating dense float vectors of **768 dimensions**.
*   **Cloud Active Mode:** Connects to the **Gemini Text Embedding API** (`text-embedding-004`), yielding high-precision **768 or 1536-dimensional** vectors.
*   **Normalization:** All output vectors undergo absolute L2 normalization prior to insertion, enabling extremely fast cosine similarity search using standard dot-product operations:
    $$\text{Cosine Similarity}(A, B) = A \cdot B$$

---

## 3. Storage & Retrieval Strategy

Retrieval speed and accuracy are enhanced via a hybrid search architecture combining keyword matches and semantic spaces:

*   **Semantic Search:** Normalized float vectors are stored in an on-device SQLite table equipped with a custom cosine distance index.
*   **Lexical Search:** Raw documents are concurrently mirrored to a standard **SQLite FTS5 (Full-Text Search)** virtual index to guarantee 100% accurate recall for specific keywords, variable names, or acronyms (e.g., "Kafka", "GGUF").
*   **Reciprocal Rank Fusion (RRF):** Results from the semantic vector match and lexical search are interleaved and re-ranked using reciprocal rank scoring to form a consolidated top-N list of the most relevant chunks.

---

## 4. Prompt Engineering & Grounding Context

The top retrieved document chunks are formatted into a clean, system-directed multi-turn prompt payload. 

### System Instruction Constraints
To prevent hallucinations and guarantee professional, high-fidelity responses, the model is bound by the following instructions:
*   *Grounding:* Synthesize answers strictly using the attached `<context_documents>`. If the information cannot be found or reasonably inferred from the context, state: "I do not have access to that information in my current second brain index."
*   *Source Citation:* Explicitly reference parent document titles when citing statements (e.g., *"According to the 'Distributed Chat Architecture' whiteboard notes..."*).
*   *Action Cards:* Detect items written as `[ ]` tasks in the context and format them as interactive action item checklists.

### Prompt Format Structure
```xml
<system_instructions>
You are Athena, the user's secondary cognitive brain assistant. 
Synthesize clear, grounded answers using the contexts provided.
</system_instructions>

<context_documents>
  <doc title="Distributed Chat Architecture" category="Whiteboard">
    - Active Users cache: Redis Cluster storing mapping of user_id -> active_gateway_ip.
    - Main Database: CockroachDB (distributed transactions).
  </doc>
</context_documents>

<conversation_history>
  <turn role="user">Where do we store active session mappings?</turn>
  <turn role="model">Active session mappings are cached in a Redis Cluster...</turn>
</conversation_history>

<user_query>
Can you summarize our choice of database and queue from the whiteboard?
</user_query>
```

---

## 5. Multi-turn Chat Completion

*   **Model Selection:** **Gemini 3.5 Flash** serves as the default inference orchestrator due to its near-instantaneous response times, low token costs, and high reasoning fidelity.
*   **Streaming Delivery:** Completed tokens are streamed back to the UI state flows in real-time, allowing the user to begin reading answers instantly without experiencing network latency freezes.
