# Recommended Project Folder Structure: Modular Monolith (Flutter & FastAPI)

This document establishes the blueprints for a **Modular Monolith** architecture combining a **Flutter** client-side application and a **FastAPI** backend service. The directory organization is specifically designed to enforce **Clean Architecture**, absolute separation of concerns, and bounded contexts (Domain-Driven Design), preventing the codebase from degrading into a tangled "big ball of mud" as features expand.

---

## High-Level Monorepo Architecture

```
/athena-modular-monolith
├── .github/                  # CI/CD workflows, issue templates, PR templates
├── apps/                     # High-level executable client and server applications
│   ├── client_flutter/       # Flutter Mobile/Desktop/Web codebase (Modular/Clean)
│   └── server_fastapi/       # FastAPI Backend service (Modular Monolith layout)
├── packages/                 # Shared cross-cutting libraries or domain contracts
│   └── core_contracts/       # Joint schema representations (e.g., Protobuf or OpenAPI schemas)
├── docker-compose.yml        # Multi-container local execution setup (DBs, Redis, Server)
└── README.md                 # Project root workspace setup instructions
```

---

## 1. Frontend: Flutter (Feature-First Clean Architecture)

Within the Flutter codebase, we enforce a **Feature-First** structure mixed with sub-layers. Grouping files by bounded features (e.g., `capture`, `chat`, `analytics`) rather than functional layers (e.g., `models`, `views`, `controllers`) makes the app highly modular.

### Recommended Directory Layout (`apps/client_flutter/`)

```
client_flutter/
├── android/                  # Android native wrapper
├── ios/                      # iOS native wrapper
├── web/                      # Web native assets and wrapper
├── assets/                   # Static images, SVG icons, typography fonts
│   ├── icons/
│   └── fonts/
├── lib/                      # Flutter core source code
│   ├── main.dart             # Application root entry point
│   ├── app.dart              # MaterialApp, localization, state initialization, routing setup
│   ├── core/                 # App-wide shared components (independent of feature domain)
│   │   ├── theme/            # Central M3 ColorSchemes, Custom Typographies, Shapes
│   │   ├── navigation/       # Type-safe routing engine, Navigation Rail, ShellRoute
│   │   ├── network/          # Http/Dio client wrappers, interceptors, token refreshers
│   │   ├── database/         # Local DB clients (Hive/Isar/ObjectBox/drift)
│   │   ├── utils/            # Validation helpers, date formats, math functions
│   │   └── widgets/          # Shared atomic widgets (custom buttons, bento cards, loaders)
│   └── features/             # Feature-based bounded contexts (The Modules)
│       ├── capture/          # "Capture & OCR" Feature Module
│       │   ├── domain/       # Core enterprise/business domain rules
│       │   │   ├── entities/ # Plain pure-Dart objects (e.g., CaptureRecord)
│       │   │   └── repositories/# Abstract repository interface contracts (the Outgoing Ports)
│       │   ├── data/         # Infrastructure adapters mapping domain to physical sources
│       │   │   ├── datasources/# Remote HTTP calls and local cache drivers
│       │   │   ├── models/   # Serialization-focused DTOs (Data Transfer Objects)
│       │   │   └── repositories/# Implementation of the domain repository interfaces
│       │   └── presentation/ # User-facing interactive components and state controllers
│       │       ├── controllers/# State Managers (Bloc, Cubit, Riverpod Notifier, or ChangeNotifier)
│       │       ├── screens/  # Primary structural page widgets (e.g., SmartCaptureScreen)
│       │       └── widgets/  # Internal-only helper widgets (e.g., DynamicGridItem)
│       ├── chat/             # "Ask Athena Chat" Feature Module
│       │   ├── domain/
│       │   ├── data/
│       │   └── presentation/
│       └── settings/         # "App Settings" Feature Module
│           ├── domain/
│           ├── data/
│           └── presentation/
├── pubspec.yaml              # Flutter dependencies and build configurations
└── test/                     # Unit, widget, and golden visual integration tests
```

