package engine.exception;

// Exception thrown when specified XML file path does not exist.
public class XmlFileNotFoundException extends XMLException {
    public XmlFileNotFoundException(String filePath) {
        super("XML file not found at path: " + filePath);
    }
}
