package engine.xml;

import engine.dto.*;
import engine.exception.XmlFileNotFoundException;
import engine.exception.XmlValidationException;
import engine.model.*;
import engine.xml.generated.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;

/**
 * Parses XML configuration files for Guess Market using JAXB Unmarshaller.
 * Supports Exercise 3 schema (GM-EX3-Schema.xsd) with in-memory stream processing.
 */
public class XMLParser {

    /**
     * Parses and validates an XML InputStream for a given Market Maker in Exercise 3.
     * Does NOT touch disk. Validates event uniqueness against existing system events.
     */
    public static List<engine.model.GMEvent> parseAndValidate(InputStream inputStream, String marketMakerName,
                                                              Set<String> existingEventNames, int startingEventId) {
        if (inputStream == null) {
            throw new XmlValidationException("XML input stream cannot be null.");
        }
        if (marketMakerName == null || marketMakerName.trim().isEmpty()) {
            throw new XmlValidationException("Market Maker username must be provided when loading events.");
        }

        GuessMarket guessMarket;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            guessMarket = (GuessMarket) unmarshaller.unmarshal(inputStream);
        } catch (Exception e) {
            throw new XmlValidationException("Failed to parse XML using JAXB: " + e.getMessage());
        }

        if (guessMarket == null) {
            throw new XmlValidationException("XML content is empty or invalid.");
        }

        if (guessMarket.getGMEvents() == null || guessMarket.getGMEvents().getGMEvent() == null || guessMarket.getGMEvents().getGMEvent().isEmpty()) {
            throw new XmlValidationException("XML file contains no events (<GM-events> missing or empty).");
        }

        List<engine.model.GMEvent> eventsList = new ArrayList<>();
        Set<String> fileEventNames = new HashSet<>();
        int currentId = startingEventId;

        List<engine.xml.generated.GMEvent> jaxbEvents = guessMarket.getGMEvents().getGMEvent();
        for (int i = 0; i < jaxbEvents.size(); i++) {
            engine.xml.generated.GMEvent jaxbEvent = jaxbEvents.get(i);

            String name = jaxbEvent.getName();
            if (name == null || name.trim().isEmpty()) {
                throw new XmlValidationException("Event at position " + (i + 1) + " missing 'name' attribute.");
            }
            name = name.trim();

            String lowerName = name.toLowerCase();
            if (fileEventNames.contains(lowerName)) {
                throw new XmlValidationException("Duplicate Event name in file: '" + name + "'. Event names must be unique.");
            }
            if (existingEventNames != null && existingEventNames.contains(lowerName)) {
                throw new XmlValidationException("Event name '" + name + "' already exists in the system.");
            }
            fileEventNames.add(lowerName);

            String description = jaxbEvent.getDescription();
            if (description == null || description.trim().isEmpty()) {
                throw new XmlValidationException("Event '" + name + "' has an empty <description>.");
            }

            // Commission
            Commission jaxbComm = jaxbEvent.getCommission();
            if (jaxbComm == null) {
                throw new XmlValidationException("Event '" + name + "' missing <commission> element.");
            }

            int commission = jaxbComm.getValue();
            if (commission < 0 || commission > 90) {
                throw new XmlValidationException("Commission percentage for Event '" + name + "' must be between 0 and 90. Found: " + commission);
            }

            String commTypeStr = jaxbComm.getType();
            if (commTypeStr == null || commTypeStr.trim().isEmpty()) {
                commTypeStr = "on-purchase";
            }
            CommissionType commissionType;
            if ("on-purchase".equalsIgnoreCase(commTypeStr.trim())) {
                commissionType = CommissionType.ON_PURCHASE;
            } else if ("on-close".equalsIgnoreCase(commTypeStr.trim())) {
                commissionType = CommissionType.ON_CLOSE;
            } else {
                throw new XmlValidationException("Invalid commission type '" + commTypeStr + "' for Event '" + name + "'. Expected 'on-purchase' or 'on-close'.");
            }

            // Options
            GMOptions jaxbOptions = jaxbEvent.getGMOptions();
            if (jaxbOptions == null || jaxbOptions.getGMOption() == null || jaxbOptions.getGMOption().size() != 2) {
                throw new XmlValidationException("Event '" + name + "' must have exactly 2 options inside <GM-options>.");
            }

            String option1 = jaxbOptions.getGMOption().get(0).trim();
            String option2 = jaxbOptions.getGMOption().get(1).trim();
            if (option1.isEmpty() || option2.isEmpty()) {
                throw new XmlValidationException("Event '" + name + "' option names cannot be empty.");
            }
            if (option1.equalsIgnoreCase(option2)) {
                throw new XmlValidationException("Event '" + name + "' has two identical options: '" + option1 + "'. Options must be distinct.");
            }

            // Method (LMSR or Order Book)
            GMMethod jaxbMethod = jaxbEvent.getGMMethod();
            if (jaxbMethod == null) {
                throw new XmlValidationException("Event '" + name + "' missing <GM-method> element.");
            }

            engine.model.GMEvent eventDomain;
            int eventId = currentId++;

            if (jaxbMethod.getGMOrderBook() != null) {
                GMOrderBook jaxbOB = jaxbMethod.getGMOrderBook();
                boolean allowMint = "true".equalsIgnoreCase(jaxbOB.getAllowMint());
                double initialShares = jaxbOB.getInitial();
                double d = jaxbOB.getD();
                if (d <= 0) {
                    throw new XmlValidationException("Event '" + name + "' parameter 'd' must be greater than 0.");
                }
                if (initialShares < 0) {
                    throw new XmlValidationException("Event '" + name + "' parameter 'initial' cannot be negative.");
                }

                double initialInvestment = initialShares * d;
                eventDomain = new engine.model.GMEvent(eventId, name, description, commission, commissionType, option1, option2,
                        EventType.ORDER_BOOK, 100, allowMint, initialInvestment, d);

            } else if (jaxbMethod.getGMLMSR() != null) {
                GMLMSR jaxbLMSR = jaxbMethod.getGMLMSR();
                int b = jaxbLMSR.getB();
                if (b <= 0) {
                    throw new XmlValidationException("Liquidity parameter 'b' for Event '" + name + "' must be positive (> 0). Found: " + b);
                }

                eventDomain = new engine.model.GMEvent(eventId, name, description, commission, commissionType, option1, option2, b);
            } else {
                throw new XmlValidationException("Event '" + name + "' missing trading method (<GM-LMSR> or <GM-order-book>).");
            }

            eventDomain.setMarketMakerName(marketMakerName.trim());
            eventsList.add(eventDomain);
        }

        return eventsList;
    }

    /**
     * File-based entry point for backwards compatibility or tests.
     */
    public static XMLDataResult parseAndValidate(String xmlFilePath) {
        File xmlFile = new File(xmlFilePath);
        if (!xmlFile.exists() || !xmlFile.isFile()) {
            throw new XmlFileNotFoundException("XML file not found at path: " + xmlFilePath);
        }
        if (!xmlFilePath.toLowerCase().endsWith(".xml")) {
            throw new XmlValidationException("File must have a .xml extension: " + xmlFilePath);
        }

        try (FileInputStream fis = new FileInputStream(xmlFile)) {
            List<engine.model.GMEvent> events = parseAndValidate(fis, "admin", Collections.emptySet(), 1);
            return new XMLDataResult(events);
        } catch (XmlValidationException | XmlFileNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new XmlValidationException("Error reading XML file: " + e.getMessage());
        }
    }
}
