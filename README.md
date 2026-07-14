# Project Athena: Intelligence Amplification & Semantic Ingestion Pipeline

Project Athena is an advanced, offline-first knowledge synthesis engine and ingestion pipeline designed to capture, digitize, and semantically categorize whiteboard scribbles, hand-written notes, and document snapshots. 

By pairing a visually stunning **Jetpack Compose Android Client** with fully responsive **Flutter Bento Layouts**, an event-driven **Asynchronous Python Pipeline**, and a high-performance **PostgreSQL vector similarity schema**, Athena establishes a state-of-the-art framework for robust personal information retrieval (RAG).

---

## 🚀 Key Architectural Pillars & Features

### 1. Jetpack Compose Android Client
- **Dynamic Theming:** Adheres to **Material Design 3 (M3)** with custom Cosmic Slate Dark (`#0B0F19`) and Paper Slate Light (`#F8FAFC`) palettes.
- **Edge-to-Edge Experience:** Pure implementation of `enableEdgeToEdge()` paired with detailed `WindowInsets` adjustments for fluid status/navigation bar transitions.
- **Polished Visuals:** Leverages structured layouts, high-contrast text ratios, and custom card elevations to provide clean information hierarchy.

### 2. Flutter Accessible Bento Grid Engine (`flutter_bento_theme.dart`)
- **WCAG 2.1 Level AAA Compliance:** Colors strictly engineered to exceed **7:1 contrast ratios** for standard body text and **4.5:1 contrast ratios** for large headings against dark card backgrounds.
- **Responsive Fluid Grid:** Adapts column counts dynamically (1 Column for Mobile, 2 for Tablets, 4 for Desktop/Expanded displays). Card heights scale safely with the system `textScaleFactor` to prevent text clipping up to **200% font zoom**.
- **Tactile Transitions:** Implements custom `AnimationController` combinations to drive smooth **Fade**, **Scale**, and **Slide** entry transitions for cards.
- **Accessibility & Focus States:** Fully keyboard-focusable (`FocusableActionDetector`) with thick high-contrast outline borders (`3.5` logical pixels) on active focus. Includes semantic labelling for screen-readers.

### 3. Asynchronous Event-Driven Architectures (Python)
- **Thread Pool Broker (`athena_event_bus.py`):** Thread-safe decoupled pub-sub system running heavy OCR decoding and Gemini parsing in background workers.
- **Cooperative multitasking Broker (`athena_asyncio_event_bus.py`):** Fully asynchronous event pipeline utilizing Python's `asyncio.Queue` to stream `ImageCapturedEvent` through to `AsyncOcrModule`, triggering `AsyncAiPipelineModule`, and settling in the `AsyncStorageSinkModule`.

### 4. Plugin Contract Verification (`test_plugin_contracts.py`)
- **Contract Enforcement:** Standardizes plugin registration at runtime via a unified Pytest suite.
- **OCR & LLM (AI) Validation:** Tests abstract base contracts (`BasePlugin`), verifying schema declarations, parameter types, bounds (e.g. OCR confidence bounds $0.0 \le C \le 1.0$), and ensuring returns strictly follow structures like `OcrResult` or classification structures.

### 5. PostgreSQL Schema Specification (`POSTGRES_SCHEMA_SPEC.md`)
- **Clean Architecture Model Separation:** Complete isolation of the pure relational entities (`Note`, `Document`, `Tag`, `Embedding`) from PostgreSQL-specific implementations.
- **pgvector Integration:** Integrates `VECTOR(768)` columns to hold high-dimensional text embeddings.
- **HNSW Graph Indexes:** Employs high-performance Hierarchical Navigable Small World (HNSW) graphs on cosine operators for instantaneous semantic lookups.
- **Triggers & Junctions:** Outlines many-to-many junction bindings for tagging systems and trigger procedures to automate `updated_at` timestamps.

---

## 📂 Repository Structure

