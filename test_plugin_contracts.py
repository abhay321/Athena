"""
Project Athena: Plugin Contract and Signature Enforcement Suite

This module provides a reusable, robust set of Pytest base classes and fixtures 
to enforce structural and semantic contracts for custom OCR and Language Model (AI) 
plugins. New plugin implementations must inherit and execute these test suites 
to guarantee they are drop-in compatible with the Project Athena lifecycle registry.
"""

from abc import ABC, abstractmethod
from typing import Dict, List, Any, Type
import pytest

from athena_plugins import (
    BasePlugin,
    OcrPlugin,
    LanguageModelPlugin,
    PluginMetadata,
    OcrResult,
    OcrBlock,
    OcrBoundingBox,
    ChatMessage,
    Document
)


# =====================================================================
# 1. Base Plugin Contract Test Suite (Lifecycle and Metadata)
# =====================================================================

class TestBasePluginContract(ABC):
    """
    Abstract contract test suite for the root BasePlugin interface.
    
    Any plugin test suite must inherit from this class and implement the
    `plugin_instance` fixture to verify general metadata and lifecycle.
    """

    @pytest.fixture
    @abstractmethod
    def plugin_instance(self) -> BasePlugin:
        """Must return an initialized instance of the BasePlugin being tested."""
        pass

    def test_metadata_signature_and_types(self, plugin_instance: BasePlugin) -> None:
        """Verify the plugin exposes a compliant, well-defined PluginMetadata structure."""
        metadata = plugin_instance.metadata
        
        assert isinstance(metadata, PluginMetadata), (
            f"Plugin '{type(plugin_instance).__name__}' metadata must return an instance of PluginMetadata."
        )
        assert isinstance(metadata.name, str) and len(metadata.name) > 0, "metadata.name must be a non-empty string."
        assert isinstance(metadata.version, str) and len(metadata.version) > 0, "metadata.version must be a non-empty string."
        assert isinstance(metadata.author, str), "metadata.author must be a string."
        assert isinstance(metadata.description, str), "metadata.description must be a string."
        assert isinstance(metadata.requires_network, bool), "metadata.requires_network must be a boolean."
        assert isinstance(metadata.config_schema, dict), "metadata.config_schema must be a dictionary configuration."

    def test_lifecycle_methods_presence_and_returns(self, plugin_instance: BasePlugin) -> None:
        """Verify that basic lifecycle initialization and shutdown methods conform to signatures."""
        # Test initialization with empty config dictionary
        init_result = plugin_instance.initialize({})
        assert isinstance(init_result, bool), (
            f"initialize() in '{type(plugin_instance).__name__}' must return a boolean value."
        )

        # Test shutdown gracefully handles resource release without throwing unhandled exceptions
        try:
            plugin_instance.shutdown()
        except Exception as e:
            pytest.fail(f"shutdown() in '{type(plugin_instance).__name__}' raised an unexpected exception: {e}")


# =====================================================================
# 2. OCR Plugin Contract Test Suite
# =====================================================================

class TestOcrPluginContract(TestBasePluginContract, ABC):
    """
    Abstract contract test suite for OcrPlugin implementations.
    
    Inherit from this test class to guarantee your OCR wrapper (e.g. Tesseract,
    ML-Kit, PaddleOCR) can be seamlessly registered at runtime.
    """

    @pytest.fixture
    @abstractmethod
    def plugin_instance(self) -> OcrPlugin:
        """Must return an initialized instance of the OcrPlugin subclass."""
        pass

    @pytest.fixture
    def sample_image_bytes(self) -> bytes:
        """A sample image byte array used for mock scanner processing."""
        return b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR"

    def test_is_available_contract(self, plugin_instance: OcrPlugin) -> None:
        """Verify that system/network accessibility check returns a pure boolean."""
        available = plugin_instance.is_available()
        assert isinstance(available, bool), (
            f"is_available() in '{type(plugin_instance).__name__}' must return a boolean."
        )

    def test_process_image_contract_and_types(
        self, 
        plugin_instance: OcrPlugin, 
        sample_image_bytes: bytes
    ) -> None:
        """Verify process_image executes character recognition and returns a compliant OcrResult."""
        result = plugin_instance.process_image(sample_image_bytes, language="en")

        # Verify output class
        assert isinstance(result, OcrResult), (
            f"process_image() in '{type(plugin_instance).__name__}' must return an OcrResult instance."
        )

        # Check required fields and type boundaries
        assert isinstance(result.raw_text, str), "OcrResult.raw_text must be a string."
        assert isinstance(result.confidence, float), "OcrResult.confidence must be a float."
        assert 0.0 <= result.confidence <= 1.0, f"OcrResult.confidence ({result.confidence}) must be between 0.0 and 1.0."
        assert isinstance(result.duration_seconds, float), "OcrResult.duration_seconds must be a float."
        assert result.duration_seconds >= 0.0, "OcrResult.duration_seconds must be a non-negative number."
        assert isinstance(result.engine_name, str) and len(result.engine_name) > 0, "OcrResult.engine_name must be a non-empty string."
        assert isinstance(result.blocks, list), "OcrResult.blocks must be a list."
        assert isinstance(result.success, bool), "OcrResult.success must be a boolean."

        if result.error_message is not None:
            assert isinstance(result.error_message, str), "OcrResult.error_message must be a string if populated."

        # Verify deep schema structure of layout blocks
        for index, block in enumerate(result.blocks):
            assert isinstance(block, OcrBlock), f"Block at index {index} must be an OcrBlock instance."
            assert isinstance(block.text, str), f"Block text at index {index} must be a string."
            assert isinstance(block.confidence, float), f"Block confidence at index {index} must be a float."
            assert 0.0 <= block.confidence <= 1.0, f"Block confidence ({block.confidence}) must be between 0.0 and 1.0."

            if block.bounding_box is not None:
                bbox = block.bounding_box
                assert isinstance(bbox, OcrBoundingBox), f"Block bounding_box at index {index} must be an OcrBoundingBox."
                assert isinstance(bbox.x, int), "OcrBoundingBox.x must be an integer."
                assert isinstance(bbox.y, int), "OcrBoundingBox.y must be an integer."
                assert isinstance(bbox.width, int) and bbox.width >= 0, "OcrBoundingBox.width must be a non-negative integer."
                assert isinstance(bbox.height, int) and bbox.height >= 0, "OcrBoundingBox.height must be a non-negative integer."


