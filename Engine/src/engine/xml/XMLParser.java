package engine.xml;

import engine.exception.XmlFileNotFoundException;
import engine.exception.XmlValidationException;
import engine.model.*;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;

/**
 * Parses XML configuration files for Guess Market (XML v1 and v2 formats).
 * Performs thorough validation of business rules and structure.
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

        Document doc;
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setIgnoringComments(true);
            dbFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
        } catch (Exception e) {
            throw new XmlValidationException("Failed to parse XML structure: " + e.getMessage());
        }

        Element root = doc.getDocumentElement();
        if (!"GuessMarket".equalsIgnoreCase(root.getTagName()) && !"Guess-Market".equalsIgnoreCase(root.getTagName())) {
            throw new XmlValidationException("Invalid root element in XML. Expected <GuessMarket> or <Guess-Market>.");
        }

        // 1. Parse Events (<GM-events>)
        NodeList eventsWrapperList = root.getElementsByTagName("GM-events");
        if (eventsWrapperList.getLength() == 0) {
            throw new XmlValidationException("XML file missing <GM-events> element.");
        }

        Element eventsWrapper = (Element) eventsWrapperList.item(0);
        NodeList eventNodes = eventsWrapper.getElementsByTagName("GM-event");
        if (eventNodes.getLength() == 0) {
            throw new XmlValidationException("XML file contains no events (<GM-event> elements missing).");
        }

        List<GMEvent> eventsList = new ArrayList<>();
        Set<Integer> seenEventIds = new HashSet<>();
        Map<Integer, GMEvent> eventIdMap = new HashMap<>();

        for (int i = 0; i < eventNodes.getLength(); i++) {
            Node node = eventNodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element eventElem = (Element) node;

            // Event Name attribute
            String name = eventElem.getAttribute("name");
            if (name == null || name.trim().isEmpty()) {
                throw new XmlValidationException("Event at position " + (i + 1) + " missing 'name' attribute.");
            }
            name = name.trim();

            // Event ID
            int id;
            try {
                String idStr = getElementText(eventElem, "id");
                id = Integer.parseInt(idStr);
            } catch (Exception e) {
                throw new XmlValidationException("Event '" + name + "' has an invalid or missing <id> element.");
            }

            if (seenEventIds.contains(id)) {
                throw new XmlValidationException("Duplicate Event ID found: " + id + ". Event IDs must be unique across the system.");
            }
            seenEventIds.add(id);

            // Description
            String description = getElementText(eventElem, "description");
            if (description.isEmpty()) {
                throw new XmlValidationException("Event ID " + id + " (" + name + ") has an empty <description>.");
            }

            // Commission
            Element commElem = getChildElement(eventElem, "commission");
            if (commElem == null) {
                commElem = getChildElement(eventElem, "comision"); // Fallback for schema variant
            }
            if (commElem == null) {
                throw new XmlValidationException("Event ID " + id + " missing <commission> element.");
            }

            int commission;
            try {
                commission = Integer.parseInt(commElem.getTextContent().trim());
            } catch (Exception e) {
                throw new XmlValidationException("Event ID " + id + " has invalid commission percentage value.");
            }

            if (commission < 0 || commission > 90) {
                throw new XmlValidationException("Commission percentage for Event ID " + id + " must be between 0 and 90. Found: " + commission);
            }

            String commTypeStr = commElem.getAttribute("type");
            if (commTypeStr == null || commTypeStr.trim().isEmpty()) {
                commTypeStr = "on-purchase"; // default
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
            Element optionsElem = getChildElement(eventElem, "GM-options");
            if (optionsElem == null) {
                throw new XmlValidationException("Event ID " + id + " missing <GM-options> element.");
            }
            NodeList optionNodes = optionsElem.getElementsByTagName("GM-option");
            if (optionNodes.getLength() != 2) {
                throw new XmlValidationException("Event ID " + id + " must have exactly 2 options. Found: " + optionNodes.getLength());
            }

            String option1 = optionNodes.item(0).getTextContent().trim();
            String option2 = optionNodes.item(1).getTextContent().trim();
            if (option1.isEmpty() || option2.isEmpty()) {
                throw new XmlValidationException("Event ID " + id + " option names cannot be empty.");
            }

            // Trading Method (LMSR or Order Book)
            Element methodElem = getChildElement(eventElem, "GM-method");
            if (methodElem == null) {
                throw new XmlValidationException("Event ID " + id + " missing <GM-method> element.");
            }

            GMEvent eventDomain;

            Element obElem = getChildElement(methodElem, "GM-order-book");
            Element lmsrElem = getChildElement(methodElem, "GM-LMSR");

            if (obElem != null) {
                // Order Book event
                boolean allowMint = Boolean.parseBoolean(obElem.getAttribute("allow-mint"));
                
                double initialInvestment = 0.0;
                String initialStr = obElem.getAttribute("initial");
                if (initialStr != null && !initialStr.trim().isEmpty()) {
                    try {
                        initialInvestment = Double.parseDouble(initialStr.trim());
                    } catch (Exception e) {
                        throw new XmlValidationException("Event ID " + id + " has invalid Order Book initial investment value.");
                    }
                }

                double d = 1.0;
                String dStr = obElem.getAttribute("d");
                if (dStr != null && !dStr.trim().isEmpty()) {
                    try {
                        d = Double.parseDouble(dStr.trim());
                    } catch (Exception e) {
                        throw new XmlValidationException("Event ID " + id + " has invalid Order Book denominator d.");
                    }
                }
                if (d <= 0) {
                    throw new XmlValidationException("Event ID " + id + " denominator 'd' must be greater than 0.");
                }

                eventDomain = new GMEvent(id, name, description, commission, commissionType, option1, option2,
                        EventType.ORDER_BOOK, 100, allowMint, initialInvestment, d);

            } else if (lmsrElem != null) {
                // LMSR event
                int b;
                try {
                    String bStr = getElementText(lmsrElem, "b");
                    b = Integer.parseInt(bStr);
                } catch (Exception e) {
                    throw new XmlValidationException("Event ID " + id + " has invalid or missing LMSR liquidity parameter 'b'.");
                }
                if (b <= 0) {
                    throw new XmlValidationException("Liquidity parameter 'b' for Event ID " + id + " must be positive (> 0). Found: " + b);
                }

                eventDomain = new GMEvent(id, name, description, commission, commissionType, option1, option2, b);
            } else {
                throw new XmlValidationException("Event ID " + id + " missing trading method (<GM-LMSR> or <GM-order-book>).");
            }

            eventsList.add(eventDomain);
            eventIdMap.put(id, eventDomain);
        }

        // 2. Parse Users (<GM-users>)
        Map<String, User> usersMap = new HashMap<>();
        Map<Integer, String> eventToMMMap = new HashMap<>();

        NodeList usersWrapperList = root.getElementsByTagName("GM-users");
        if (usersWrapperList.getLength() > 0) {
            Element usersWrapper = (Element) usersWrapperList.item(0);
            NodeList userNodes = usersWrapper.getElementsByTagName("GM-user");

            for (int i = 0; i < userNodes.getLength(); i++) {
                Node node = userNodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element userElem = (Element) node;

                String userName = userElem.getAttribute("name");
                if (userName == null || userName.trim().isEmpty()) {
                    throw new XmlValidationException("User at position " + (i + 1) + " missing 'name' attribute.");
                }
                userName = userName.trim();

                if (usersMap.containsKey(userName.toLowerCase())) {
                    throw new XmlValidationException("Duplicate user name found in XML: '" + userName + "'. User names must be unique.");
                }

                double initialCash;
                try {
                    String cashStr = getElementText(userElem, "initial-cash");
                    initialCash = Double.parseDouble(cashStr);
                } catch (Exception e) {
                    throw new XmlValidationException("User '" + userName + "' has invalid or missing <initial-cash>.");
                }

                if (initialCash <= 0) {
                    throw new XmlValidationException("User '" + userName + "' initial balance must be greater than 0. Found: " + initialCash);
                }

                User userObj = new User(userName, initialCash);

                // Parse Market Maker assigned events
                Element mmElem = getChildElement(userElem, "GM-market-maker");
                if (mmElem != null) {
                    NodeList mmEventNodes = mmElem.getElementsByTagName("event");
                    for (int j = 0; j < mmEventNodes.getLength(); j++) {
                        Element eventRef = (Element) mmEventNodes.item(j);
                        String eventIdStr = eventRef.getAttribute("id");
                        int eventId;
                        try {
                            eventId = Integer.parseInt(eventIdStr);
                        } catch (Exception e) {
                            throw new XmlValidationException("User '" + userName + "' has invalid Market Maker event ID reference.");
                        }

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
            for (GMEvent ev : eventsList) {
                if (ev.getMarketMakerName().isEmpty()) {
                    throw new XmlValidationException("Event ID " + ev.getId() + " (" + ev.getName() + ") has no Market Maker assigned from the defined users.");
                }
            }
        }

        return new XMLDataResult(eventsList, usersMap);
    }

    private static String getElementText(Element parent, String tagName) {
        Element elem = getChildElement(parent, tagName);
        return elem != null ? elem.getTextContent().trim() : "";
    }

    private static Element getChildElement(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return (Element) list.item(0);
        }
        return null;
    }
}
