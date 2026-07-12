# Project Athena: PostgreSQL SQL Schema & Clean Architecture Specification

This document defines the production-ready PostgreSQL relational database schema for Project Athena, coupled with a **Clean Architecture** specification that isolates the relational model from database-agnostic core domain models.

---

## 1. Clean Architecture Model Separation

To prevent database implementation details (such as column constraints, PostgreSQL-specific data types, and ORM decorators) from leaking into our core business logic, we enforce a strict separation between the **Domain Layer** (pure entities) and the **Data/Infrastructure Layer** (database-specific schemas).

```
                      +------------------------------------------+
                      |               DOMAIN LAYER               |
                      |  - Encapsulates enterprise business rules|
                      |  - Pure, zero-dependency model objects    |
                      |  - Defines Repository Interfaces (Abst)  |
                      |                                          |
                      |   [Note]   [Document]   [Tag]  [Embedding]
                      +------------------------------------------+
                                           ^
                                           | Boundary Crossing via Mappers
                                           v
                      +------------------------------------------+
                      |         DATA / INFRASTRUCTURE LAYER      |
                      |  - Encapsulates database technology      |
                      |  - Relational mapping & query optimization|
                      |  - Implements Repository Interfaces      |
                      |                                          |
                      |    PostgreSQL Schemas | Mappers | SQL     |
                      +------------------------------------------+
```

### The Boundary Contract
- **Domain Layer:** Defines pure, raw domain objects (e.g., Python `dataclasses` or Kotlin standard classes) and abstract repository interfaces.
- **Infrastructure Layer:** Implements database connections, executes raw SQL / ORM operations, contains the PostgreSQL schema definitions, and utilizes dedicated **Mappers** to translate between database rows and domain objects on retrieval or insertion.

---

## 2. Database-Agnostic Core Domain Models

These models have **zero imports** referencing PostgreSQL, SQLAlchemy, pgvector, or any database-specific library. They are pure representations of our business entities.

### Python Domain Entities (`athena/domain/models.py`)

```python
from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional, Dict, Any
import uuid

@dataclass(frozen=True)
class Tag:
    id: uuid.UUID
    name: str
    created_at: datetime

@dataclass(frozen=True)
class Note:
    id: uuid.UUID
    title: str
    content: str
    created_at: datetime
    updated_at: datetime
    is_favorite: bool
    tags: List[Tag] = field(default_factory=list)
    metadata_fields: Dict[str, Any] = field(default_factory=dict)

@dataclass(frozen=True)
class Document:
    id: uuid.UUID
    title: str
    raw_text: str
    markdown_content: str
    image_uri: Optional[str]
    file_size_bytes: int
    mime_type: str
    category: str  # e.g., "Whiteboard", "Book Page", "Meeting Notes"
    summary: Optional[str]
    created_at: datetime
    updated_at: datetime
    tags: List[Tag] = field(default_factory=list)

@dataclass(frozen=True)
class Embedding:
    id: uuid.UUID
    vector: List[float]
    dimension: int
    entity_type: str  # "note" or "document"
    entity_id: uuid.UUID
    created_at: datetime
```

---

## 3. PostgreSQL SQL Schema (`schema.sql`)

This production DDL includes index configurations, primary key constraints, foreign key cascades, and uses the `pgvector` extension for efficient $O(N)$ and $O(\log N)$ vector similarity lookups on high-dimensional embeddings (e.g., 768 or 1536 dimensions from models like text-embedding-3-small/large).

