"""
Project Athena: Configuration-Driven Dynamic Plugin Loader

This module implements a dynamic plugin loading system that reads a configuration file 
(JSON format) and dynamically imports and registers swappable OCR, AI, and Storage 
modules at runtime using Python's `importlib`.
"""

import json
import os
import sys
import importlib
from typing import Dict, Any, Optional, Type

# Import the abstract ports and registry definitions from our core plugins module
from athena_plugins import (
    AthenaPluginRegistry,
    BasePlugin,
    OcrPlugin,
    LanguageModelPlugin,
    StoragePlugin
)


class ConfigurationPluginLoader:
    """
    Handles dynamic discovery, validation, and construction of swappable plugins
    based on a central JSON configuration.
    """

    def __init__(self, config_path: str, registry: Optional[AthenaPluginRegistry] = None) -> None:
        self.config_path = config_path
        self.registry = registry or AthenaPluginRegistry()
        self.config_data: Dict[str, Any] = {}

    def load_configuration(self) -> Dict[str, Any]:
        """Loads and parses the JSON configuration file."""
        if not os.path.exists(self.config_path):
            raise FileNotFoundError(f"Configuration file not found at: {self.config_path}")
        
        with open(self.config_path, 'r', encoding='utf-8') as f:
            self.config_data = json.load(f)
        return self.config_data

    def _import_class(self, module_name: str, class_name: str) -> Type[Any]:
        """
        Dynamically imports a module and retrieves the class reference.
        
        Args:
            module_name: Fully qualified module name (e.g. "plugins.google_ml_kit_plugin")
            class_name: Class name to import (e.g. "GoogleMlKitOcrPlugin")
            
        Returns:
            Type[Any]: Class constructor reference
        """
        try:
            # Ensure the current directory is in the sys.path so local folder structures are visible
            current_dir = os.getcwd()
            if current_dir not in sys.path:
                sys.path.insert(0, current_dir)

            # Dynamically import the Python module
            module = importlib.import_module(module_name)
            
            # Retrieve the class attribute from the module
            if not hasattr(module, class_name):
                raise AttributeError(f"Module '{module_name}' has no class named '{class_name}'")
                
            return getattr(module, class_name)
        except ImportError as e:
            raise ImportError(f"Failed to dynamically import module '{module_name}': {e}")

    def _instantiate_and_verify(
        self, 
        cls: Type[Any], 
        expected_type: Type[Any], 
        config: Dict[str, Any]
    ) -> Any:
        """
        Instantiates a plugin class, verifies it adheres to standard inheritance 
        contracts, and runs its lifecycle initialization method.
        """
        # 1. Verify inheritance signature
        if not issubclass(cls, expected_type):
            raise TypeError(
                f"Class '{cls.__name__}' is invalid. "
                f"Must inherit from abstract contract '{expected_type.__name__}'."
            )
            
        # 2. Instantiate the plugin (constructor takes zero arguments by convention)
        plugin_instance = cls()
        
        # 3. Perform contract initialization passing config parameters
        success = plugin_instance.initialize(config)
        if not success:
            raise RuntimeError(f"Plugin '{cls.__name__}' failed during lifecycle initialization.")
            
        return plugin_instance

    def load_and_register_all(self) -> AthenaPluginRegistry:
        """
        Parses all sections of the config file, dynamically instantiates 
        the active providers, and populates the system registry.
        """
        if not self.config_data:
            self.load_configuration()

        # Parse OCR configurations
        ocr_section = self.config_data.get("ocr", {})
        active_ocr_key = ocr_section.get("active")
        ocr_providers = ocr_section.get("providers", {})
        
        for key, info in ocr_providers.items():
            # Only load the active plugin or simulate registry for pre-compiled extensions
            is_active = (key == active_ocr_key)
            if is_active:
                print(f"[Loader] Loading ACTIVE OCR Provider: {key}...")
                try:
                    cls = self._import_class(info["module"], info["class"])
                    # Check if interface aligns with OcrPlugin or standard BasePlugin
                    plugin = self._instantiate_and_verify(cls, BasePlugin, info.get("config", {}))
                    self.registry.register_ocr(key, plugin)
                    self.registry.set_active_ocr(key)
                except Exception as e:
                    print(f"[Loader ERROR] Failed to load OCR plugin '{key}': {e}")
                    print("[Loader Info] Falling back to internal Athena Simulator for demonstration.")

        # Parse AI configurations
        ai_section = self.config_data.get("ai", {})
        active_ai_key = ai_section.get("active")
        ai_providers = ai_section.get("providers", {})

        for key, info in ai_providers.items():
            is_active = (key == active_ai_key)
            if is_active:
                print(f"[Loader] Loading ACTIVE AI Provider: {key}...")
                try:
                    cls = self._import_class(info["module"], info["class"])
                    plugin = self._instantiate_and_verify(cls, BasePlugin, info.get("config", {}))
                    self.registry.register_ai(key, plugin)
                    self.registry.set_active_ai(key)
                except Exception as e:
                    print(f"[Loader ERROR] Failed to load AI plugin '{key}': {e}")

        # Parse Storage configurations
        storage_section = self.config_data.get("storage", {})
        active_storage_key = storage_section.get("active")
        storage_providers = storage_section.get("providers", {})

        for key, info in storage_providers.items():
            is_active = (key == active_storage_key)
            if is_active:
                print(f"[Loader] Loading ACTIVE Storage Provider: {key}...")
                try:
                    cls = self._import_class(info["module"], info["class"])
                    # The storage adapters in athena_storage_plugin.py implement StoragePort directly
                    plugin_instance = cls()
                    # Connect database using supplied configuration
                    connection_str = info.get("config", {}).get("database_path") or info.get("config", {}).get("connection_string", "")
                    success = plugin_instance.connect(connection_str)
                    if success:
                        self.registry.register_storage(key, plugin_instance)
                        self.registry.set_active_storage(key)
                except Exception as e:
                    print(f"[Loader ERROR] Failed to load Storage plugin '{key}': {e}")

        return self.registry


