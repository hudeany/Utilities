package de.soderer.utilities.xml;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads configuration values from an XML document of the form
 * {@code <Configuration><Entry name="..." value="..."/></Configuration>}.
 * DOCTYPE declarations and external entities are rejected (protection against XXE).
 */
public class ConfigurationXmlHandler extends DefaultHandler {

	private static final String TAGNAME_ROOT = "Configuration";
	private static final String TAGNAME_ENTRY = "Entry";
	private static final String ATTRIBUTENAME_ENTRY_NAME = "name";
	private static final String ATTRIBUTENAME_ENTRY_value = "value";

	private final Map<String, String> values = new LinkedHashMap<>();

	private boolean foundRootTag = false;

	/**
	 * Parses the configuration from the given stream. The stream is not closed.
	 *
	 * @param inputStream stream with the XML configuration
	 * @throws Exception if the XML is invalid, has another root tag or contains an entry without name
	 */
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
			// The SAX parser closes its input when done, but the stream belongs to the caller
			final InputStream nonClosingInputStream = new FilterInputStream(inputStream) {
				@Override
				public void close() {
					// Do not close the caller's stream
				}
			};
			saxParserFactory.newSAXParser().parse(new InputSource(nonClosingInputStream), this);
		} catch (final Exception ex) {
			throw new Exception("Error while parsing XmlConfiguration", ex);
		}
	}

	/**
	 * Returns the parsed configuration values.
	 *
	 * @return map of entry names to values, in document order; a value is null if the entry has no value attribute
	 */
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
			final String name = attributes.getValue(ATTRIBUTENAME_ENTRY_NAME);
			if (name == null) {
				throw new SAXException("Missing attribute '" + ATTRIBUTENAME_ENTRY_NAME + "' of tag '" + TAGNAME_ENTRY + "' in XmlConfiguration");
			}
			values.put(name, attributes.getValue(ATTRIBUTENAME_ENTRY_value));
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