```sql
-- =====================================================================
-- 1. Prerequisites and Extensions
-- =====================================================================

-- Enable pgvector for high-performance vector operations and distance indexes
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

-- =====================================================================
-- 2. Core Relational Tables
-- =====================================================================

-- A. Normalized Tags Table
CREATE TABLE tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL CHECK (char_length(trim(name)) > 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- B. Documents Table
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title VARCHAR(255) NOT NULL,
    raw_text TEXT NOT NULL,
    markdown_content TEXT NOT NULL,
    image_uri VARCHAR(1024),
    file_size_bytes BIGINT NOT NULL DEFAULT 0 CHECK (file_size_bytes >= 0),
    mime_type VARCHAR(100) NOT NULL DEFAULT 'text/plain',
    category VARCHAR(100) NOT NULL DEFAULT 'General',
    summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- C. Notes Table (Supports standalone textual notes or references to source documents)
CREATE TABLE notes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID REFERENCES documents(id) ON DELETE SET NULL, -- Optional source link
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    metadata_fields JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- =====================================================================
-- 3. High-Dimensional Vector Embeddings Table
-- =====================================================================

-- Custom enum representing the entity ownership type
CREATE TYPE embedding_source_type AS ENUM ('note', 'document');

CREATE TABLE embeddings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    entity_type embedding_source_type NOT NULL,
    note_id UUID REFERENCES notes(id) ON DELETE CASCADE,
    document_id UUID REFERENCES documents(id) ON DELETE CASCADE,
    -- Store 1536-dimensional vectors (standard for OpenAI, Gemini text-embedding-004 is 768)
    vector VECTOR(768) NOT NULL,
    dimension INTEGER NOT NULL CHECK (dimension IN (768, 1536)),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    -- Constraint enforcing that exactly one relation column is filled based on the type
    CONSTRAINT check_exclusive_owner CHECK (
        (entity_type = 'note' AND note_id IS NOT NULL AND document_id IS NULL) OR
        (entity_type = 'document' AND document_id IS NOT NULL AND note_id IS NULL)
    )
);

-- =====================================================================
-- 4. Many-to-Many Bridge / Junction Tables for Normalized Tags
-- =====================================================================

CREATE TABLE document_tags (
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (document_id, tag_id)
);

CREATE TABLE note_tags (
    note_id UUID NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
    tag_id UUID NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (note_id, tag_id)
);

-- =====================================================================
-- 5. Automated Updated-At Modification Trigger Actions
-- =====================================================================

CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER set_timestamp_documents
BEFORE UPDATE ON documents
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

CREATE TRIGGER set_timestamp_notes
BEFORE UPDATE ON notes
FOR EACH ROW
EXECUTE FUNCTION trigger_set_timestamp();

-- =====================================================================
-- 6. Indexes & Query Optimization Strategy
-- =====================================================================

-- Regular relational indexes for lookup speed
CREATE INDEX idx_documents_category ON documents(category);
CREATE INDEX idx_notes_is_favorite ON notes(is_favorite) WHERE is_favorite = TRUE;
CREATE INDEX idx_notes_metadata_gin ON notes USING gin (metadata_fields);

-- Junction table lookups
CREATE INDEX idx_document_tags_tag_id ON document_tags(tag_id);
CREATE INDEX idx_note_tags_tag_id ON note_tags(tag_id);

-- Polymorphic lookup indexes for Embeddings
CREATE INDEX idx_embeddings_note_id ON embeddings(note_id) WHERE note_id IS NOT NULL;
CREATE INDEX idx_embeddings_document_id ON embeddings(document_id) WHERE document_id IS NOT NULL;

-- High-performance HNSW indexes for cosine similarity metrics (pgvector)
-- HNSW builds a multi-layer graph. m=16, ef_construction=64 offers balanced build times and search precision.
CREATE INDEX idx_embeddings_vector_cosine ON embeddings USING hnsw (vector vector_cosine_ops);
```

---

## 4. Mapper & Repository Design (Infrastructure Layer)

To bridge the database-agnostic **Domain Entities** with raw **SQL mappings**, we use Python Mappers. This pattern allows us to query PostgreSQL and return pure, safe Domain objects.

### Interface Definition (`athena/domain/repositories.py`)

```python
from abc import ABC, abstractmethod
from typing import List, Optional
import uuid
from .models import Note, Document, Embedding

class NoteRepository(ABC):
    @abstractmethod
    def get_by_id(self, note_id: uuid.UUID) -> Optional[Note]:
        pass

    @abstractmethod
    def save(self, note: Note) -> None:
        pass

    @abstractmethod
    def search_similar_notes(self, query_vector: List[float], limit: int = 5) -> List[Note]:
        pass
```

### PostgreSQL Concrete Implementation (`athena/infrastructure/db_repository.py`)

