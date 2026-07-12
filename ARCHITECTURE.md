# Project Athena: C4 Architecture Documentation

This document defines the system architecture of Project Athena using the **C4 Model** (System Context and Container levels). These boundaries map the relationships between users, the mobile application container, local database components, and hybrid (local/cloud) services.

---

## Level 1: System Context Diagram

The System Context diagram establishes the boundaries of the Athena Second Brain ecosystem, illustrating how users interact with the mobile application and how the application communicates with external entities.

```
+------------------------------------------------------------------------+
|                                                                        |
|                           System Context                               |
|                                                                        |
+------------------------------------------------------------------------+

                    +--------------------------+
                    |      Knowledge Worker    |
                    | (Student / Professional) |
                    +------------+-------------+
                                 |
                                 | Ingests notes, searches,
                                 | chats, and organizes
                                 v
                    +------------+-------------+
                    |      Project Athena      |
                    |       (Mobile App)       |
                    +------+-------------+-----+
                           |             |
         Queries / Sends   |             | Scans / Extracts
         Extracted Text    |             | Local Images
                           v             v
              +------------+----+   +----+-------------+
              |   Gemini API    |   |  Local Sensors   |
              | (Cloud Service) |   |  & OCR Engine    |
              +-----------------+   +------------------+
```

### Element Descriptions

| Element | Type | Description |
| :--- | :--- | :--- |
| **Knowledge Worker** | Person | A student, researcher, or professional who wants to capture, organize, and query visual/textual knowledge. |
| **Project Athena** | Software System | The mobile application (this codebase) which acts as the "Second Brain." It facilitates offline-first local search, semantic graph mapping, and context-aware chat. |
| **Gemini API** | External System | Cloud-hosted intelligence service used for synthesizing notes, generating tags, summaries, and answering multi-turn RAG (Retrieval-Augmented Generation) queries. |
| **Local Sensors & OCR Engine** | External System | System hardware (camera / file import) and local OCR simulation service that digitizes captured visual text. |

---

## Level 2: Container Diagram

The Container diagram shows the high-level technical building blocks inside the Project Athena mobile application and how data flows across them.

```
+----------------------------------------------------------------------------------------+
|                                                                                        |
|                             Container: Project Athena                                  |
|                                                                                        |
+----------------------------------------------------------------------------------------+

                                 +------------------------+
                                 |    Knowledge Worker    |
                                 +-----------+------------+
                                             |
                                             | Interacts with UI
                                             v
+--------------------------------------------+-------------------------------------------+
| Project Athena App Container (Android)                                                 |
|                                                                                        |
|   +--------------------------------------------------------------------------------+   |
|   |                              Presentation Layer                                |   |
|   |                                                                                |   |
|   |  +--------------------+  +--------------------+  +--------------------------+  |   |
|   |  |  Dashboard Screen  |  |   Capture Screen   |  | Library & Chat Screens   |  |   |
|   |  |   (Bento Grid)     |  | (Camera/OCR View)  |  |  (Graph & Chat Interface)|  |   |
|   |  +---------+----------+  +---------+----------+  +------------+-------------+  |   |
|   +------------|-----------------------|--------------------------|----------------+   |
|                |                       v                          |                    |
|                +-----------------------+--------------------------+                    |
|                                        | Observes State /                               |
|                                        | Triggers Actions                               |
|                                        v                                               |
|   +------------------------------------+-------------------------------------------+   |
|   |                              ViewModel Layer                                   |   |
|   |                                                                                |   |
|   |                            +-------------------+                               |   |
|   |                            |  BrainViewModel   |                               |   |
|   |                            +---------+---------+                               |   |
|   +--------------------------------------|-----------------------------------------+   |
|                                          |                                             |
|                     +--------------------+--------------------+                        |
|                     | Coordinates Operations & Data Streams   |                        |
|                     v                                         v                        |
|   +-----------------+-------------------+   +-----------------+--------------------+   |
|   |         Service/Integration Layer   |   |             Database Layer           |   |
|   |                                     |   |                                      |   |
|   |  +--------------+  +-------------+  |   |  +--------------+  +--------------+  |   |
|   |  | GeminiService|  | OcrService/ |  |   |  | DocumentDao  |  | AppDatabase  |  |   |
|   |  |  (Remote AI) |  | Simulator   |  |   |  | (Data Access)|  | (Room/SQLite)|  |   |
|   |  +-------+------+  +------+------+  |   |  +-------+------+  +-------+------+  |   |
|   +----------|----------------|---------+   +----------|-----------------|---------+   |
+--------------|----------------|------------------------|-----------------|-------------+
               |                |                        |                 |
               | HTTPS          | Local API              | SQL Queries     | Direct Reads
               v                v                        v                 v
        +------+-------+ +------+-------+         +------+-------+  +------+------+
        |  Gemini API  | | Local Camera |         |  SQLite DB   |  | File System |
        |   (Cloud)    | | & Storage    |         |   On-Device  |  |  On-Device  |
        +--------------+ +--------------+         +--------------+  +-------------+
```

### Container Component Descriptions

#### 1. Presentation Layer (Jetpack Compose)
- **Dashboard Screen (Bento Grid):** An adaptive visual grid consolidating system indicators, second brain nodes count, direct text notes shortcut, a link to the semantic concept map, and a quick-chat toggle.
- **Capture Screen:** Manages camera feed simulation, OCR extraction triggers, metadata classification inputs, and custom notes logging.
- **Library & Chat Screens:** Supports multi-turn RAG chat querying, semantic search, folder hierarchy view, and detailed nodes visualization.

#### 2. ViewModel Layer (BrainViewModel)
- Acts as the central state hub. Holds UI states in `MutableStateFlow` (e.g., active document list, conversational chat history, processing load states).
- Orchestrates asynchronous operations using Kotlin Coroutines and ensures configuration-change survival.

#### 3. Service & Integration Layer
- **GeminiService:** Interacts with the Gemini REST API endpoints, sending system instructions, knowledge-context documents, and chat prompts to obtain summaries, smart tags, or structured chat answers.
- **OcrService / Simulator:** Handles local image analysis, character scanning, and text recognition. Returns the resulting string of digitized text.

#### 4. Database Layer (Room Database)
- **AppDatabase / DocumentDao:** The local structured store. Models nodes (`CapturedDocument`), concept relationships (`BrainRelationship`), and system preferences. Ensures that Athena is completely functional and queryable offline.
