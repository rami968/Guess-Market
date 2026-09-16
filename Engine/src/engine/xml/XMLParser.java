package engine.xml;

import engine.exception.XmlFileNotFoundException;
import engine.exception.XmlValidationException;
import engine.model.*;
import engine.xml.generated.*;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;

/**
 * Parses XML configuration files for Guess Market using JAXB Unmarshaller.
 * Performs thorough validation of business rules and schema structure.
 */
public class XMLParser {

    public static XMLDataResult parseAndValidate(String xmlFilePath) {
        File xmlFile = new File(xmlFilePath);
        if (!xmlFile.exists() || !xmlFile.isFile()) {
            throw new XmlFileNotFoundException("XML file not found at path: " + xmlFilePath);
        }

        if (!xmlFilePath.toLowerCase().endsWith(".xml")) {
            throw new XmlValidationException("File must have a .xml extension: " + xmlFilePath);
        }

        GuessMarket guessMarket;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(GuessMarket.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            guessMarket = (GuessMarket) unmarshaller.unmarshal(xmlFile);
        } catch (Exception e) {
            throw new XmlValidationException("Failed to parse XML using JAXB: " + e.getMessage());
        }

        if (guessMarket == null) {
            throw new XmlValidationException("XML file is empty or invalid.");
        }

        if (guessMarket.getGMEvents() == null || guessMarket.getGMEvents().getGMEvent() == null || guessMarket.getGMEvents().getGMEvent().isEmpty()) {
            throw new XmlValidationException("XML file contains no events (<GM-events> missing or empty).");
        }

        List<engine.model.GMEvent> eventsList = new ArrayList<>();
        Set<Integer> seenEventIds = new HashSet<>();
        Map<Integer, engine.model.GMEvent> eventIdMap = new HashMap<>();

        // 1. Parse JAXB events
        List<engine.xml.generated.GMEvent> jaxbEvents = guessMarket.getGMEvents().getGMEvent();
        for (int i = 0; i < jaxbEvents.size(); i++) {
            engine.xml.generated.GMEvent jaxbEvent = jaxbEvents.get(i);

            String name = jaxbEvent.getName();
            if (name == null || name.trim().isEmpty()) {
                throw new XmlValidationException("Event at position " + (i + 1) + " missing 'name' attribute.");
            }
            name = name.trim();

            int id = jaxbEvent.getId();
            if (id <= 0) {
                throw new XmlValidationException("Event '" + name + "' has an invalid <id> element: " + id);
            }

            if (seenEventIds.contains(id)) {
                throw new XmlValidationException("Duplicate Event ID found: " + id + ". Event IDs must be unique across the system.");
            }
            seenEventIds.add(id);

            String description = jaxbEvent.getDescription();
            if (description == null || description.trim().isEmpty()) {
                throw new XmlValidationException("Event ID " + id + " (" + name + ") has an empty <description>.");
            }

            // Commission
            Commission jaxbComm = jaxbEvent.getCommission();
            if (jaxbComm == null) {
                throw new XmlValidationException("Event ID " + id + " missing <commission> element.");
            }

            int commission = jaxbComm.getValue();
            if (commission < 0 || commission > 90) {
                throw new XmlValidationException("Commission percentage for Event ID " + id + " must be between 0 and 90. Found: " + commission);
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
                throw new XmlValidationException("Invalid commission type '" + commTypeStr + "' for Event ID " + id + ". Expected 'on-purchase' or 'on-close'.");
            }

            // Options
            GMOptions jaxbOptions = jaxbEvent.getGMOptions();
            if (jaxbOptions == null || jaxbOptions.getGMOption() == null || jaxbOptions.getGMOption().size() != 2) {
                throw new XmlValidationException("Event ID " + id + " must have exactly 2 options inside <GM-options>.");
            }

            String option1 = jaxbOptions.getGMOption().get(0).trim();
            String option2 = jaxbOptions.getGMOption().get(1).trim();
            if (option1.isEmpty() || option2.isEmpty()) {
                throw new XmlValidationException("Event ID " + id + " option names cannot be empty.");
            }

            // Method (LMSR or Order Book)
            GMMethod jaxbMethod = jaxbEvent.getGMMethod();
            if (jaxbMethod == null) {
                throw new XmlValidationException("Event ID " + id + " missing <GM-method> element.");
            }

            engine.model.GMEvent eventDomain;

            if (jaxbMethod.getGMOrderBook() != null) {
                GMOrderBook jaxbOB = jaxbMethod.getGMOrderBook();
                boolean allowMint = "true".equalsIgnoreCase(jaxbOB.getAllowMint());
                double initialInvestment = jaxbOB.getInitial();
                double d = jaxbOB.getD();
                if (d <= 0) {
                    throw new XmlValidationException("Event ID " + id + " denominator 'd' must be greater than 0.");
                }

                eventDomain = new engine.model.GMEvent(id, name, description, commission, commissionType, option1, option2,
                        EventType.ORDER_BOOK, 100, allowMint, initialInvestment, d);

            } else if (jaxbMethod.getGMLMSR() != null) {
                GMLMSR jaxbLMSR = jaxbMethod.getGMLMSR();
                int b = jaxbLMSR.getB();
                if (b <= 0) {
                    throw new XmlValidationException("Liquidity parameter 'b' for Event ID " + id + " must be positive (> 0). Found: " + b);
                }

                eventDomain = new engine.model.GMEvent(id, name, description, commission, commissionType, option1, option2, b);
            } else {
                throw new XmlValidationException("Event ID " + id + " missing trading method (<GM-LMSR> or <GM-order-book>).");
            }

            eventsList.add(eventDomain);
            eventIdMap.put(id, eventDomain);
        }

        // 2. Parse JAXB Users
        Map<String, User> usersMap = new HashMap<>();
        Map<Integer, String> eventToMMMap = new HashMap<>();

        if (guessMarket.getGMUsers() != null && guessMarket.getGMUsers().getGMUser() != null) {
            List<GMUser> jaxbUsers = guessMarket.getGMUsers().getGMUser();

            for (int i = 0; i < jaxbUsers.size(); i++) {
                GMUser jaxbUser = jaxbUsers.get(i);

                String userName = jaxbUser.getName();
                if (userName == null || userName.trim().isEmpty()) {
                    throw new XmlValidationException("User at position " + (i + 1) + " missing 'name' attribute.");
                }
                userName = userName.trim();

                if (usersMap.containsKey(userName.toLowerCase())) {
                    throw new XmlValidationException("Duplicate user name found in XML: '" + userName + "'. User names must be unique.");
                }

                double initialCash = jaxbUser.getInitialCash();
                if (initialCash <= 0) {
                    throw new XmlValidationException("User '" + userName + "' initial balance must be greater than 0. Found: " + initialCash);
                }

                User userObj = new User(userName, initialCash);

                // Market Maker assignments
                GMMarketMaker jaxbMM = jaxbUser.getGMMarketMaker();
                if (jaxbMM != null && jaxbMM.getEvent() != null) {
                    for (Event eventRef : jaxbMM.getEvent()) {
                        int eventId = eventRef.getId();

                        if (!eventIdMap.containsKey(eventId)) {
                            throw new XmlValidationException("User '" + userName + "' is assigned as MM for non-existent Event ID: " + eventId);
                        }

                        if (eventToMMMap.containsKey(eventId)) {
                            throw new XmlValidationException("Event ID " + eventId + " has multiple Market Makers assigned ('" + eventToMMMap.get(eventId) + "' and '" + userName + "'). Every event must have exactly ONE MM.");
                        }

                        eventToMMMap.put(eventId, userName);
                        userObj.addMarketMakerEventId(eventId);
                        eventIdMap.get(eventId).setMarketMakerName(userName);
                    }
                }

                usersMap.put(userName.toLowerCase(), userObj);
            }
        }

        // Validate that EVERY event has an assigned Market Maker if users were provided
        if (!usersMap.isEmpty()) {
            for (engine.model.GMEvent ev : eventsList) {
                if (ev.getMarketMakerName().isEmpty()) {
                    throw new XmlValidationException("Event ID " + ev.getId() + " (" + ev.getName() + ") has no Market Maker assigned from the defined users.");
                }
            }
        }

        return new XMLDataResult(eventsList, usersMap);
    }
}
