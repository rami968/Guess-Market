# 📈 Guess Market - Exercise 3 (Client-Server Distributed Prediction Market)

> **★ Bonus Feature Implemented: Real-Time Multi-User Global Chat Room (5 Points Bonus) ★**  
> Integrated into both the Tomcat backend (`/api/chat`) and the JavaFX GUI client (`💬 Chat Room` tab), enabling all active market participants to communicate in real time.

Submitted by: **Rami Zargian** (ID: `324081231`)  
Repository: [https://github.com/rami968/Guess-Market.git](https://github.com/rami968/Guess-Market.git)

---

## 🌟 Key Features (Exercise 3)

* **Multi-Tier Distributed Architecture**:
  * **Server Tier (Apache Tomcat 10.1 / Jakarta EE 10)**: Single deployable WAR (`guess_market.war`) managing stateful prediction market logic via RESTful JSON servlets and multipart XML stream handling.
  * **Client Tier (JavaFX 21+)**: Rich desktop client connecting exclusively over HTTP (`HttpClientManager`) with background periodic pulling (1-second interval) for live state synchronization.
  * **Core Engine & DTOs**: Immutable Data Transfer Objects, Order Book matching engine with Auto-Minting, and LMSR pricing engine.
* **Pure In-Memory Server State**:
  * XML configuration files are streamed and unmarshalled on-the-fly directly from request byte streams using JAXB without writing temporary files to disk, adhering strictly to examiner server permission constraints.
  * All state resides in memory; restarting the server cleanly resets all data.
* **Dynamic User Registration**:
  * Dedicated Login screen ensuring unique usernames. Users deposit funds dynamically via `Charge Balance`.
* **Market Maker Dynamic Attribution & Event Accumulation**:
  * Any connected trader can upload an XML file conforming to `GM-EX3-Schema.xsd`.
  * The uploader automatically becomes the Market Maker for the uploaded events. Subsequent uploads accumulate into the market without replacing existing events.
* **Dual Trading Mechanisms (LMSR & Order Book)**:
  * **LMSR**: Automated market maker scoring rule with dynamic pricing and probability calculations.
  * **Order Book**: Limit orders (BUY/SELL), continuous order matching on price crosses, Auto-Minting, and live **LAST**, **BID**, **ASK**, **MID**, and **SPREAD** statistics.
* **Event Account Zeroing**:
  * Event closure completely pays out gross returns and commissions to holders, guaranteeing the event account balance drops exactly to $0.00.
* **Responsive JavaFX GUI**:
  * Built using `SplitPane`, `ScrollPane`, and `FlowPane` with minimum stage dimensions (`1100x700`), ensuring all tables, controls, and statistics remain fully visible and scale gracefully upon resizing.

---

## 🏗️ System Architecture

```
Guess Market/
├── Engine/               # Pure Java Domain Layer
│   └── src/
│       ├── engine/dto/         # Immutable DTOs (EventDTO, UserDTO, OrderDTO, ChatDTO, etc.)
│       ├── engine/exception/   # XmlValidationException, XMLException
│       ├── engine/lmsr/        # LMSR Math Calculator
│       ├── engine/model/       # GMEvent, User, Order, OrderBook, Transaction
│       ├── engine/service/     # EngineService & EngineServiceImpl
│       └── engine/xml/         # XMLParser (JAXB in-memory stream parser & schema validator)
├── Server/               # Jakarta EE 10 / Tomcat 10.1 Web Application
│   ├── src/server/
│   │   ├── context/            # EngineContext (Application Singleton & Chat Queue)
│   │   ├── servlets/           # AuthServlet, EventsServlet, EventActionServlet,
│   │   │                       # ChargeServlet, UploadEventServlet, SyncServlet, ChatServlet
│   │   └── utils/              # ServletUtils (JSON request/response parsing)
│   └── web/WEB-INF/web.xml     # Deployment Descriptor
├── UI/                   # JavaFX 21 Client Application
│   └── src/ui/
│       ├── controller/         # LoginController, MainController, EventsTabController,
│       │                       # UsersTabController, ChatTabController
│       ├── http/               # HttpClientManager (java.net.http.HttpClient & Gson)
│       ├── resources/          # FXML views (login, main, events, users, chat) and style.css
│       └── MainApp.java        # JavaFX Client Launcher
├── lib/                  # Runtime JARs (JavaFX 21 SDK, Gson, JAXB, Servlet API)
├── build_war.py          # Automated script building guess_market.war
├── build_submission.py   # Automated script creating final submission ZIP
└── run.bat               # Client launch script
```

---

## 🚀 Execution Instructions

### Step 1: Start Apache Tomcat 10.1 Server
1. Copy `guess_market.war` into your Tomcat `webapps` folder (e.g. `C:\Tomcat\webapps\guess_market.war`).
2. Run `bin\startup.bat` to start Tomcat.
3. Verify server is running at: `http://localhost:8080/guess_market/ping`

### Step 2: Run the Client Application
1. Double click `run.bat` (or in the submission package: `Client/run.bat`).
2. Enter your username on the Login screen and click **Connect & Enter Market**.
3. Deposit funds using **Charge Balance** or upload new prediction events using **Upload XML Events**.

---

## 📦 Submission Package Structure (`Rami_Zargian_324081231_EX3.zip`)

```
Rami_Zargian_324081231_EX3.zip
├── guess_market.war        # Deployable WAR with bundled Gson, JAXB, and Engine.jar
├── README.docx             # Complete documentation and class descriptions
└── Client/                 # Standalone client folder
    ├── Engine.jar          # Compiled core engine
    ├── UI.jar              # Compiled JavaFX GUI client
    ├── lib/                # JavaFX SDK and runtime dependencies
    └── run.bat             # Standalone client launcher batch script
```