# =====================================================================
# Main Execution / Demonstration Block
# =====================================================================

if __name__ == "__main__":
    print("=== Athena Dynamic Plugin Loader Console ===")
    
    # Define an on-the-fly demo file structure for plugins package to allow successful local importing
    os.makedirs("./plugins", exist_ok=True)
    with open("./plugins/__init__.py", "w") as f:
        pass

    # Create a quick local implementation of a mock Google ML Kit OCR plugin
    with open("./plugins/google_ml_kit_plugin.py", "w") as f:
        f.write('''\
from athena_plugins import OcrPlugin, PluginMetadata, OcrResult, OcrBlock
from typing import Dict, Any

class GoogleMlKitOcrPlugin(OcrPlugin):
    @property
    def metadata(self) -> PluginMetadata:
        return PluginMetadata(
            name="Google ML Kit OCR Plugin",
            version="1.0.0",
            author="Google AI Studio",
            description="On-device text scanner for Project Athena"
        )

    def initialize(self, config: Dict[str, Any]) -> bool:
        self.lang = config.get("language_code", "en")
        self.threshold = config.get("confidence_threshold", 0.45)
        print(f"[ML Kit Plugin] Initialized with lang={self.lang}, threshold={self.threshold}")
        return True

    def shutdown(self) -> None:
        print("[ML Kit Plugin] Shutting down.")

    def is_available(self) -> bool:
        return True

    def process_image(self, image_bytes: bytes, language: str = "en") -> OcrResult:
        print(f"[ML Kit Plugin] Scanning {len(image_bytes)} bytes of image buffer...")
        return OcrResult(
            raw_text="Extracted text from ML Kit",
            confidence=0.98,
            duration_seconds=0.15,
            engine_name="Google ML Kit"
        )
''')

    # Create a quick local implementation of a mock Gemini AI plugin
    with open("./plugins/gemini_ai_plugin.py", "w") as f:
        f.write('''\
from athena_plugins import LanguageModelPlugin, PluginMetadata, ChatMessage, Document
from typing import Dict, List, Any

class GeminiAiPlugin(LanguageModelPlugin):
    @property
    def metadata(self) -> PluginMetadata:
        return PluginMetadata(
            name="Gemini Cloud AI Plugin",
            version="2.0.0",
            author="Google DeepMind",
            description="Multimodal synthesis model plugin"
        )

    def initialize(self, config: Dict[str, Any]) -> bool:
        self.model = config.get("model_name", "gemini-3.5-flash")
        print(f"[Gemini Plugin] Connected to Google Cloud backend. Loaded model: {self.model}")
        return True

    def shutdown(self) -> None:
        print("[Gemini Plugin] Disposing sessions.")

    def generate_summary(self, raw_text: str) -> str:
        return f"[Gemini 3.5] Summary of: {raw_text[:50]}..."

    def classify_document(self, raw_text: str) -> Dict[str, Any]:
        return {
            "title": "Decoded Strategy Notes",
            "category": "Meeting Minutes",
            "tags": ["strategy", "planning"]
        }

    def chat_complete(self, history: List[ChatMessage], context_documents: List[Document]) -> str:
        return "Consolidated synthesized answer based on Athena contexts."
''')

    # Initialize the Configuration Loader
    loader = ConfigurationPluginLoader(config_path="./athena_plugins_config.json")
    registry = loader.load_and_register_all()

    print("\n--- Verification of Configured Active Engines ---")
    try:
        active_ocr = registry.get_ocr()
        print(f"Active OCR Engine: {active_ocr.metadata.name} (v{active_ocr.metadata.version})")
        ocr_out = active_ocr.process_image(b"MOCK_PNG_IMAGE_BYTES")
        print(f"Scan Outcome: '{ocr_out.raw_text}' (Confidence: {ocr_out.confidence * 100}%)")
    except Exception as e:
        print(f"OCR Verification failed: {e}")

    try:
        active_ai = registry.get_ai()
        print(f"Active AI Engine: {active_ai.metadata.name} (v{active_ai.metadata.version})")
        summary = active_ai.generate_summary("Enterprise AI Strategy Roadmap Q3-Q4 overview documents.")
        print(f"Synthesizer Out: {summary}")
    except Exception as e:
        print(f"AI Verification failed: {e}")

    try:
        active_storage = registry.get_storage()
        print(f"Active Storage Engine: {active_storage.__class__.__name__}")
    except Exception as e:
        print(f"Storage Verification failed: {e}")

    # Graceful shutdown of resources
    print("\nShutting down plugin registry...")
    registry.shutdown_all()
