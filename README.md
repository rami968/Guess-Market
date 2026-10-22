# 📈 Guess Market — Prediction Market Trading Platform

[![Java](https://img.shields.io/badge/Java-21%2B%20%2F%2025-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21-blue.svg)](https://openjfx.io/)
[![Tomcat](https://img.shields.io/badge/Apache%20Tomcat-10.1%20(Jakarta%20EE)-yellow.svg)](https://tomcat.apache.org/)
[![Architecture](https://img.shields.io/badge/Architecture-Distributed%20Client--Server-brightgreen.svg)]()

> **Final Capstone Project**  
> Developed and submitted by: **Rami Zargian**  
> GitHub Repository: [https://github.com/rami968/Guess-Market](https://github.com/rami968/Guess-Market)

---

## 📖 Overview

**Guess Market** is a full-featured, distributed **Prediction Market Platform** implemented in Java. The platform enables multi-user trading on the outcomes of real-world binary and categorical events. 

The system implements two distinct financial market mechanics:
1. **LMSR (Logarithmic Market Scoring Rule)**: An automated algorithmic liquidity provider guaranteeing continuous pricing and liquidity based on Robin Hanson's information market model.
2. **Order Book (Continuous Double Auction)**: A peer-to-peer limit order market supporting BID and ASK orders, price-time priority matching, and an automated **Auto-Minting** mechanism when opposing bids cross.

Developed across three progressive milestones, Guess Market evolved from an initial core mathematical engine into a robust, high-performance **distributed Client-Server enterprise architecture** powered by Apache Tomcat 10.1 (Jakarta EE 10) and a modern JavaFX 21 desktop client.

---

## 🌟 The Evolution of Guess Market

### 🔹 Milestone 1 — Core Mathematical Engine & XML Validation
* **LMSR Math Engine**: Pure mathematical implementation of Robin Hanson's market scoring rule:
  $$C(q_1, q_2) = b \cdot \ln\left(e^{q_1/b} + e^{q_2/b}\right)$$
* **Dynamic Pricing & Probabilities**: Automated marginal cost calculations and instantaneous probabilities:
  $$p_i = \frac{e^{q_i / b}}{\sum_{j} e^{q_j / b}}$$
* **XML v1 Schema Validation**: JAXB unmarshalling with strict semantic validation: verifying non-negative liquidity subsidies ($b > 0$), probability sanity, and uniqueness constraints.
* **Console Interface**: Interactive CLI for inspecting events and testing purchases.

### 🔹 Milestone 2 — JavaFX Graphical UI, Order Book & Multi-User Portfolios
* **Modern JavaFX GUI**: Rich, responsive dark-mode interface structured with FXML and custom CSS.
* **Order Book Matching Engine**:
  * Continuous limit order trading (BUY and SELL).
  * Price-time priority order matching.
  * **Auto-Minting Mechanism**: When opposing limit BUY orders cross at complementary prices ($\text{Price}_A + \text{Price}_B \ge \$100$), complementary shares are minted dynamically and matched.
  * Real-time calculation of **LAST**, **BID**, **ASK**, **MID**, and **SPREAD** statistics.
* **Multi-User Account Management**: User portfolios, balance tracking, starting balance validation, and historical transaction ledgers.
* **Market Maker (MM) Lifecycle**: Event lifecycle management (`INACTIVE` $\rightarrow$ `ACTIVE` $\rightarrow$ `CLOSED`), initial liquidity funding, and fee collection.
* **Dynamic Filtering**: Real-time filtering by Event Type (LMSR/OB), Status (Inactive/Active/Closed), and Fee Scheme (On-Purchase/On-Close).

### 🔹 Milestone 3 — Distributed Client-Server Architecture
* **Tomcat 10.1 / Jakarta EE Server**:
  * Transitions from desktop architecture into a distributed multi-tier Client-Server platform deployed as a single WAR (`guess_market.war`).
  * Pure **In-Memory Server State**: To strictly accommodate examiner file-permission constraints, XML files are parsed on-the-fly directly from request input streams without writing temporary files to disk. All state resides in memory and resets cleanly on server restart.
* **Dynamic User Registration**: Dedicated Login screen with username uniqueness enforcement; no hardcoded users in XML files.
* **Market Maker Dynamic Attribution**: Any client can upload XML files; the uploader automatically becomes the Market Maker for the newly added events.
* **Periodic Pull (Polling) Synchronization**:
  * JavaFX client pulls updates every 1 second in the background.
  * Utilizes **State Versioning** (`/api/sync`) for efficient bandwidth management—data tables only refresh when state has actually changed.
  * Utilizes **Delta Fetching** for incremental chat message retrieval.
* **Event Closure Balance Zeroing**: Event payouts completely distribute all funds and commissions, ensuring the event account balance zeroes out to exactly **$0.00**.
* **★ Live Multi-User Chat Room (5-Point Bonus Feature)**: Real-time global group chat room integrated into both the backend (`/api/chat`) and the JavaFX GUI client (`💬 Chat Room` tab).

---

## 💡 Core Market Mechanics

### 1. LMSR (Automated Market Maker)
* **Pricing Formula**: Every purchase of outcome shares shifts the market state vector $\vec{q} = (q_1, q_2)$. The total cost to buy $\Delta q_i$ shares is $C(\vec{q} + \Delta q_i \vec{e}_i) - C(\vec{q})$.
* **Constant Liquidity**: The market maker is always available to quote a price, regardless of trading volume.
* **Subsidy Parameter ($b$)**: Controls market depth and price sensitivity. Higher $b$ requires larger volumes to shift prices.

### 2. Order Book (Continuous Double Auction with Auto-Minting)
* **Standard Matching**: Direct matching between BUY and SELL orders for the same outcome when $\text{BID} \ge \text{ASK}$.
* **Auto-Minting Mechanism**: When two traders submit BUY orders for complementary outcomes ($A$ and $B$) such that $\text{Price}_A + \text{Price}_B \ge \$100$, the engine automatically mints a complete share bundle ($A + B$) for $\$100$, executes both orders immediately, and credits the surplus appropriately.
* **Price Bounds**: Share outcomes are bounded between $\$1.00$ and $\$99.00$.

### 3. Fee Schemes & Event Resolution
* **On-Purchase Fee**: The Market Maker takes a configurable percentage fee on every trade execution.
* **On-Close Fee**: The Market Maker takes a commission percentage when the event resolves.
* **Event Resolution**: Upon closing an event, the Market Maker declares the winning outcome. Each winning share pays out exactly **$100.00**. Losing shares become worthless. Remaining reserves and commissions are distributed such that the event's internal ledger balances to **$0.00**.

---

## 🖥️ Client Graphical Interface (JavaFX)

The desktop client provides an intuitive, high-density financial workstation experience:
* **Top Navigation Bar**: Live connection status indicator, current logged-in user badge, account balance with a quick-charge modal dialog, and an **Upload XML Events** button.
* **📊 Events & Predictions Tab**:
  * Master-detail view displaying event metadata, status, Market Maker, type, and options.
  * Real-time badges for **LAST**, **BID**, **ASK**, **MID**, and **SPREAD**.
  * Dynamic filters for Market Type, Status, and Fee Scheme.
  * Action modals: **Buy LMSR Shares**, **Submit Limit Order**, **Activate Event**, and **Close Event**.
* **👤 Traders & Portfolios Tab**:
  * Real-time user balance and net worth calculation.
  * Comprehensive table of current shareholdings broken down by event and outcome.
  * Complete transaction ledger recording every buy, sell, deposit, and payout.
  * Directory of all active registered traders in the market.
* **💬 Live Chat Room Tab (Bonus Feature)**:
  * Global real-time communication channel across all active traders.
  * Delta-polled background sync with auto-scroll and instant message dispatch.

---

## 🏗️ Architecture & Component Design

```
Guess Market/
├── Engine/                     # Domain & Business Logic (Pure Java, no UI dependencies)
│   └── src/
│       ├── engine/dto/         # Immutable DTOs (EventDTO, UserDTO, OrderDTO, ChatDTO, etc.)
│       ├── engine/exception/   # XmlValidationException, XMLException
│       ├── engine/lmsr/        # LMSR Math Calculator
│       ├── engine/model/       # GMEvent, User, Order, OrderBook, Transaction
│       ├── engine/service/     # EngineService & EngineServiceImpl
│       └── engine/xml/         # XMLParser (JAXB in-memory stream parser & schema validator)
├── Server/                     # Jakarta EE 10 / Tomcat 10.1 Web Application
│   ├── src/server/
│   │   ├── context/            # EngineContext (Application Singleton & Chat Queue)
│   │   ├── servlets/           # AuthServlet, EventsServlet, EventActionServlet,
│   │   │                       # ChargeServlet, UploadEventServlet, SyncServlet, ChatServlet
│   │   └── utils/              # ServletUtils (JSON request/response parsing)
│   └── web/WEB-INF/web.xml     # Deployment Descriptor
├── UI/                         # JavaFX 21 Client Application
│   └── src/ui/
│       ├── controller/         # LoginController, MainController, EventsTabController,
│       │                       # UsersTabController, ChatTabController
│       ├── http/               # HttpClientManager (java.net.http.HttpClient & Gson)
│       ├── resources/          # FXML views (login, main, events, users, chat) and style.css
│       └── MainApp.java        # JavaFX Client Launcher
├── lib/                        # JavaFX SDK 21, Gson, JAXB, Servlet API
├── build_war.py                # Automated build script for guess_market.war
├── build_submission.py         # Automated build script for final submission ZIP
└── run.bat                     # Standalone client launch script
```

---

## 💻 RESTful Server Endpoints

| Endpoint | Method | Description |
| :--- | :---: | :--- |
| `/ping` | `GET` | Health check & servlet container verification |
| `/api/login` | `POST` | Registers or authenticates a unique username |
| `/api/users` | `GET` | Retrieves full user profile or list of all active traders |
| `/api/user/charge` | `POST` | Deposits funds into a user's balance |
| `/api/events/upload`| `POST` | In-memory multipart XML upload; sets uploader as Market Maker |
| `/api/events` | `GET` | Lists all events with live Order Book BID/ASK stats |
| `/api/events/action`| `POST` | Dispatches MM activation, OB limit orders, LMSR purchases, and closure |
| `/api/sync` | `GET` | Returns current state version for lightweight client polling |
| `/api/chat` | `GET`/`POST` | Broadcasts and delta-fetches multi-user chat messages (Bonus) |

---

## 🚀 Execution & Quick Start

### Prerequisites
* **Java SDK 21+** (Java 25 verified)
* **Apache Tomcat 10.1+** (Jakarta EE 10)
* Windows 10/11 OS

### Step 1: Start Apache Tomcat Server
1. Copy `guess_market.war` into your Tomcat `webapps` directory (e.g. `C:\Tomcat\webapps\guess_market.war`).
2. Run `startup.bat` from Tomcat's `bin` folder.
3. Verify server is running at: `http://localhost:8080/guess_market/ping`

### Step 2: Launch the Client Application
1. Double-click `run.bat` (or execute via command line):
   ```cmd
   run.bat
   ```
2. Enter your username on the Login screen and click **Connect & Enter Market**.
3. Deposit trading capital with **Charge Balance** or upload prediction events via **Upload XML Events**.

---



---

## 👤 Author
**Rami Zargian**    
Email: [ramizargian@gmail.com](mailto:ramizargian@gmail.com)  
GitHub: [https://github.com/rami968/Guess-Market](https://github.com/rami968/Guess-Market)