# =====================================================================
# 3. Language Model Plugin Contract Test Suite
# =====================================================================

class TestLanguageModelPluginContract(TestBasePluginContract, ABC):
    """
    Abstract contract test suite for LanguageModelPlugin implementations.
    
    Inherit from this test class to guarantee your AI wrapper (e.g. Gemini,
    Ollama, Anthropic) can handle summarization and semantic categorization.
    """

    @pytest.fixture
    @abstractmethod
    def plugin_instance(self) -> LanguageModelPlugin:
        """Must return an initialized instance of the LanguageModelPlugin subclass."""
        pass

    @pytest.fixture
    def sample_raw_text(self) -> str:
        """Sample text captured from whiteboards to simulate LLM context prompts."""
        return (
            "Distributed Key-Value Cache design.\n"
            "Consistent Hashing Ring allocates items across 4 main partition nodes.\n"
            "Virtual Nodes are instantiated to maintain uniform distribution under load."
        )

    @pytest.fixture
    def sample_chat_history(self) -> List[ChatMessage]:
        """A valid multi-turn chat interaction history."""
        return [
            ChatMessage(role="user", content="Explain partition balance in Consistent Hashing."),
            ChatMessage(role="model", content="Virtual nodes solve imbalances by distributing hash ranges evenly.")
        ]

    @pytest.fixture
    def sample_rag_documents(self) -> List[Document]:
        """A list of retrieved documents representing context databases."""
        return [
            Document(
                id="doc_cache_01",
                title="Whiteboard: Hashing Mechanics",
                raw_text="Consistency hashing guarantees minimal keys are remapped on cluster resize events.",
                summary="Introduction to dynamic consistent cluster hashing mechanics.",
                category="System Architecture",
                tags=["infrastructure", "scaling"]
            )
        ]

    def test_generate_summary_contract_and_types(
        self, 
        plugin_instance: LanguageModelPlugin, 
        sample_raw_text: str
    ) -> None:
        """Verify generate_summary takes string and returns structured summary."""
        summary = plugin_instance.generate_summary(sample_raw_text)
        
        assert isinstance(summary, str), "generate_summary() must return a string."
        assert len(summary) > 0, "generate_summary() should return a non-empty string for valid textual inputs."

    def test_classify_document_contract_and_types(
        self, 
        plugin_instance: LanguageModelPlugin, 
        sample_raw_text: str
    ) -> None:
        """Verify classify_document parses text and returns compliant semantic categorization schema."""
        classification = plugin_instance.classify_document(sample_raw_text)

        # Output must be a dictionary matching core expectation
        assert isinstance(classification, dict), "classify_document() must return a dictionary."
        
        # Verify schema presence
        assert "title" in classification, "Classification dict is missing required key: 'title'."
        assert "category" in classification, "Classification dict is missing required key: 'category'."
        assert "tags" in classification, "Classification dict is missing required key: 'tags'."

        # Verify key types
        assert isinstance(classification["title"], str), "classification['title'] must be a string."
        assert isinstance(classification["category"], str), "classification['category'] must be a string."
        assert isinstance(classification["tags"], list), "classification['tags'] must be a list."
        
        for idx, tag in enumerate(classification["tags"]):
            assert isinstance(tag, str), f"Tag at index {idx} in classification['tags'] must be a string."

    def test_chat_complete_contract_and_types(
        self, 
        plugin_instance: LanguageModelPlugin, 
        sample_chat_history: List[ChatMessage], 
        sample_rag_documents: List[Document]
    ) -> None:
        """Verify chat_complete implements multi-turn RAG conversation synthesis."""
        response = plugin_instance.chat_complete(sample_chat_history, sample_rag_documents)
        
        assert isinstance(response, str), "chat_complete() must return a string response."
        assert len(response) > 0, "chat_complete() should return a non-empty chat synthesis response."