```python
import json
import psycopg2
from psycopg2.extras import RealDictCursor
from typing import List, Optional, Dict, Any
import uuid
from datetime import datetime

from athena.domain.models import Note, Tag
from athena.domain.repositories import NoteRepository

class PostgresNoteMapper:
    """
    Translates raw SQL query result records directly into domain models,
    and formats domain attributes for secure insertion statements.
    """

    @staticmethod
    def to_domain(row: Dict[str, Any], tags_rows: List[Dict[str, Any]]) -> Note:
        # Construct isolated Tag domain models
        tags = [
            Tag(id=t["id"], name=t["name"], created_at=t["created_at"])
            for t in tags_rows
        ]
        
        # Build pure business entity
        return Note(
            id=uuid.UUID(str(row["id"])),
            title=row["title"],
            content=row["content"],
            created_at=row["created_at"],
            updated_at=row["updated_at"],
            is_favorite=row["is_favorite"],
            tags=tags,
            metadata_fields=row["metadata_fields"] if isinstance(row["metadata_fields"], dict) else json.loads(row["metadata_fields"])
        )

    @staticmethod
    def to_persistence(note: Note) -> Dict[str, Any]:
        return {
            "id": str(note.id),
            "title": note.title,
            "content": note.content,
            "is_favorite": note.is_favorite,
            "metadata_fields": json.dumps(note.metadata_fields),
            "created_at": note.created_at,
            "updated_at": note.updated_at
        }


class PostgresNoteRepository(NoteRepository):
    """
    Concrete implementation of the NoteRepository interface using raw connection pooling.
    This infrastructure code has no connection to domain state boundaries.
    """
    
    def __init__(self, connection_pool) -> None:
        self.pool = connection_pool

    def get_by_id(self, note_id: uuid.UUID) -> Optional[Note]:
        conn = self.pool.getconn()
        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                # 1. Fetch note row
                cur.execute("SELECT * FROM notes WHERE id = %s", (str(note_id),))
                note_row = cur.fetchone()
                if not note_row:
                    return None

                # 2. Fetch linked tags through junction join table
                cur.execute(
                    "SELECT t.* FROM tags t "
                    "JOIN note_tags nt ON t.id = nt.tag_id "
                    "WHERE nt.note_id = %s", 
                    (str(note_id),)
                )
                tags_rows = cur.fetchall()

                # 3. Assemble and return Domain object
                return PostgresNoteMapper.to_domain(note_row, tags_rows)
        finally:
            self.pool.putconn(conn)

    def save(self, note: Note) -> None:
        conn = self.pool.getconn()
        try:
            with conn.cursor() as cur:
                data = PostgresNoteMapper.to_persistence(note)
                
                # Perform atomic upsert using PostgreSQL ON CONFLICT clause
                cur.execute(
                    "INSERT INTO notes (id, title, content, is_favorite, metadata_fields, created_at, updated_at) "
                    "VALUES (%(id)s, %(title)s, %(content)s, %(is_favorite)s, %(metadata_fields)s, %(created_at)s, %(updated_at)s) "
                    "ON CONFLICT (id) DO UPDATE SET "
                    "title = EXCLUDED.title, content = EXCLUDED.content, is_favorite = EXCLUDED.is_favorite, "
                    "metadata_fields = EXCLUDED.metadata_fields, updated_at = EXCLUDED.updated_at",
                    data
                )
                
                # Re-sync associated tag bindings
                cur.execute("DELETE FROM note_tags WHERE note_id = %s", (str(note.id),))
                for tag in note.tags:
                    cur.execute(
                        "INSERT INTO note_tags (note_id, tag_id) VALUES (%s, %s)",
                        (str(note.id), str(tag.id))
                    )
                conn.commit()
        except Exception:
            conn.rollback()
            raise
        finally:
            self.pool.putconn(conn)

    def search_similar_notes(self, query_vector: List[float], limit: int = 5) -> List[Note]:
        """
        Executes a cosine similarity vector search query utilizing the HNSW index on embeddings.
        Translates records back to the pure domain note collection.
        """
        conn = self.pool.getconn()
        try:
            with conn.cursor(cursor_factory=RealDictCursor) as cur:
                # pgvector syntax `<=>` represents cosine distance. Sorting ascending returns the closest vectors.
                cur.execute(
                    "SELECT n.*, (1 - (e.vector <=> %s::vector)) as similarity_score "
                    "FROM notes n "
                    "JOIN embeddings e ON n.id = e.note_id "
                    "ORDER BY e.vector <=> %s::vector "
                    "LIMIT %s",
                    (query_vector, query_vector, limit)
                )
                rows = cur.fetchall()
                
                results = []
                for row in rows:
                    # Retrieve matching tags for each result record
                    cur.execute(
                        "SELECT t.* FROM tags t JOIN note_tags nt ON t.id = nt.tag_id WHERE nt.note_id = %s",
                        (str(row["id"]),)
                    )
                    tags_rows = cur.fetchall()
                    results.append(PostgresNoteMapper.to_domain(row, tags_rows))
                
                return results
        finally:
            self.pool.putconn(conn)
```

---

## 5. Performance SQL Operations & Vector Similarity

The following queries showcase typical high-performance access paths required by Project Athena's offline-to-cloud synchronization and semantic query engine.

### A. Cosmic Search (Pure Semantic Query)
Fetch document segments closest in concept to a user prompt, returning the similarity metric score ($1.0$ being an exact semantic match):

```sql
SELECT 
    d.id, 
    d.title, 
    d.category,
    (1 - (e.vector <=> '[0.012, -0.045, 0.089, ... 768 elements]'::vector)) AS score
FROM documents d
JOIN embeddings e ON d.id = e.document_id
WHERE e.entity_type = 'document'
ORDER BY e.vector <=> '[0.012, -0.045, 0.089, ... 768 elements]'::vector
LIMIT 5;
```

### B. Hybrid Search (Semantic Lookup + Relational Metadata + Tag Constraints)
Retrieve notes that are highly similar to a query vector, but restricted to the *'System Architecture'* category tag, which are flagged as a *'favorite'*:

```sql
SELECT 
    n.id, 
    n.title, 
    (1 - (e.vector <=> '[0.012, -0.045, 0.089, ... 768 elements]'::vector)) AS score
FROM notes n
JOIN embeddings e ON n.id = e.note_id
JOIN note_tags nt ON n.id = nt.note_id
JOIN tags t ON nt.tag_id = t.id
WHERE n.is_favorite = TRUE
  AND t.name = 'System Architecture'
ORDER BY e.vector <=> '[0.012, -0.045, 0.089, ... 768 elements]'::vector
LIMIT 3;
```
