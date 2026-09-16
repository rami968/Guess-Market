package ui.task;

import engine.service.EngineService;
import javafx.concurrent.Task;

/**
 * Asynchronous JavaFX Task for loading XML configuration files in the background.
 * Belongs strictly to the UI layer per lecturer guidelines.
 * Updates progress and status message, with a brief artificial delay (1-2s).
 */
public class LoadXmlTask extends Task<Boolean> {

    private final EngineService engineService;
    private final String filePath;

    public LoadXmlTask(EngineService engineService, String filePath) {
        this.engineService = engineService;
        this.filePath = filePath;
    }

    @Override
    protected Boolean call() throws Exception {
        updateMessage("Validating XML file path...");
        updateProgress(0.1, 1.0);
        Thread.sleep(300);

        updateMessage("Parsing XML structure and validating schema...");
        updateProgress(0.4, 1.0);
        Thread.sleep(500);

        // Perform actual XML parsing and domain model initialization
        engineService.loadXml(filePath);

        updateMessage("Initializing market liquidity and users...");
        updateProgress(0.8, 1.0);
        Thread.sleep(500);

        updateMessage("XML file loaded successfully!");
        updateProgress(1.0, 1.0);
        Thread.sleep(200);

        return true;
    }
}
