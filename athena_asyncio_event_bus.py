"""
Project Athena: Asynchronous asyncio-based Event Bus with asyncio.Queue

This module implements an asynchronous Event Bus utilizing Python's `asyncio`
standard library and `asyncio.Queue` for non-blocking message passing.
It enables fully cooperative multitasking between the OCR engine and the AI Pipeline.
"""

import asyncio
import time
from abc import ABC, abstractmethod
from dataclasses import dataclass, field
from typing import Dict, List, Callable, Any, Type, Set, Coroutine

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
# 2. Async Event Bus Abstract Base Contract
# =====================================================================

# Handlers are coroutine functions in this async implementation
AsyncEventHandler = Callable[[Any], Coroutine[Any, Any, None]]

class AsyncEventBus(ABC):
    """
    Abstract Base Class for an asyncio-driven asynchronous event bus.
    """

    @abstractmethod
    async def subscribe(self, event_class: Type[Event], handler: AsyncEventHandler) -> None:
        """
        Registers an async coroutine handler to monitor a specific Event class.
        
        Args:
            event_class: The concrete Event Type subclass to monitor.
            handler: Async coroutine function of signature: async def (event: Event) -> None
        """
        pass

    @abstractmethod
    async def unsubscribe(self, event_class: Type[Event], handler: AsyncEventHandler) -> None:
        """
        Removes an async coroutine handler registration for a specific Event class.
        """
        pass

    @abstractmethod
    async def publish(self, event: Event) -> None:
        """
        Publishes an event to the internal asyncio.Queue for background processing.
        
        Args:
            event: An instance of an Event subclass.
        """
        pass

    @abstractmethod
    async def start(self) -> None:
        """
        Starts background task loop processing of queue events.
        """
        pass

    @abstractmethod
    async def shutdown(self) -> None:
        """
        Gracefully drains the queue and stops background consumer workers.
        """
        pass


# =====================================================================
# 3. Concrete asyncio.Queue Event Bus Implementation
# =====================================================================

class AsyncioQueueEventBus(AsyncEventBus):
    """
    An asyncio-native Event Bus using an asyncio.Queue to serialize, buffer,
    and distribute event dispatches asynchronously to subscribers.
    """

    def __init__(self, queue_maxsize: int = 100) -> None:
        self._subscribers: Dict[Type[Event], Set[AsyncEventHandler]] = {}
        self._queue: asyncio.Queue[Event] = asyncio.Queue(maxsize=queue_maxsize)
        self._worker_task: asyncio.Task | None = None
        self._is_active = False

    async def subscribe(self, event_class: Type[Event], handler: AsyncEventHandler) -> None:
        if not issubclass(event_class, Event):
            raise TypeError("Can only subscribe to subclasses of Event.")
        if not asyncio.iscoroutinefunction(handler):
            raise TypeError("Handler must be an async coroutine function (async def).")

        if event_class not in self._subscribers:
            self._subscribers[event_class] = set()
        self._subscribers[event_class].add(handler)
        print(f"[AsyncBus] Subscribed '{handler.__name__}' to '{event_class.__name__}'.")

    async def unsubscribe(self, event_class: Type[Event], handler: AsyncEventHandler) -> None:
        if event_class in self._subscribers:
            self._subscribers[event_class].discard(handler)
            if not self._subscribers[event_class]:
                del self._subscribers[event_class]
            print(f"[AsyncBus] Unsubscribed '{handler.__name__}' from '{event_class.__name__}'.")

    async def publish(self, event: Event) -> None:
        if not self._is_active:
            raise RuntimeError("Event Bus is not started or has been shut down.")
        
        # Non-blocking insertion into the queue (yields/blocks only if queue is completely full)
        await self._queue.put(event)
        print(f"[AsyncBus] Published event '{event.event_type}' to queue (size: {self._queue.qsize()}).")

    async def start(self) -> None:
        """Starts the background event routing loop."""
        if self._is_active:
            return
        self._is_active = True
        self._worker_task = asyncio.create_task(self._event_loop_worker())
        print("[AsyncBus] Background queue processing worker started.")

    async def _event_loop_worker(self) -> None:
        """Main background task processing elements from asyncio.Queue."""
        while self._is_active or not self._queue.empty():
            try:
                # Retrieve next event from the queue
                event = await self._queue.get()
                
                event_class = type(event)
                handlers = list(self._subscribers.get(event_class, []))
                
                if handlers:
                    # Execute all subscribed handlers concurrently
                    tasks = [self._safely_execute_handler(h, event) for h in handlers]
                    await asyncio.gather(*tasks)
                
                self._queue.task_done()
            except asyncio.CancelledError:
                break
            except Exception as e:
                print(f"[AsyncBus Loop Error] {e}")

    async def _safely_execute_handler(self, handler: AsyncEventHandler, event: Event) -> None:
        """Executes a coroutine handler, catching exceptions to protect the running loop."""
        try:
            await handler(event)
        except Exception as e:
            print(f"[AsyncBus Handler Error] Exception in '{handler.__name__}' for '{event.event_type}': {e}")

    async def shutdown(self) -> None:
        print("[AsyncBus] Shutting down. Waiting for queue to drain...")
        self._is_active = False
        
        # Wait for remaining tasks in the queue to be completed
        if self._queue.qsize() > 0:
            await self._queue.join()
            
        if self._worker_task:
            self._worker_task.cancel()
            try:
                await self._worker_task
            except asyncio.CancelledError:
                pass
        print("[AsyncBus] System gracefully finalized.")


