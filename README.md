# 📈 Guess Market - EX2 (JavaFX GUI, Order Book & Multi-User Prediction Market)

**Guess Market** is a Prediction Market Java application implementing both the **Logarithmic Market Scoring Rule (LMSR)** and **Order Book (OB)** trading mechanisms, built with a **JavaFX Graphical User Interface**.

Submitted by: **Rami Zargian** (ID: `324081231`)

---

## 🌟 Key Features (Exercise 2)

* **JavaFX Graphical User Interface**: Rich, resizable GUI featuring a top bar with `FileChooser` loading, progress indicator (`ProgressBar`), and 2 main navigation tabs (`Events` and `Users`).
* **Order Book (OB) Mutual Trading**: Peer-to-peer share trading via limit orders (BID and ASK), automatic trade matching engine on price cross, auto-minting mechanism, and real-time calculation of **LAST**, **BID**, **ASK**, **MID**, and **SPREAD** statistics.
* **LMSR Math Engine**: Preserved from EX1, supporting non-mutual market scoring with automated liquidity subsidy.
* **Multi-User & Account Management**: Comprehensive user account tracking, starting balance validation ($>0$), transaction history, holdings per event, and strict non-negative balance enforcement (blocks user if an action would cause negative balance).
* **Market Maker (MM) Event Lifecycle**: Event lifecycle states (`INACTIVE` $\rightarrow$ `ACTIVE` $\rightarrow$ `CLOSED`). MM exclusive event activation, initial funding (subsidy for LMSR, stock batch for OB), closure/resolution, and fee collection into MM account.
* **Event Filtering**: Dynamic filtering by Event Type (LMSR/OB), Event Status (INACTIVE/ACTIVE/CLOSED), and Fee Scheme (on-purchase/on-close).
* **XML v2 Schema Validation**: Unmarshalls and validates XML v2 files (`<GM-users>`, `<GM-market-maker>`, `<GM-order-book>`), validating unique event IDs, unique usernames, positive balances, valid MM event references, and exact 1 MM per event rule.
* **Asynchronous Task File Loading**: `LoadXmlTask` implemented in the UI layer extending `javafx.concurrent.Task`, updating progress with a 1-2 second artificial delay without blocking the UI thread.

---

## 🏗️ Architecture & Design Decisions

```
Guess Market/
├── Engine/               # Pure Java Engine Module (Passive, no JavaFX imports)
│   ├── src/
│   │   ├── engine/dto/         # Immutable DTOs (EventDTO, UserDTO, OrderDTO, OrderBookStatsDTO)
│   │   ├── engine/exception/   # XmlValidationException, XmlFileNotFoundException
│   │   ├── engine/lmsr/        # LMSR Math Calculator
│   │   ├── engine/model/       # User, Order, OrderBook, GMEvent, Enums
│   │   ├── engine/service/     # EngineService & EngineServiceImpl
│   │   └── engine/xml/         # XMLParser (DOM/XML v2 parser & validators)
├── UI/                   # JavaFX GUI Module
│   ├── src/ui/
│   │   ├── controller/         # MainController, EventsTabController, UsersTabController
│   │   ├── resources/          # main.fxml, events_tab.fxml, users_tab.fxml, style.css
│   │   ├── task/               # LoadXmlTask (JavaFX Task for background loading)
│   │   └── MainApp.java        # JavaFX Application Launcher
├── lib/                  # JavaFX 21 SDK & JAXB Libraries
└── run.bat               # Automated Execution Batch Script
```

### Design Decisions:
1. **Engine Passive Model**: In accordance with lecturer guidelines, the Engine contains no JavaFX `Property` objects. It remains a pure Java domain layer returning immutable DTOs to ensure portability for future Client-Server architecture (EX3).
2. **UI Layer Task Placement**: `LoadXmlTask` sits strictly inside the `ui.task` package within the UI module, managing JavaFX thread progress and UI callbacks without polluting the Engine.
3. **Resizable Layout**: Built using `SplitPane`, `ScrollPane`, and flex containers (`VBox`, `HBox`, `BorderPane`) with minimum window dimensions (900x600), guaranteeing seamless scaling across different monitor resolutions.

---

## 🚀 Execution & Quick Start

### Prerequisites
* **Java SDK 25** (or Java 11+)
* Windows 10/11 OS

### Running the Application
Run the batch script from the project root:
```cmd
run.bat
```

Or execute via command line with JavaFX module path:
```cmd
java --module-path "lib\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp "out\production\UI;out\production\Engine;lib\*" ui.MainApp
```

---

## 🔗 Repository
* **GitHub**: [https://github.com/rami968/Guess-Market](https://github.com/rami968/Guess-Market)