```
├── app/                             # Jetpack Compose Android Client module
│   ├── src/main/java/com/example/
│   │   ├── MainActivity.kt          # Primary Android Entry Point
│   │   ├── ui/                      # Dashboard, Capture, Chat, Library & Settings Screens
│   │   └── data/                    # Local Room DB, OCR Engines, Gemini Integration
├── flutter_bento_theme.dart         # Accessible Bento Grid Engine (Dart)
├── athena_event_bus.py              # ThreadPool-based Event Bus Broker (Python)
├── athena_asyncio_event_bus.py      # Asyncio Queue-based Event Bus Broker (Python)
├── test_plugin_contracts.py         # Pytest Plugin Contract Suite (Python)
├── POSTGRES_SCHEMA_SPEC.md          # PostgreSQL Database and Clean Spec (Markdown)
├── BENTO_ACCESSIBILITY_SPEC.md      # WCAG 2.1 AAA Contrast Calculations (Markdown)
├── metadata.json                    # Platform Integration Metadata
└── README.md                        # Project Core Documentation
```

---

## 🛠️ Installation & Execution Guidelines

### 1. Running & Compiling the Android Application

You can compile Project Athena's Android application locally using your native development tools or automate the entire toolchain through **Docker**.

#### Option A: Fully Automated Build with Docker Compose (Recommended)
This is the easiest option since it automatically configures the Android SDK, platforms, build tools, licenses, and compiler inside an isolated container:

1. **Prerequisites:** Ensure you have [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/) installed.
2. **Build and Run:** Run the following command from the project root folder:
   ```bash
   docker compose up
   ```
3. **Retrieve the APK:** Once the build successfully completes, the compilation output maps back directly to your local computer. You will find your ready-to-install Android APK at:
   ```filepath
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

#### Option B: Local Building via VS Code (or Terminal)
If you prefer building natively using your local machine environment:

1. **Prerequisites:**
   - Install **Java Development Kit (JDK) 17** (e.g., [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17)).
   - Set the `JAVA_HOME` environment variable pointing to your JDK 17 installation.
   - Install the **Kotlin** and **Extension Pack for Java** extensions in VS Code if you want code completion/syntax highlighting.
2. **Accept SDK Licenses:** Ensure your local Android SDK licenses are accepted by running:
   ```bash
   sdkmanager --licenses
   ```
3. **Compile the APK:**
   - **Linux / macOS:**
     ```bash
     chmod +x ./gradlew
     ./gradlew assembleDebug
     ```
   - **Windows (PowerShell):**
     ```powershell
     .\gradlew.bat assembleDebug
     ```
4. **Locate your APK:** Find the generated installable file in your workspace at `app/build/outputs/apk/debug/app-debug.apk`. Copy this onto your device via USB, or install it using Android Debug Bridge (ADB):
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### 2. Testing the Asynchronous Event-Driven Pipelines (Python)
To run the thread-safe event bus simulation:
```bash
python athena_event_bus.py
```
To run the cooperative `asyncio.Queue` pipeline simulation:
```bash
python athena_asyncio_event_bus.py
```

### 3. Running Plugin Contract Verification
To verify that OCR and Language Model plugins meet the structural contracts:
```bash
pytest test_plugin_contracts.py -v
```

---

## 🌐 Web View & Online Demonstration

For quick, non-destructive web-sandbox testing of the bento-style design mechanics, Project Athena includes an offline interactive visual template.

1. Locate the file `bento_demo.html` in the root workspace.
2. Open this HTML file in any modern web browser.
3. **Web View Experience:**
   - **Interactive Grid:** Shows how cards scale smoothly on narrow (Mobile) vs wide (Desktop) viewport configurations.
   - **Dynamic Contrast Toggle:** Switch between regular Cosmic Slate and High Contrast Mode.
   - **Interactive States:** Hover or focus cards to inspect high-contrast active bounds, and tap them to trigger active ripple visual feedback.

---

## 🔒 Security & Secrets Management

API credentials (such as the Google Gemini API Key) are managed securely through the **AI Studio Secrets Panel**. 
- **DO NOT** hardcode credentials in any resource or code files.
- Access secrets dynamically using `BuildConfig` injection, keeping keys protected in production.
- Refer to `.env.example` in the root folder for format references.

---

## 📜 License & Compliance

Project Athena is released under the **MIT License**. The accessible components are designed in alignment with the **World Wide Web Consortium (W3C) Web Content Accessibility Guidelines (WCAG) 2.1 Level AAA**.