# =====================================================================
# 4. Decoupled Coroutine-Based System Modules
# =====================================================================

class AsyncOcrModule:
    """Decoupled OCR engine built with asyncio."""

    def __init__(self, event_bus: AsyncEventBus) -> None:
        self.event_bus = event_bus

    async def initialize(self) -> None:
        await self.event_bus.subscribe(ImageCapturedEvent, self.handle_image_captured)

    async def handle_image_captured(self, event: ImageCapturedEvent) -> None:
        print(f"[AsyncOCR] Received captured image '{event.image_id}'...")
        print("[AsyncOCR] Running optical character extraction (yielding to event loop)...")
        
        # Yield to let other tasks run while simulating image decoding/ML-Kit OCR latency
        await asyncio.sleep(0.4)
        
        extracted_text = (
            "ATHENA WORKSPACE:\n"
            "Active OCR Engine = Google ML Kit\n"
            "Active Chat Model = Gemini 3.5 Flash\n"
            "Pipeline Mode = Fully Non-Blocking Asyncio.Queue"
        )
        
        processed_event = OcrProcessedEvent(
            image_id=event.image_id,
            extracted_text=extracted_text,
            confidence=0.99,
            duration_seconds=0.4
        )
        print("[AsyncOCR] Characters successfully segmented! Publishing OcrProcessedEvent...")
        await self.event_bus.publish(processed_event)


class AsyncAiPipelineModule:
    """Decoupled AI context synthesis engine built with asyncio."""

    def __init__(self, event_bus: AsyncEventBus) -> None:
        self.event_bus = event_bus

    async def initialize(self) -> None:
        await self.event_bus.subscribe(OcrProcessedEvent, self.handle_ocr_completed)

    async def handle_ocr_completed(self, event: OcrProcessedEvent) -> None:
        print(f"[AsyncAI] Received OCR processed text for image '{event.image_id}'...")
        print("[AsyncAI] Submitting context to Gemini Flash model (yielding to event loop)...")
        
        # Simulate network round-trip latency to the cloud Gemini API
        await asyncio.sleep(0.6)
        
        summary = "An ingestion pipeline configured with reactive non-blocking queues."
        category = "Cloud Engineering"
        tags = ["asyncio", "queues", "python", "gemini-flash"]
        
        completed_event = AiPipelineCompletedEvent(
            document_id=f"doc_{event.image_id}",
            original_text=event.extracted_text,
            summary=summary,
            category=category,
            tags=tags
        )
        print("[AsyncAI] Document analyzed and indexed! Publishing AiPipelineCompletedEvent...")
        await self.event_bus.publish(completed_event)


class AsyncStorageSinkModule:
    """Decoupled Storage module for document persistence."""

    def __init__(self, event_bus: AsyncEventBus) -> None:
        self.event_bus = event_bus

    async def initialize(self) -> None:
        await self.event_bus.subscribe(AiPipelineCompletedEvent, self.handle_pipeline_completed)

    async def handle_pipeline_completed(self, event: AiPipelineCompletedEvent) -> None:
        print(f"[AsyncStorage] Storing document node '{event.document_id}'...")
        await asyncio.sleep(0.1) # Simulate file IO write latency
        print(f"[AsyncStorage] SUCCESSFULLY INDEXED:\n"
              f"  - Document: {event.document_id}\n"
              f"  - Title:    {event.category}\n"
              f"  - Tags:     {event.tags}\n"
              f"  - Summary:  '{event.summary}'")


# =====================================================================
# 5. Core Runner Execution
# =====================================================================

async def main() -> None:
    print("=== Athena Asyncio Event Bus Sandbox ===")
    
    # 1. Instantiate the Queue-based broker
    bus = AsyncioQueueEventBus()
    await bus.start()

    # 2. Instantiate and wire modular subsystems
    ocr = AsyncOcrModule(bus)
    ai = AsyncAiPipelineModule(bus)
    storage = AsyncStorageSinkModule(bus)

    await ocr.initialize()
    await ai.initialize()
    await storage.initialize()

    print("\n--- Triggering Ingestion Sequence ---")
    
    # 3. Inject first capture event
    capture = ImageCapturedEvent(
        image_id="frame_001_athena",
        raw_bytes=b"\x89PNG\r\n\x1a\n..."
    )
    
    # Trigger non-blocking publish
    await bus.publish(capture)
    
    print("[Application Thread] Event published! Standing by to let async tasks cascade...\n")
    
    # Wait for the complete pipeline to cascade asynchronously
    # Flow: Capture Event -> OCR Module -> Processed Event -> AI Module -> Completed Event -> Storage Module
    await asyncio.sleep(1.5)

    print("\n--- Initiating Graceful System Shutdown ---")
    await bus.shutdown()

if __name__ == "__main__":
    asyncio.run(main())