# =====================================================================
# 4. Compliant and Non-Compliant Mocks (Self-Contained Verification)
# =====================================================================

# --- COMPLIANT PLUGINS ---

class CompliantOcrPlugin(OcrPlugin):
    """A completely compliant OCR implementation matching the contract."""
    
    @property
    def metadata(self) -> PluginMetadata:
        return PluginMetadata(
            name="Compliant OCR Mock",
            version="1.2.0",
            author="Athena Core Team",
            description="High fidelity test mock for OCR contracts.",
            requires_network=False,
            config_schema={"threshold": "float"}
        )

    def initialize(self, config: Dict[str, Any]) -> bool:
        return True

    def shutdown(self) -> None:
        pass

    def is_available(self) -> bool:
        return True

    def process_image(self, image_bytes: bytes, language: str = "en") -> OcrResult:
        bbox = OcrBoundingBox(x=10, y=20, width=150, height=45)
        block = OcrBlock(text="ATHENA ENGINE", confidence=0.99, bounding_box=bbox)
        return OcrResult(
            raw_text="ATHENA ENGINE",
            confidence=0.99,
            duration_seconds=0.05,
            engine_name="Compliant Mock Scanner",
            blocks=[block],
            success=True
        )


class CompliantLanguageModelPlugin(LanguageModelPlugin):
    """A completely compliant AI implementation matching the contract."""

    @property
    def metadata(self) -> PluginMetadata:
        return PluginMetadata(
            name="Compliant Gemini Mock",
            version="3.5.0",
            author="DeepMind",
            description="Synthesis and Chat Complete mock plugin for contract verification.",
            requires_network=True
        )

    def initialize(self, config: Dict[str, Any]) -> bool:
        return True

    def shutdown(self) -> None:
        pass

    def generate_summary(self, raw_text: str) -> str:
        return "Core Summary of whiteboards."

    def classify_document(self, raw_text: str) -> Dict[str, Any]:
        return {
            "title": "System Design Synthesis",
            "category": "Whiteboard Logs",
            "tags": ["testing", "athena"]
        }

    def chat_complete(self, history: List[ChatMessage], context_documents: List[Document]) -> str:
        return "Constituted synthesis based on context document inputs."


# --- NON-COMPLIANT PLUGINS (For proving contracts fail correctly) ---

class NonCompliantOcrPlugin(OcrPlugin):
    """
    Violates contract:
    - Returns a raw string instead of OcrResult.
    - Missing required configuration parameter validations.
    """
    @property
    def metadata(self) -> PluginMetadata:
        return PluginMetadata(name="NonCompliant OCR", version="0.1", author="Unknown", description="Fails")

    def initialize(self, config: Dict[str, Any]) -> bool:
        return True

    def shutdown(self) -> None:
        pass

    def is_available(self) -> bool:
        return True

    def process_image(self, image_bytes: bytes, language: str = "en") -> Any:
        # Invalid: Returns raw text string instead of OcrResult!
        return "Extracted raw string instead of OcrResult object"


# =====================================================================
# 5. Execute Tests for Compliant & Non-Compliant Modules
# =====================================================================

class TestCompliantOcrPlugin(TestOcrPluginContract):
    """Verifies that CompliantOcrPlugin successfully passes the contract test suite."""
    
    @pytest.fixture
    def plugin_instance(self) -> OcrPlugin:
        plugin = CompliantOcrPlugin()
        plugin.initialize({})
        return plugin


class TestCompliantLanguageModelPlugin(TestLanguageModelPluginContract):
    """Verifies that CompliantLanguageModelPlugin successfully passes the contract test suite."""
    
    @pytest.fixture
    def plugin_instance(self) -> LanguageModelPlugin:
        plugin = CompliantLanguageModelPlugin()
        plugin.initialize({})
        return plugin


def test_non_compliant_ocr_fails_contract(sample_image_bytes: bytes = b"RAW_IMAGE_BYTES") -> None:
    """
    Assures that the contract suite correctly flags and blocks non-compliant plugins.
    Ensures that any violation of the OcrResult signature fails early.
    """
    bad_plugin = NonCompliantOcrPlugin()
    bad_plugin.initialize({})
    
    # Asserting that standard contract validation asserts on returning incorrect types
    with pytest.raises(AssertionError, match="must return an OcrResult instance"):
        # Wrap the exact assertion from our contract base class
        result = bad_plugin.process_image(sample_image_bytes, language="en")
        assert isinstance(result, OcrResult), "process_image() must return an OcrResult instance."

if __name__ == "__main__":
    print("=== Executing Project Athena Contract Tests in Main Demonstration ===")
    pytest.main(["-v", __file__])
