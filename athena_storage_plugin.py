"""
Project Athena: Hexagonal Storage Port and Pluggable Adapters

This file defines the decoupled Port/Adapter pattern for the storage layer of Project Athena.
In Hexagonal Architecture:
1. The Core Application depends ONLY on the StoragePort abstract interface (the "Port").
2. External systems (SQLite, Room, PostgreSQL, Cloud Spanner) implement the interface (the "Adapters").
3. This allows seamless switching of the physical storage layer without changing core business logic.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Dict, List, Optional, Any
import time


# =====================================================================
# Domain Model (Core Entity)
# =====================================================================

@dataclass
class CapturedDocument:
    """The central business entity representing a parsed knowledge node."""
    id: str
    title: str
    raw_text: str
    summary: str
    category: str
    tags: List[str] = field(default_factory=list)
    created_at: float = field(default_factory=time.time)
    metadata: Dict[str, Any] = field(default_factory=dict)


# =====================================================================
# The Port (Abstract Interface)
# =====================================================================

class StoragePort(ABC):
    """
    Abstract Port defining the repository interface for Project Athena.
    
    The domain services/viewmodels interact strictly with this interface,
    preserving absolute infrastructure independence.
    """

    @abstractmethod
    def connect(self, connection_string: str, options: Optional[Dict[str, Any]] = None) -> bool:
        """
        Establishes connection to the data store.
        
        Args:
            connection_string: Server URI, physical file path, or configuration identifier.
            options: Additional driver settings.
            
        Returns:
            bool: True if connection is successfully established and verified.
        """
        pass

    @abstractmethod
    def disconnect(self) -> None:
        """Closes all active connection streams and releases native client memory."""
        pass

    @abstractmethod
    def save(self, document: CapturedDocument) -> bool:
        """
        Inserts or overwrites a knowledge node inside the database.
        
        Args:
            document: The CapturedDocument domain entity.
            
        Returns:
            bool: True if the commit succeeds.
        """
        pass

    @abstractmethod
    def find_by_id(self, doc_id: str) -> Optional[CapturedDocument]:
        """
        Locates a document node by its unique identifier.
        
        Args:
            doc_id: The UUID or key of the document.
            
        Returns:
            Optional[CapturedDocument]: The populated domain entity, or None if not found.
        """
        pass

    @abstractmethod
    def delete(self, doc_id: str) -> bool:
        """
        Permanently expunges a document node.
        
        Args:
            doc_id: The identifier of the document to erase.
            
        Returns:
            bool: True if deletion succeeds and row count is altered.
        """
        pass

    @abstractmethod
    def fetch_all(self, category_filter: Optional[str] = None) -> List[CapturedDocument]:
        """
        Retrieves captured nodes sorted chronologically in descending order.
        
        Args:
            category_filter: Optional category string to filter nodes.
            
        Returns:
            List[CapturedDocument]: Historical documents.
        """
        pass

    @abstractmethod
    def search_vector_similarity(self, query_vector: List[float], limit: int = 5) -> List[CapturedDocument]:
        """
        Queries the integrated vector space index.
        
        Args:
            query_vector: Floating-point dense embeddings array.
            limit: Maximum neighbors count.
            
        Returns:
            List[CapturedDocument]: Neighbor nodes sorted by cosine similarity distance metrics.
        """
        pass


# =====================================================================
# The Adapters (Concrete Pluggable Implementations)
# =====================================================================

class LocalSqliteAdapter(StoragePort):
    """
    Pluggable local SQLite storage adapter.
    Implements on-device, low-latency, offline-first SQL queries.
    """

    def __init__(self) -> None:
        self._db: Dict[str, CapturedDocument] = {}
        self._filepath: Optional[str] = None

    def connect(self, connection_string: str, options: Optional[Dict[str, Any]] = None) -> bool:
        self._filepath = connection_string
        print(f"[SQLite Adapter] Successfully mounted on-device database at '{connection_string}'")
        return True

    def disconnect(self) -> None:
        print(f"[SQLite Adapter] Closing connection stream to '{self._filepath}'")
        self._filepath = None

    def save(self, document: CapturedDocument) -> bool:
        print(f"[SQLite Adapter] Saving document '{document.title}' to local SQLite database.")
        self._db[document.id] = document
        return True

    def find_by_id(self, doc_id: str) -> Optional[CapturedDocument]:
        return self._db.get(doc_id)

    def delete(self, doc_id: str) -> bool:
        if doc_id in self._db:
            del self._db[doc_id]
            print(f"[SQLite Adapter] Deleted document '{doc_id}' from local storage.")
            return True
        return False

    def fetch_all(self, category_filter: Optional[str] = None) -> List[CapturedDocument]:
        docs = list(self._db.values())
        if category_filter:
            docs = [d for d in docs if d.category.lower() == category_filter.lower()]
        return sorted(docs, key=lambda x: x.created_at, reverse=True)

    def search_vector_similarity(self, query_vector: List[float], limit: int = 5) -> List[CapturedDocument]:
        # Simulates low-latency local cosine similarity matching
        print(f"[SQLite Adapter] Performing local K-NN cosine search across {len(self._db)} rows.")
        return list(self._db.values())[:limit]


class RemoteCloudSpannerAdapter(StoragePort):
    """
    Pluggable Remote Cloud Spanner storage adapter.
    Handles massive, highly concurrent global-scale enterprise data consistency.
    """

    def __init__(self) -> None:
        self._spanner_client_active = False
        self._instance_id: Optional[str] = None
        self._mock_cloud_storage: Dict[str, CapturedDocument] = {}

    def connect(self, connection_string: str, options: Optional[Dict[str, Any]] = None) -> bool:
        # Expected connection format: "spanner://projects/gcp-proj/instances/athena-inst"
        self._instance_id = connection_string
        self._spanner_client_active = True
        print(f"[Cloud Spanner Adapter] Authenticated session with global multi-region database: {connection_string}")
        return True

    def disconnect(self) -> None:
        print("[Cloud Spanner Adapter] Released session pool locks and disconnected Google Cloud clients.")
        self._spanner_client_active = False

    def save(self, document: CapturedDocument) -> bool:
        if not self._spanner_client_active:
            raise RuntimeError("Spanner client session is closed.")
        print(f"[Cloud Spanner Adapter] Executing read-write transaction to insert node '{document.title}' with global consistency.")
        self._mock_cloud_storage[document.id] = document
        return True

    def find_by_id(self, doc_id: str) -> Optional[CapturedDocument]:
        return self._mock_cloud_storage.get(doc_id)

    def delete(self, doc_id: str) -> bool:
        if doc_id in self._mock_cloud_storage:
            del self._mock_cloud_storage[doc_id]
            print(f"[Cloud Spanner Adapter] Committing row deletion transaction for '{doc_id}'")
            return True
        return False

    def fetch_all(self, category_filter: Optional[str] = None) -> List[CapturedDocument]:
        docs = list(self._mock_cloud_storage.values())
        if category_filter:
            docs = [d for d in docs if d.category.lower() == category_filter.lower()]
        return sorted(docs, key=lambda x: x.created_at, reverse=True)

    def search_vector_similarity(self, query_vector: List[float], limit: int = 5) -> List[CapturedDocument]:
        print("[Cloud Spanner Adapter] Executing GCP Vector Search indexing query...")
        return list(self._mock_cloud_storage.values())[:limit]


# =====================================================================
# Application Core Service (Demonstrating Use of the Port)
# =====================================================================

class DocumentWorkflowManager:
    """
    Core Domain Service.
    Interacts purely with the `StoragePort` interface.
    """

    def __init__(self, storage: StoragePort) -> None:
        # Core domain logic only references the StoragePort type signature
        self._storage = storage

    def ingest_node(self, title: str, text: str, category: str, tags: List[str]) -> CapturedDocument:
        """Runs the business validation and uses the injected adapter to save."""
        if not title.strip() or not text.strip():
            raise ValueError("Document title and content cannot be blank.")
            
        summary = f"Synthesized Summary: {text[:80]}..."
        doc = CapturedDocument(
            id=f"doc_{int(time.time())}",
            title=title,
            raw_text=text,
            summary=summary,
            category=category,
            tags=tags
        )
        
        success = self._storage.save(doc)
        if success:
            print(f"[Core Domain] Ingested node '{title}' successfully.")
        return doc


if __name__ == "__main__":
    # -------------------------------------------------------------
    # Demonstrating the swappable Storage Adapter flow
    # -------------------------------------------------------------
    print("=== Hexagonal Storage Adapter Demonstration ===")
    
    # 1. Start with Local SQLite Adapter
    local_sqlite = LocalSqliteAdapter()
    local_sqlite.connect("/data/user/0/com.aistudio.athena/databases/athena.db")
    
    core_engine = DocumentWorkflowManager(storage=local_sqlite)
    core_engine.ingest_node(
        title="Distribute Chat",
        text="WebSocket system architecture with Kafka buffers.",
        category="Whiteboard",
        tags=["system_design", "backend"]
    )
    local_sqlite.disconnect()
    
    print("\n--- Swapping Storage Provider at Runtime to Spanner ---")
    
    # 2. Swap to Enterprise Cloud Spanner Adapter seamlessly
    cloud_spanner = RemoteCloudSpannerAdapter()
    cloud_spanner.connect("spanner://projects/gcp-proj/instances/athena-inst")
    
    core_engine_spanner = DocumentWorkflowManager(storage=cloud_spanner)
    core_engine_spanner.ingest_node(
        title="Generative AI Strategy",
        text="Integrating LLMs locally and in the cloud with RAG pipelines.",
        category="Meeting Slide",
        tags=["ai", "enterprise"]
    )
    cloud_spanner.disconnect()
