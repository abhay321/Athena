"""
Project Athena: Extensible Plugin Architecture
This module defines the abstract base interfaces and core runtime registry 
for swappable OCR, AI (Language Models), and Storage modules. 
"""

from abc import ABC, abstractmethod
from typing import Dict, List, Optional, Any, Type, Iterator
from dataclasses import dataclass, field
import time


# =====================================================================
# 1. Core Metadata & Result Structures
# =====================================================================

@dataclass
class PluginMetadata:
    """Standardized metadata structure for all swappable plugins."""
    name: str
    version: str
    author: str
    description: str
    requires_network: bool = False
    config_schema: Dict[str, Any] = field(default_factory=dict)


@dataclass
class OcrBoundingBox:
    """Spatial rectangle coordinates for recognized text segments."""
    x: int
    y: int
    width: int
    height: int


@dataclass
class OcrBlock:
    """Granular piece of text extracted by an OCR engine with positional context."""
    text: str
    confidence: float
    bounding_box: Optional[OcrBoundingBox] = None


@dataclass
class OcrResult:
    """Unified container representing raw and layout-analyzed text."""
    raw_text: str
    confidence: float
    duration_seconds: float
    engine_name: str
    blocks: List[OcrBlock] = field(default_factory=list)
    success: bool = True
    error_message: Optional[str] = None


@dataclass
class ChatMessage:
    """Individual interaction turn in conversational contexts."""
    role: str  # "user", "model", "system"
    content: str


@dataclass
class Document:
    """Principal entity representing a knowledge node inside Athena."""
    id: str
    title: str
    raw_text: str
    summary: str
    category: str
    tags: List[str] = field(default_factory=list)
    created_at: float = field(default_factory=time.time)


# =====================================================================
# 2. Abstract Base Classes (The Plugins)
# =====================================================================

class BasePlugin(ABC):
    """The root abstract base class that all Project Athena plugins must implement."""

    @property
    @abstractmethod
    def metadata(self) -> PluginMetadata:
        """Returns the static information and configuration parameters of this plugin."""
        pass

    @abstractmethod
    def initialize(self, config: Dict[str, Any]) -> bool:
        """
        Initializes runtime assets, loads static weights, or authenticates credentials.
        Returns True if initialization completes successfully, False otherwise.
        """
        pass

    @abstractmethod
    def shutdown(self) -> None:
        """Performs graceful resource release, closes sessions, and frees native memory."""
        pass


class OcrPlugin(BasePlugin):
    """
    Abstract contract for OCR extraction. Allows implementations wrapping 
    Google ML Kit API, PaddleOCR, Tesseract, or cloud parsers.
    """

    @abstractmethod
    def is_available(self) -> bool:
        """Checks if native dynamic libraries, trained weights, or network endpoints are reachable."""
        pass

    @abstractmethod
    def process_image(self, image_bytes: bytes, language: str = "en") -> OcrResult:
        """
        Performs character extraction on raw image inputs.
        
        Args:
            image_bytes: Binary buffer containing raw visual data.
            language: Target ISO-639 language code for contextual dictionary lookups.
            
        Returns:
            OcrResult: Unified structured text data.
        """
        pass


class LanguageModelPlugin(BasePlugin):
    """
    Abstract contract for intelligence services. Allows swapping between local GGUF models 
    (via llama.cpp/Ollama) and remote APIs (Gemini 3.5, Anthropic, OpenAI).
    """

    @abstractmethod
    def generate_summary(self, raw_text: str) -> str:
        """Synthesizes deep knowledge nodes into concise bullet summaries."""
        pass

    @abstractmethod
    def classify_document(self, raw_text: str) -> Dict[str, Any]:
        """
        Extracts structured metadata, categories, and tags from unstructured text.
        
        Returns:
            A dictionary containing:
            - "title": Proposed document header
            - "category": Recommended semantic category (e.g. "Whiteboard")
            - "tags": List of string semantic labels
        """
        pass

    @abstractmethod
    def chat_complete(self, history: List[ChatMessage], context_documents: List[Document]) -> str:
        """
        Executes a RAG (Retrieval-Augmented Generation) chat completion turn.
        
        Args:
            history: Thread conversation history representing past turns.
            context_documents: Injected knowledge base documents matching query embeddings.
            
        Returns:
            str: Synthesized response message.
        """
        pass


