"""
Project Athena: Lightweight Asynchronous Event Bus Architecture

This module implements an Abstract Base Class (ABC) for a thread-safe, decoupled 
Event Bus. It facilitates non-blocking asynchronous communication between core 
subsystems (e.g., OCR, AI Pipeline, Storage) without tight coupling.
"""

from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Dict, List, Callable, Any, Type, Set
import time
import threading
from concurrent.futures import ThreadPoolExecutor


# =====================================================================
# 1. Base Event Definitions
# =====================================================================

@dataclass
class Event:
    """
    Base representation of an immutable domain message in the system.
    All custom events must inherit from this class.
    """
    event_type: str = field(init=False)
    timestamp: float = field(default_factory=time.time, init=False)

    def __post_init__(self) -> None:
        # Auto-resolve the event type from the class name for easier routing
        self.event_type = self.__class__.__name__


# Custom Domain Events
@dataclass
class ImageCapturedEvent(Event):
    """Fired when a raw visual asset or whiteboard screenshot is ingested."""
    image_id: str
    raw_bytes: bytes
    language: str = "en"


@dataclass
class OcrProcessedEvent(Event):
    """Fired when an OCR engine completes scanning and characters are segmented."""
    image_id: str
    extracted_text: str
    confidence: float
    duration_seconds: float


@dataclass
class AiPipelineCompletedEvent(Event):
    """Fired when a Language Model completes document categorization and summary."""
    document_id: str
    original_text: str
    summary: str
    category: str
    tags: List[str]


# =====================================================================
# 2. Event Bus Abstract Base Contract
# =====================================================================

EventHandler = Callable[[Any], None]

class EventBus(ABC):
    """
    Abstract Base Class defining the interface for Event subscription, 
    unsubscription, and asynchronous publishing.
    """

    @abstractmethod
    def subscribe(self, event_class: Type[Event], handler: EventHandler) -> None:
        """
        Registers a callback handler to listen for a specific Event class.
        
        Args:
            event_class: The concrete Event Type subclass to monitor.
            handler: Callback executable of signature (event: Event) -> None.
        """
        pass

    @abstractmethod
    def unsubscribe(self, event_class: Type[Event], handler: EventHandler) -> None:
        """
        Removes a callback handler registration for a specific Event class.
        """
        pass

    @abstractmethod
    def publish(self, event: Event) -> None:
        """
        Dispatches an Event asynchronously to all registered subscriber callbacks.
        
        Args:
            event: An instance of an Event subclass.
        """
        pass

    @abstractmethod
    def shutdown(self) -> None:
        """Gracefully tears down background queues, threads, or executors."""
        pass


# =====================================================================
# 3. Thread-Safe, Asynchronous Event Bus Implementation
# =====================================================================

class AsyncThreadPoolEventBus(EventBus):
    """
    A lightweight, thread-safe Event Bus that utilizes a ThreadPoolExecutor 
    to dispatch messages asynchronously to subscriber tasks.
    This guarantees that heavy pipelines (like AI inference or OCR processing) 
    do not block the main application interface thread.
    """

    def __init__(self, max_workers: int = 4) -> None:
        self._subscribers: Dict[Type[Event], Set[EventHandler]] = {}
        self._lock = threading.RLock() # Reentrant lock for concurrent access to the subscriber list
        self._executor = ThreadPoolExecutor(
            max_workers=max_workers, 
            thread_name_prefix="AthenaEventBusWorker"
        )
        self._is_active = True
        print(f"[EventBus] Initialized Thread Pool Event Bus with {max_workers} workers.")

    def subscribe(self, event_class: Type[Event], handler: EventHandler) -> None:
        if not issubclass(event_class, Event):
            raise TypeError("Can only subscribe to classes inheriting from 'Event'.")
            
        with self._lock:
            if event_class not in self._subscribers:
                self._subscribers[event_class] = set()
            self._subscribers[event_class].add(handler)
            print(f"[EventBus] Registered handler '{handler.__qualname__}' for '{event_class.__name__}'.")

    def unsubscribe(self, event_class: Type[Event], handler: EventHandler) -> None:
        with self._lock:
            if event_class in self._subscribers:
                self._subscribers[event_class].discard(handler)
                if not self._subscribers[event_class]:
                    del self._subscribers[event_class]
                print(f"[EventBus] Unsubscribed handler '{handler.__qualname__}' from '{event_class.__name__}'.")

    def publish(self, event: Event) -> None:
        """
        Publishes the event. Subscribers are executed asynchronously 
        inside background thread pool workers.
        """
        if not self._is_active:
            raise RuntimeError("Cannot publish event. Event Bus has been shut down.")

        event_class = type(event)
        handlers_to_trigger: List[EventHandler] = []

        # Safely copy handlers under lock to prevent concurrent modification exceptions
        with self._lock:
            if event_class in self._subscribers:
                handlers_to_trigger = list(self._subscribers[event_class])

        if not handlers_to_trigger:
            return

        # Submit each callback as an independent task to the pool
        for handler in handlers_to_trigger:
            self._executor.submit(self._safely_execute_handler, handler, event)

    def _safely_execute_handler(self, handler: EventHandler, event: Event) -> None:
        """Executes a handler, swallowing and isolating exceptions to protect the pipeline."""
        try:
            handler(event)
        except Exception as e:
            print(
                f"[EventBus ERROR] Exception raised in handler '{handler.__qualname__}' "
                f"while processing event '{event.event_type}': {e}"
            )

    def shutdown(self) -> None:
        """Gracefully shuts down the thread executor."""
        with self._lock:
            self._is_active = False
            self._subscribers.clear()
        print("[EventBus] Draining queued asynchronous operations and shutting down workers...")
        self._executor.shutdown(wait=True)
        print("[EventBus] Event Bus shutdown successfully completed.")


