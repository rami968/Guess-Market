package ui;

import engine.service.EngineService;
import engine.service.EngineServiceImpl;

// Application main entry point.
public class Main {

    public static void main(String[] args) {
        // Instantiate engine through interface contract
        EngineService engine = new EngineServiceImpl();

        // Instantiate and run console UI
        ConsoleUI consoleUI = new ConsoleUI(engine);
        consoleUI.run();
    }
}