class StoragePlugin(BasePlugin):
    """
    Abstract contract for physical and vector document storage. Allows swapping
    between local SQLite/Room, on-device vector spaces, or server-side databases (e.g. Spanner).
    """

    @abstractmethod
    def insert_document(self, doc: Document) -> bool:
        """Saves a Document into the physical database."""
        pass

    @abstractmethod
    def get_document(self, doc_id: str) -> Optional[Document]:
        """Retrieves a single Document node by its unique identifier."""
        pass

    @abstractmethod
    def delete_document(self, doc_id: str) -> bool:
        """Removes a Document node from storage."""
        pass

    @abstractmethod
    def list_documents(self) -> List[Document]:
        """Lists all captured document nodes in chronological order."""
        pass

    @abstractmethod
    def search_semantic(self, query_embedding: List[float], limit: int = 5) -> List[Document]:
        """
        Queries the vector index to retrieve close neighbors.
        
        Args:
            query_embedding: Floating-point dense representation of search text.
            limit: Maximum count of retrieved documents.
            
        Returns:
            List[Document]: Nodes sorted by cosine distance metric.
        """
        pass


# =====================================================================
# 3. Dynamic Plugin Registry & Lifecycle Manager
# =====================================================================

class AthenaPluginRegistry:
    """
    Central runtime hub of Project Athena's plugin system.
    Manages loading, activation, and swapping of modules without core modifications.
    """

    def __init__(self) -> None:
        self._ocr_plugins: Dict[str, OcrPlugin] = {}
        self._ai_plugins: Dict[str, LanguageModelPlugin] = {}
        self._storage_plugins: Dict[str, StoragePlugin] = {}

        self._active_ocr: Optional[str] = None
        self._active_ai: Optional[str] = None
        self._active_storage: Optional[str] = None

    def register_ocr(self, name: str, plugin: OcrPlugin) -> None:
        """Registers a new OCR module."""
        self._ocr_plugins[name] = plugin
        if not self._active_ocr:
            self._active_ocr = name

    def register_ai(self, name: str, plugin: LanguageModelPlugin) -> None:
        """Registers a new AI model interface."""
        self._ai_plugins[name] = plugin
        if not self._active_ai:
            self._active_ai = name

    def register_storage(self, name: str, plugin: StoragePlugin) -> None:
        """Registers a new storage driver."""
        self._storage_plugins[name] = plugin
        if not self._active_storage:
            self._active_storage = name

    # Swap active execution plugins
    def set_active_ocr(self, name: str) -> bool:
        if name in self._ocr_plugins:
            self._active_ocr = name
            return True
        return False

    def set_active_ai(self, name: str) -> bool:
        if name in self._ai_plugins:
            self._active_ai = name
            return True
        return False

    def set_active_storage(self, name: str) -> bool:
        if name in self._storage_plugins:
            self._active_storage = name
            return True
        return False

    # Get active implementations
    def get_ocr(self) -> OcrPlugin:
        if not self._active_ocr or self._active_ocr not in self._ocr_plugins:
            raise RuntimeError("No active OCR plugin registered.")
        return self._ocr_plugins[self._active_ocr]

    def get_ai(self) -> LanguageModelPlugin:
        if not self._active_ai or self._active_ai not in self._ai_plugins:
            raise RuntimeError("No active AI plugin registered.")
        return self._ai_plugins[self._active_ai]

    def get_storage(self) -> StoragePlugin:
        if not self._active_storage or self._active_storage not in self._storage_plugins:
            raise RuntimeError("No active Storage plugin registered.")
        return self._storage_plugins[self._active_storage]

    def shutdown_all(self) -> None:
        """Safely shuts down all loaded plugins."""
        for p in list(self._ocr_plugins.values()) + list(self._ai_plugins.values()) + list(self._storage_plugins.values()):
            try:
                p.shutdown()
            except Exception as e:
                print(f"Error shutting down plugin {p.metadata.name}: {e}")