# =====================================================================
# 4. Decoupled Subsystem Modules (OCR, AI Pipeline, Persistence)
# =====================================================================

class OcrModule:
    """
    Decoupled subsystem that listens for raw raw image captures, performs
    OCR processing, and publishes extracted text.
    """
    def __init__(self, event_bus: EventBus) -> None:
        self.event_bus = event_bus
        # Connect listeners to relevant events
        self.event_bus.subscribe(ImageCapturedEvent, self.handle_image_captured)

    def handle_image_captured(self, event: ImageCapturedEvent) -> None:
        print(f"[OCR Module] Recieved '{event.event_type}' for ID '{event.image_id}' on thread '{threading.current_thread().name}'.")
        print(f"[OCR Module] Simulating text extraction on {len(event.raw_bytes)} bytes...")
        
        # Simulating heavy visual layout/text scanning latency
        time.sleep(0.5) 
        
        extracted_text = (
            "WHITEBOARD NOTES:\n"
            "Topic: Distributed Event-Driven Flow\n"
            "1. Client publishes ImageCapturedEvent\n"
            "2. OCR subscriber processes image bytes\n"
            "3. AI Pipeline generates summaries asynchronously"
        )
        
        # Construct and publish the outcome event
        processed_event = OcrProcessedEvent(
            image_id=event.image_id,
            extracted_text=extracted_text,
            confidence=0.97,
            duration_seconds=0.5
        )
        print(f"[OCR Module] Extraction finished. Publishing '{processed_event.event_type}'...")
        self.event_bus.publish(processed_event)


class AiPipelineModule:
    """
    Decoupled subsystem that listens to OCR completion events, applies LLM
    categorization and synthesis (e.g. Gemini), and publishes refined summaries.
    """
    def __init__(self, event_bus: EventBus) -> None:
        self.event_bus = event_bus
        # Connect listeners to OCR processed texts
        self.event_bus.subscribe(OcrProcessedEvent, self.handle_ocr_completed)

    def handle_ocr_completed(self, event: OcrProcessedEvent) -> None:
        print(f"[AI Pipeline] Recieved '{event.event_type}' on thread '{threading.current_thread().name}'.")
        print(f"[AI Pipeline] Analyzing context confidence: {event.confidence * 100}%...")
        
        # Simulating LLM response synthesis time (e.g., Gemini Flash model parsing)
        time.sleep(0.8)
        
        summary = "An architecture design representing decoupled, asynchronous event routing."
        category = "System Architecture"
        tags = ["event-driven", "pub-sub", "python", "athena"]
        
        completion_event = AiPipelineCompletedEvent(
            document_id=f"doc_{event.image_id}",
            original_text=event.extracted_text,
            summary=summary,
            category=category,
            tags=tags
        )
        print(f"[AI Pipeline] Summary & Metadata generated. Publishing '{completion_event.event_type}'...")
        self.event_bus.publish(completion_event)


class StorageLoggerModule:
    """
    Decoupled database sink that persists completed knowledge nodes to local indexes.
    """
    def __init__(self, event_bus: EventBus) -> None:
        self.event_bus = event_bus
        # Listens directly to final completions
        self.event_bus.subscribe(AiPipelineCompletedEvent, self.handle_pipeline_completed)

    def handle_pipeline_completed(self, event: AiPipelineCompletedEvent) -> None:
        print(f"[Storage Sink] Recieved '{event.event_type}' on thread '{threading.current_thread().name}'.")
        print(f"[Storage Sink] PERSISTING DOCUMENT INTO LOCAL VECTOR STORAGE:")
        print(f"  - Document ID: {event.document_id}")
        print(f"  - Category:    {event.category}")
        print(f"  - Tags:        {event.tags}")
        print(f"  - Summary:     '{event.summary}'")
        print("[Storage Sink] Document node successfully indexed in SQLite/Room DB.")


# =====================================================================
# 5. Pipeline Execution Demonstration
# =====================================================================

if __name__ == "__main__":
    print("=== Project Athena Event-Driven Architecture Test Console ===")
    
    # Initialize Core asynchronous broker
    bus = AsyncThreadPoolEventBus(max_workers=3)

    # Instantiate decoupled system actors (they wire themselves into the bus)
    ocr_actor = OcrModule(bus)
    ai_actor = AiPipelineModule(bus)
    storage_actor = StorageLoggerModule(bus)

    print("\n--- Simulating Live Mobile Capture (Triggering Flow) ---")
    
    # Simulate camera snapshot event from client UI (Main Application Thread context)
    capture_event = ImageCapturedEvent(
        image_id="snap_1783818582",
        raw_bytes=b"\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR..."
    )
    
    print(f"[Client UI] Camera button pressed. Publishing '{capture_event.event_type}' on main thread...")
    bus.publish(capture_event)
    
    print("[Client UI] Event published. UI thread is responsive! Waiting for background tasks to trickle in...\n")
    
    # Let threads complete their dynamic publish/subscribe cascade
    # Flow: Client -> ImageCaptured -> OCR -> OcrProcessed -> AI -> AiPipelineCompleted -> Storage
    time.sleep(2.0)

    print("\nShutting down Athena system...")
    bus.shutdown()
    print("Execution finalized successfully.")
