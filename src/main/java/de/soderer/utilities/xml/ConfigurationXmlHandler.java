package de.soderer.utilities.xml;

import java.io.InputStream;
import java.util.Hashtable;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ConfigurationXmlHandler extends DefaultHandler {

	private static final String TAGNAME_ROOT = "Configuration";
	private static final String TAGNAME_ENTRY = "Entry";
	private static final String ATTRIBUTENAME_ENTRY_NAME = "name";
	private static final String ATTRIBUTENAME_ENTRY_value = "value";

	private final Map<String, String> values = new Hashtable<>();

	private boolean foundRootTag = false;

	public ConfigurationXmlHandler(final InputStream inputStream) throws Exception {
		super();
		try {
			final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
			// Harden against XXE: no DOCTYPE, no external (parameter) entities, no external DTD loading
			saxParserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			saxParserFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			saxParserFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			saxParserFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			saxParserFactory.setXIncludeAware(false);
			saxParserFactory.newSAXParser().parse(new InputSource(inputStream), this);
		} catch (final Exception ex) {
			throw new Exception("Error while parsing XmlConfiguration", ex);
		}
	}

	public Map<String, String> getConfigurationValueMap() {
		return values;
	}

	@Override
	public void startElement(final String namespaceURI, final String localName, final String qName, final Attributes attributes) throws SAXException {
		if (!foundRootTag) {
			if (!qName.equals(TAGNAME_ROOT)) {
				throw new SAXException("Invalid RootTag in XmlConfiguration: " + qName);
			} else {
				foundRootTag = true;
			}
		} else if (qName.equals(TAGNAME_ENTRY)) {
			values.put(attributes.getValue(ATTRIBUTENAME_ENTRY_NAME), attributes.getValue(ATTRIBUTENAME_ENTRY_value));
		}
	}

	@Override
	public void endElement(final String namespaceURI, final String localName, final String qName) throws SAXException {
		if (qName.equals(TAGNAME_ROOT)) {
			// do noting
		} else if (qName.equals(TAGNAME_ENTRY)) {
			// do noting
		}
	}
}