### Key Modular Design Rules for Flutter:
1.  **Isolation of Domain:** The `domain/` folder of any feature **must never import** external libraries (except helper libraries like `equatable` or `fpdart`). It represents the pure business rules of your application.
2.  **Strict Feature Separation:** A file in `features/capture/` should never directly import files from `features/chat/`. If two features must share logic, that shared logic must be promoted to the `core/` folder or communicated via a decoupled global state coordinator.

---

## 2. Backend: FastAPI (Modular Monolith)

FastAPI is highly dynamic, which can lead to disorganized structures if not constrained. The **Modular Monolith** structure packages all components of a business domain (Routers, Use Cases, Models, Schemas) under a single feature folder (a "Module"). This ensures high cohesion within modules and low coupling between them.

### Recommended Directory Layout (`apps/server_fastapi/`)

```
server_fastapi/
├── alembic/                  # Database migration scripts
├── src/                      # Backend source directory
│   ├── main.py               # FastAPI application initialization & middleware registration
│   ├── config.py             # Pydantic BaseSettings, environment variable configuration
│   ├── core/                 # Cross-cutting platform concerns (Module-independent)
│   │   ├── security/         # Password hashing, JWT verification, OAuth2 flows
│   │   ├── database.py       # SQLAlchemy/SQLModel engine & SessionPool lifecycle manager
│   │   ├── exceptions.py     # Unified API Exception types and HTTP handlers
│   │   └── logging.py        # Centralized structured JSON logging configuration
│   └── modules/              # Unified business modules (The Bounded Contexts)
│       ├── capture/          # "Capture & OCR" Module
│       │   ├── __init__.py   # Exports public APIs/Interfaces of this module
│       │   ├── router.py     # API endpoints (the Incoming Adapters)
│       │   ├── service.py    # Business workflow handlers (Application layer/Use Cases)
│       │   ├── models.py     # SQLAlchemy DB entities (Infrastructure/Storage layer)
│       │   ├── schemas.py    # Pydantic request/response validation schemas (Deduplicated DTOs)
│       │   ├── interfaces.py # Abstract base definitions (e.g. OCR provider ABC class)
│       │   └── providers/    # Third-party JNI or API adapters (e.g., Google ML Kit, PaddleOCR)
│       ├── chat/             # "Ask Athena Chat" Module
│       │   ├── router.py
│       │   ├── service.py
│       │   ├── models.py
│       │   └── schemas.py
│       └── storage/          # "Document Indexing & Vector Search" Module
│           ├── router.py
│           ├── service.py
│           ├── models.py
│           └── schemas.py
├── tests/                    # Backend unit and integration test suite
│   ├── conftest.py           # Pytest fixtures (DB transaction setup, mock clients)
│   ├── test_capture.py       # Endpoint tests for Capture module
│   └── test_chat.py          # End-to-end tests for RAG chat module
├── Dockerfile                # Production container assembly file
├── requirements.txt          # Python dependencies
└── alembic.ini               # SQL database migration engine settings
```

### Key Modular Design Rules for FastAPI:
1.  **Encapsulated Database Models:** Each module declares its own tables/models in its own `models.py`. Foreign key relationships across modules should be minimized; reference standard UUID IDs rather than enforcing strict foreign keys in the ORM. This allows a module to be easily extracted into a microservice in the future if scale demands it.
2.  **Decoupled Communication:** If Module A needs to execute logic in Module B, it should do so by invoking Module B's dynamic services (registered through a central service registry) or by emitting async events via an in-memory or external queue (e.g., Redis).
3.  **Local Module Routers:** Every module exposes a `router` instance in its `router.py`. The central `main.py` simply mounts these routes dynamically:
    ```python
    from src.modules.capture.router import router as capture_router
    from src.modules.chat.router import router as chat_router

    app.include_router(capture_router, prefix="/capture", tags=["Capture"])
    app.include_router(chat_router, prefix="/chat", tags=["Chat"])
    ```
