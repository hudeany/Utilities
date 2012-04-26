package de.soderer.utilities;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.SAXParser;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author Andreas Soderer
 */
public class ConfigurationXml extends DefaultHandler {

	private static final String TAGNAME_ROOT = "Configuration";
	private static final String TAGNAME_ENTRY = "Entry";
	private static final String ATTRIBUTENAME_ENTRY_NAME = "name";
	private static final String ATTRIBUTENAME_ENTRY_value = "value";

	private Map<String, String> values = new Hashtable<String, String>();

	private boolean foundRootTag = false;

	public ConfigurationXml(InputStream inputStream) throws Exception {
		super();
		try {
			XmlParserPool parserPool = XmlParserPool.getInstance();
			SAXParser parser = parserPool.borrowObject();
			parser.parse(new InputSource(inputStream), this);
			parserPool.returnObject(parser);
		} catch (Exception ex) {
			throw new Exception("Fehler beim Parsen der XmlConfiguration", ex);
		}
	}
	
	public Map<String, String> getConfigurationValueMap() {
		return values;
	}

	public void startElement(String namespaceURI, String localName, String qName, Attributes attributes)
			throws SAXException {
		if (!foundRootTag) {
			if (!qName.equals(TAGNAME_ROOT))
				throw new SAXException("Ungültiges RootTag in XmlConfiguration: " + qName);
			else
				foundRootTag = true;
		}
		else if (qName.equals(TAGNAME_ENTRY)) {
			values.put(attributes.getValue(ATTRIBUTENAME_ENTRY_NAME), attributes.getValue(ATTRIBUTENAME_ENTRY_value));
		}
	}

	public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
		if (qName.equals(TAGNAME_ROOT)) {
		}
		else if (qName.equals(TAGNAME_ENTRY)) {
		}
	}
}
