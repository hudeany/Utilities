package de.soderer.utilities.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.soderer.utilities.StringUtilities;

/**
 * Utilities class to work with XML documents.
 */
public class XmlUtilities {
	private XmlUtilities() {
		// Utility class with static methods only
	}

	/**
	 * Creates a DocumentBuilderFactory that is hardened against XXE attacks
	 * (external entity resolution, external DTD loading, XInclude).
	 * Use this instead of calling DocumentBuilderFactory.newInstance() directly.
	 */
	private static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		documentBuilderFactory.setXIncludeAware(false);
		documentBuilderFactory.setExpandEntityReferences(false);
		return documentBuilderFactory;
	}

	/**
	 * Same hardening as {@link #createSecureDocumentBuilderFactory()}, but still allows a DOCTYPE
	 * declaration to be present (needed e.g. for XHTML documents). External entities and external
	 * DTDs are still never resolved/loaded, which is what actually prevents XXE.
	 */
	private static DocumentBuilderFactory createSecureDocumentBuilderFactoryAllowingDoctype() throws ParserConfigurationException {
		final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		documentBuilderFactory.setXIncludeAware(false);
		return documentBuilderFactory;
	}

	/**
	 * Default timeout in milliseconds for connecting to and reading from a URL.
	 */
	private static final int DOWNLOAD_TIMEOUT_MILLIS = 60_000;

	/**
	 * Encoding attribute inside an XML declaration
	 */
	private static final Pattern XML_DECLARATION_ENCODING_PATTERN = Pattern.compile("\\sencoding\\s*=\\s*[\"']([A-Za-z][A-Za-z0-9._\\-]*)[\"']", Pattern.CASE_INSENSITIVE);

	/**
	 * Checks whether data starts with an XML declaration ("&lt;?xml" followed by whitespace).
	 * A leading UTF-8 byte order mark is skipped.
	 *
	 * @param data data to check
	 * @return true if the data starts with an XML declaration
	 */
	public static boolean isXmlDocument(final byte[] data) {
		if (data == null) {
			return false;
		}
		int offset = 0;
		if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
			offset = 3;
		}
		if (data.length < offset + 6) {
			return false;
		}
		final String start = new String(data, offset, 6, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
		return start.startsWith("<?xml") && Character.isWhitespace(start.charAt(5));
	}

	/**
	 * Gets the empty document.
	 *
	 * @return the empty document
	 * @throws ParserConfigurationException
	 *             the parser configuration exception
	 */
	public static Document getEmptyDocument() throws ParserConfigurationException {
		final DocumentBuilderFactory documentBuilderFactory = createSecureDocumentBuilderFactory();
		final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
		final Document document = documentBuilder.newDocument();
		return document;
	}

	/**
	 * Download and parse xml file.
	 * Redirects are only followed within the same protocol (Java default), so a redirect from http to https
	 * results in an exception with the HTTP code. Connecting and each read time out after 60 seconds.
	 *
	 * @param url
	 *            the url
	 * @param proxy
	 *            the proxy to use, or null for the default proxy settings
	 * @return the document
	 * @throws Exception
	 *             if the download fails (with the HTTP code, if it was not 200) or the data is no valid XML
	 */
	public static Document downloadAndParseXmlFile(final String url, final Proxy proxy) throws Exception {
		final URLConnection urlConnection;
		if (proxy == null) {
			urlConnection = URI.create(url).toURL().openConnection();
		} else {
			urlConnection = URI.create(url).toURL().openConnection(proxy);
		}
		// Without timeouts an unresponsive server would block forever
		urlConnection.setConnectTimeout(DOWNLOAD_TIMEOUT_MILLIS);
		urlConnection.setReadTimeout(DOWNLOAD_TIMEOUT_MILLIS);

		try {
			// The HTTP code is checked on the same connection, so the URL is requested only once
			if (urlConnection instanceof HttpURLConnection) {
				final int httpCode = ((HttpURLConnection) urlConnection).getResponseCode();
				if (httpCode != HttpURLConnection.HTTP_OK) {
					throw new Exception("Cannot retrieve xml document from Url '" + url + "'. HTTP code was: " + httpCode);
				}
			}

			try (BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream())) {
				final DocumentBuilderFactory documentBuilderFactory = createSecureDocumentBuilderFactory();
				final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
				return documentBuilder.parse(new InputSource(inputStream));
			}
		} finally {
			if (urlConnection instanceof HttpURLConnection) {
				((HttpURLConnection) urlConnection).disconnect();
			}
		}
	}

	/**
	 * Parse xml file.
	 *
	 * @param file
	 *            the file
	 * @return the document
	 */
	public static Document parseXmlFile(final File file) {
		try (BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file))) {
			final DocumentBuilderFactory documentBuilderFactory = createSecureDocumentBuilderFactory();
			final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			final Document document = documentBuilder.parse(new InputSource(inputStream));
			return document;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
	}


	/**
	 * Parse xml bytearray.
	 *
	 * @param xmlData bytearray
	 * @return the document
	 */
	public static Document parseXmlFile(final byte[] xmlData) {
		try (BufferedInputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(xmlData))) {
			final DocumentBuilderFactory documentBuilderFactory = createSecureDocumentBuilderFactory();
			final DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			final Document document = documentBuilder.parse(inputStream);
			return document;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
	}

	/**
	 * Gets a single x path node.
	 *
	 * @param document
	 *            the document
	 * @param xpathExpression
	 *            the xpath expression
	 * @return the single x path node
	 */
	public static Node getSingleXPathNode(final Document document, final String xpathExpression) {
		if (document == null) {
			return null;
		} else if (StringUtilities.isBlank(xpathExpression)) {
			return null;
		} else {
			try {
				final NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpathExpression, document, XPathConstants.NODESET);
				if (nodeList != null && nodeList.getLength() > 0) {
					return nodeList.item(0);
				} else {
					return null;
				}
			} catch (@SuppressWarnings("unused") final XPathExpressionException e) {
				return null;
			}
		}
	}

	/**
	 * Parses a xml string.
	 *
	 * @param xmlInput
	 *            the xml input
	 * @return the document
	 * @throws Exception
	 *             the exception
	 */
	public static Document parseXmlString(final String xmlInput) throws Exception {
		final boolean validation = false;
		final boolean ignoreWhitespace = true;
		final boolean ignoreComments = true;
		final boolean putCDATAIntoText = true;
		final boolean createEntityRefs = false;

		final DocumentBuilderFactory dbf = createSecureDocumentBuilderFactory();

		// set the configuration options
		dbf.setValidating(validation);
		dbf.setIgnoringComments(ignoreComments);
		dbf.setIgnoringElementContentWhitespace(ignoreWhitespace);
		dbf.setCoalescing(putCDATAIntoText);
		// The opposite of creating entity ref nodes is expanding them inline
		dbf.setExpandEntityReferences(!createEntityRefs);

		DocumentBuilder db = null;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();
			doc = db.parse(new InputSource(new StringReader(xmlInput)));
			return doc;

		} catch (final Exception e) {
			throw new Exception("Error while parsing xml", e);
		}
	}

	/**
	 * Ignores DTD for better performance. This works faster for XHTML, because it doesn't download the dtd from any website like "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"
	 *
	 * @param xmlInput
	 *            the xml input
	 * @return the document
	 * @throws Exception
	 *             the exception
	 */
	public static Document parseXmlStringIngoringDtd(final String xmlInput) throws Exception {
		if (xmlInput == null) {
			throw new Exception("XML-Data is null");
		} else if (StringUtilities.isBlank(xmlInput)) {
			throw new Exception("XML-Data is empty");
		}

		final boolean validation = false;
		final boolean ignoreWhitespace = true;
		final boolean ignoreComments = true;
		final boolean putCDATAIntoText = true;
		final boolean createEntityRefs = false;

		final DocumentBuilderFactory dbf = createSecureDocumentBuilderFactoryAllowingDoctype();

		// set the configuration options
		dbf.setValidating(validation);
		dbf.setIgnoringComments(ignoreComments);
		dbf.setIgnoringElementContentWhitespace(ignoreWhitespace);
		dbf.setCoalescing(putCDATAIntoText);
		// The opposite of creating entity ref nodes is expanding them inline
		dbf.setExpandEntityReferences(!createEntityRefs);

		DocumentBuilder db = null;
		Document doc = null;
		try {
			db = dbf.newDocumentBuilder();

			db.setEntityResolver(new EntityResolver() {
				@Override
				public InputSource resolveEntity(final String publicId, final String systemId) throws SAXException, IOException {
					return new InputSource(new StringReader(""));
				}
			});

			doc = db.parse(new InputSource(new StringReader(xmlInput)));
			return doc;
		} catch (final Exception e) {
			throw new Exception("Error while parsing xml", e);
		}
	}

	/**
	 * Returns the content of a simple text tag If there are more than one text node or other nodetypes it will return null.
	 *
	 * @param node
	 *            the node
	 * @return the simple text value from node
	 */
	public static String getSimpleTextValueFromNode(final Node node) {
		if (node != null && node.getChildNodes().getLength() == 1 && node.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
			return node.getChildNodes().item(0).getTextContent();
		} else {
			return null;
		}
	}

	/**
	 * Gets a attribute value.
	 *
	 * @param pNode
	 *            the p node
	 * @param pAttributeName
	 *            the p attribute name
	 * @return the attribute value
	 */
	public static String getAttributeValue(final Node pNode, final String pAttributeName) {
		final NamedNodeMap attributes = pNode.getAttributes();
		if (attributes != null) {
			// XML names are case sensitive, so an exact match takes precedence over a case insensitive one
			final Node exactMatch = attributes.getNamedItem(pAttributeName);
			if (exactMatch != null) {
				return exactMatch.getNodeValue();
			}
			for (int i = 0; i < attributes.getLength(); i++) {
				if (attributes.item(i).getNodeName().equalsIgnoreCase(pAttributeName)) {
					return attributes.item(i).getNodeValue();
				}
			}
		}

		return null;
	}

	/**
	 * Gets a node value.
	 *
	 * @param pNode
	 *            the p node
	 * @return the node value
	 */
	public static String getNodeValue(final Node pNode) {
		if (pNode.getNodeValue() != null) {
			return pNode.getNodeValue();
		} else if (pNode.getFirstChild() != null) {
			return getNodeValue(pNode.getFirstChild());
		} else {
			return null;
		}
	}

	/**
	 * Gets the node as string.
	 *
	 * @param pNode
	 *            the p node
	 * @param encoding
	 *            the encoding
	 * @param pRemoveXmlLine
	 *            the p remove xml line
	 * @return the node as string
	 * @throws Exception
	 *             the exception
	 */
	public static String getNodeAsString(final Node pNode, final Charset encoding, final boolean pRemoveXmlLine) throws Exception {
		final StringWriter writer = new StringWriter();
		final StreamResult result = new StreamResult(writer);
		transformNode(pNode, encoding, pRemoveXmlLine, result);
		return writer.toString();
	}

	/**
	 * Gets the node as raw.
	 *
	 * @param pNode
	 *            the p node
	 * @param encoding
	 *            the encoding
	 * @param pRemoveXmlLine
	 *            the p remove xml line
	 * @return the node as raw
	 * @throws Exception
	 *             the exception
	 */
	public static byte[] getNodeAsRaw(final Node pNode, final Charset encoding, final boolean pRemoveXmlLine) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final StreamResult result = new StreamResult(outputStream);
		transformNode(pNode, encoding, pRemoveXmlLine, result);
		return outputStream.toByteArray();
	}

	/**
	 * Write document to file.
	 *
	 * @param document
	 *            the document
	 * @param encoding
	 *            the encoding
	 * @param file
	 *            the file
	 * @throws Exception
	 *             the exception
	 */
	public static void writeDocumentToFile(final Document document, final Charset encoding, final File file) throws Exception {
		try (OutputStream outputStream = new FileOutputStream(file)) {
			final StreamResult result = new StreamResult(outputStream);
			transformNode(document, encoding, false, result);
		} catch (final Exception e) {
			// Do not leave a truncated file behind
			file.delete();
			throw e;
		}
	}

	/**
	 * Transform node.
	 *
	 * @param node
	 *            the node
	 * @param encoding
	 *            the encoding, or null for UTF-8
	 * @param removeXmlHeader
	 *            the remove xml header
	 * @param result
	 *            the result
	 * @throws Exception
	 *             the exception
	 */
	private static void transformNode(final Node node, Charset encoding, final boolean removeXmlHeader, final StreamResult result) throws Exception {
		if (encoding == null) {
			encoding = StandardCharsets.UTF_8;
		}

		TransformerFactory transformerFactory = null;
		Transformer transformer = null;
		DOMSource source = null;

		try {
			transformerFactory = TransformerFactory.newInstance();
			if (transformerFactory == null) {
				throw new Exception("TransformerFactory error");
			}

			transformer = transformerFactory.newTransformer();
			if (transformer == null) {
				throw new Exception("Transformer error");
			}

			transformer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
			if (removeXmlHeader) {
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			} else {
				transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			}

			source = new DOMSource(node);

			transformer.transform(source, result);
		} catch (final TransformerFactoryConfigurationError e) {
			throw new Exception("TransformerFactoryConfigurationError", e);
		} catch (final TransformerConfigurationException e) {
			throw new Exception("TransformerConfigurationException", e);
		} catch (final TransformerException e) {
			throw new Exception("TransformerException", e);
		}
	}

	/**
	 * Parses XML data and validates it against an XML schema.
	 * Only the given schema is used, schema locations named in the XML data (xsi:schemaLocation) are ignored and
	 * never loaded. Trailing zero bytes of the data are ignored.
	 *
	 * @param pData
	 *            the XML data, null results in null
	 * @param byteEncoding
	 *            the encoding of the data, or null to detect it from the XML declaration
	 * @param xsdFileName
	 *            the XML schema file path or URL, or null for no validation
	 * @return the document
	 * @throws Exception
	 *             if the data is empty, invalid or does not match the schema (warnings included)
	 */
	public static Document parseXMLDataAndXSDVerifyByDOM(byte[] pData, final String byteEncoding, final String xsdFileName) throws Exception {
		try {
			if (pData == null) {
				return null;
			} else if (pData.length == 0) {
				throw new Exception("XML data is empty");
			}

			if (pData[pData.length - 1] == 0) {
				// Remove trailing zero bytes on byte level, a conversion to String would corrupt non UTF-8 data
				int length = pData.length;
				while (length > 0 && pData[length - 1] == 0) {
					length--;
				}
				pData = Arrays.copyOf(pData, length);
			}

			final DocumentBuilderFactory docBuilderFactory = createSecureDocumentBuilderFactory();
			docBuilderFactory.setNamespaceAware(true);

			if (xsdFileName != null) {
				// A Schema object validates only against its own grammar, xsi:schemaLocation hints of the data are not loaded
				final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
				final File xsdFile = new File(xsdFileName);
				final StreamSource schemaSource = xsdFile.isFile() ? new StreamSource(xsdFile) : new StreamSource(xsdFileName);
				final Schema schema = schemaFactory.newSchema(schemaSource);
				docBuilderFactory.setSchema(schema);
			}

			final DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

			final InputSource inputSource = new InputSource(new ByteArrayInputStream(pData));
			if (byteEncoding != null) {
				inputSource.setEncoding(byteEncoding);
			}
			final ParseErrorHandler errorHandler = new ParseErrorHandler();
			docBuilder.setErrorHandler(errorHandler);

			final Document xmlDocument = docBuilder.parse(inputSource);

			if (errorHandler.problemsOccurred()) {
				throw new Exception("ErrorConstGlobals.XML_SCHEMA_ERROR " + xsdFileName + " " + errorHandler.getMessage());
			} else {
				return xmlDocument;
			}
		} catch (final ParserConfigurationException e) {
			throw new Exception("ErrorConstException.XML_PROCESSING " + e.getClass().getSimpleName() + " " + e.getMessage(), e);
		} catch (final SAXException e) {
			throw new Exception("ErrorConstException.XML_PROCESSING " + e.getClass().getSimpleName() + " " + e.getMessage(), e);
		} catch (final IOException e) {
			throw new Exception("ErrorConstException.XML_PROCESSING " + e.getClass().getSimpleName() + " " + e.getMessage(), e);
		}
	}

	/**
	 * The Class ParseErrorHandler.
	 */
	private static class ParseErrorHandler implements ErrorHandler {

		/** The warnings. */
		ArrayList<SAXParseException> warnings = null;

		/** The errors. */
		ArrayList<SAXParseException> errors = null;

		/** The fatal errors. */
		ArrayList<SAXParseException> fatalErrors = null;

		/** The problems. */
		boolean problems = false;

		/**
		 * Problems occurred.
		 *
		 * @return true, if problems occurred
		 */
		public boolean problemsOccurred() {
			return problems;
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.xml.sax.ErrorHandler#warning(org.xml.sax.SAXParseException)
		 */
		@Override
		public void warning(final SAXParseException exception) throws SAXException {
			problems = true;
			if (warnings == null) {
				warnings = new ArrayList<>();
			}
			warnings.add(exception);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.xml.sax.ErrorHandler#error(org.xml.sax.SAXParseException)
		 */
		@Override
		public void error(final SAXParseException exception) throws SAXException {
			problems = true;
			if (errors == null) {
				errors = new ArrayList<>();
			}
			errors.add(exception);
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.xml.sax.ErrorHandler#fatalError(org.xml.sax.SAXParseException)
		 */
		@Override
		public void fatalError(final SAXParseException exception) throws SAXException {
			problems = true;
			if (fatalErrors == null) {
				fatalErrors = new ArrayList<>();
			}
			fatalErrors.add(exception);
		}

		/**
		 * Gets the message.
		 *
		 * @return the message
		 */
		public String getMessage() {
			if (fatalErrors != null && fatalErrors.size() > 0) {
				return fatalErrors.get(0).getMessage();
			} else if (errors != null && errors.size() > 0) {
				return errors.get(0).getMessage();
			} else if (warnings != null && warnings.size() > 0) {
				return warnings.get(0).getMessage();
			} else {
				return "No ParserErrors occured";
			}
		}
	}

	/**
	 * Convert xml to string.
	 *
	 * @param pDocument
	 *            the p document
	 * @param encoding
	 *            the encoding
	 * @return the string
	 * @throws Exception
	 *             the exception
	 */
	public static String convertXML2String(final Document pDocument, final Charset encoding) throws Exception {
		TransformerFactory transformerFactory = null;
		Transformer transformer = null;
		DOMSource domSource = null;
		StreamResult result = null;

		try {
			transformerFactory = TransformerFactory.newInstance();
			if (transformerFactory == null) {
				throw new Exception("TransformerFactory error");
			}

			transformer = transformerFactory.newTransformer();
			if (transformer == null) {
				throw new Exception("Transformer error");
			}

			if (encoding != null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
			} else {
				transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
			}

			domSource = new DOMSource(pDocument);
			try (StringWriter writer = new java.io.StringWriter()) {
				result = new StreamResult(writer);

				transformer.transform(domSource, result);

				return writer.toString();
			}
		} catch (final TransformerFactoryConfigurationError e) {
			throw new Exception("TransformerFactoryConfigurationError", e);
		} catch (final TransformerConfigurationException e) {
			throw new Exception("TransformerConfigurationException", e);
		} catch (final TransformerException e) {
			throw new Exception("TransformerException", e);
		}
	}

	/**
	 * Convert xml to byte array.
	 *
	 * @param pDocument
	 *            the p document
	 * @param encoding
	 *            the encoding
	 * @return the byte[]
	 * @throws Exception
	 *             the exception
	 */
	public static byte[] convertXML2ByteArray(final Node pDocument, final Charset encoding) throws Exception {
		TransformerFactory transformerFactory = null;
		Transformer transformer = null;
		DOMSource domSource = null;
		StreamResult result = null;

		try {
			transformerFactory = TransformerFactory.newInstance();
			if (transformerFactory == null) {
				throw new Exception("TransformerFactory error");
			}

			transformer = transformerFactory.newTransformer();
			if (transformer == null) {
				throw new Exception("Transformer error");
			}

			if (encoding != null) {
				transformer.setOutputProperty(OutputKeys.ENCODING, encoding.name());
			} else {
				transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
			}

			domSource = new DOMSource(pDocument);
			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
				result = new StreamResult(outputStream);

				transformer.transform(domSource, result);

				return outputStream.toByteArray();
			}
		} catch (final TransformerFactoryConfigurationError e) {
			throw new Exception("TransformerFactoryConfigurationError", e);
		} catch (final TransformerConfigurationException e) {
			throw new Exception("TransformerConfigurationException", e);
		} catch (final TransformerException e) {
			throw new Exception("TransformerException", e);
		}
	}

	// public static void parseXMLDataAndXSDVerifyBySAX(byte[] pUploadXmlDocumentArray, UploadXMLContentHandler pContentHandler) throws Exception, Exception {
	// try {
	// if (pUploadXmlDocumentArray == null || pUploadXmlDocumentArray.length == 0) {
	// throw new Exception(ErrorConstAtsGlobals.UPLOAD_XML_EMPTY);
	// }
	// if (pUploadXmlDocumentArray[0] != "<".getBytes(StandardCharsets.UTF_8)[0]) {
	// throw new Exception(ErrorConstAtsGlobals.INVALID_XML_DATA, "XML data is invalid");
	// }
	//
	// SAXParser parser = new SAXParser();
	// String schemaFileName = pContentHandler.getSchemaFileName();
	// if (schemaFileName != null) {
	// URI schemaFileURI = Utilities.getResource(schemaFileName);
	// if (schemaFileURI == null) {
	// throw new Exception("XML-Schema was not found: " + schemaFileName);
	// }
	// parser.setFeature("http://xml.org/sax/features/validation", true);
	// parser.setFeature("http://apache.org/xml/features/validation/schema", true);
	// parser.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
	// parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation", schemaFileURI.toString());
	// }
	//
	// parser.setContentHandler(pContentHandler);
	//
	// ParseErrorHandler errorHandler = new ParseErrorHandler();
	// parser.setErrorHandler(errorHandler);
	//
	// InputSource inputSource = new InputSource(new ByteArrayInputStream(pUploadXmlDocumentArray));
	// parser.parse(inputSource);
	//
	// if (errorHandler.problemsOccurred()) {
	// throw new Exception(ErrorConstAtsGlobals.XML_SCHEMA_ERROR, pContentHandler.getSchemaFileName(), errorHandler.getMessage());
	// }
	//
	// /*
	// * String parserClass = "org.apache.xerces.parsers.SAXParser"; String validationFeature = "http://xml.org/sax/features/validation"; String schemaFeature =
	// * "http://apache.org/xml/features/validation/schema";
	// *
	// * XMLReader r = XMLReaderFactory.createXMLReader(parserClass); r.setFeature(validationFeature,true); r.setFeature(schemaFeature,true);
	// *
	// * InputSource inputSourcSAX = new InputSource(pDataStream);
	// * if (byteEncoding != null) {
	// * inputSourcSAX.setEncoding(byteEncoding);
	// * }
	// * ParseErrorHandler errorHandlerSax = new ParseErrorHandler();
	// * r.setErrorHandler(errorHandlerSax); r.parse(inputSourcSAX);
	// */
	// } catch (SAXException e) {
	// logger.error(e.getClass().getSimpleName(), e);
	// throw new Exception(ErrorConstException.XML_PROCESSING, e.getClass().getSimpleName(), e.getMessage());
	// } catch (IOException e) {
	// logger.error(e.getClass().getSimpleName() + " while XML processing", e);
	// throw new Exception(ErrorConstException.XML_PROCESSING, e.getClass().getSimpleName(), e.getMessage());
	// }
	// }

	/**
	 * Creates the root tag node.
	 *
	 * @param baseDocument
	 *            the base document
	 * @param rootTagName
	 *            the root tag name
	 * @return the element
	 */
	public static Element createRootTagNode(final Document baseDocument, final String rootTagName) {
		final Element newNode = baseDocument.createElement(rootTagName);
		baseDocument.appendChild(newNode);
		return newNode;
	}

	/**
	 * Append node.
	 *
	 * @param baseNode
	 *            the base node
	 * @param tagName
	 *            the tag name
	 * @return the element
	 */
	public static Element appendNode(final Node baseNode, final String tagName) {
		final Element newNode = getDocumentOfNode(baseNode).createElement(tagName);
		baseNode.appendChild(newNode);
		return newNode;
	}

	/**
	 * Append text value node.
	 *
	 * @param baseNode
	 *            the base node
	 * @param tagName
	 *            the tag name
	 * @param tagValue
	 *            the tag value
	 * @return the element
	 */
	public static Element appendTextValueNode(final Node baseNode, final String tagName, final int tagValue) {
		return appendTextValueNode(baseNode, tagName, Integer.toString(tagValue));
	}

	/**
	 * Append text value node.
	 *
	 * @param baseNode
	 *            the base node
	 * @param tagName
	 *            the tag name
	 * @param tagValue
	 *            the tag value
	 * @return the element
	 */
	public static Element appendTextValueNode(final Node baseNode, final String tagName, final double tagValue) {
		return appendTextValueNode(baseNode, tagName, Double.toString(tagValue));
	}

	/**
	 * Append text value node.
	 *
	 * @param baseNode
	 *            the base node
	 * @param tagName
	 *            the tag name
	 * @param tagValue
	 *            the tag value
	 * @return the element
	 */
	public static Element appendTextValueNode(final Node baseNode, final String tagName, final String tagValue) {
		final Element newNode = appendNode(baseNode, tagName);
		if (tagValue == null) {
			newNode.appendChild(getDocumentOfNode(baseNode).createTextNode("<null>"));
		} else {
			newNode.appendChild(getDocumentOfNode(baseNode).createTextNode(tagValue));
		}
		return newNode;
	}

	/**
	 * Append attribute.
	 *
	 * @param baseNode
	 *            the base node
	 * @param attributeName
	 *            the attribute name
	 * @param attributeValue
	 *            the attribute value
	 */
	public static void appendAttribute(final Element baseNode, final String attributeName, final boolean attributeValue) {
		appendAttribute(baseNode, attributeName, attributeValue ? "true" : "false");
	}

	/**
	 * Append attribute.
	 *
	 * @param baseNode
	 *            the base node
	 * @param attributeName
	 *            the attribute name
	 * @param attributeValue
	 *            the attribute value
	 */
	public static void appendAttribute(final Element baseNode, final String attributeName, final int attributeValue) {
		appendAttribute(baseNode, attributeName, Integer.toString(attributeValue));
	}

	/**
	 * Append attribute.
	 *
	 * @param baseNode
	 *            the base node
	 * @param attributeName
	 *            the attribute name
	 * @param attributeValue
	 *            the attribute value
	 */
	public static void appendAttribute(final Element baseNode, final String attributeName, final String attributeValue) {
		final Attr typeAttribute = baseNode.getOwnerDocument().createAttribute(attributeName);
		if (attributeValue == null) {
			typeAttribute.setNodeValue("<null>");
		} else {
			typeAttribute.setNodeValue(attributeValue);
		}
		baseNode.setAttributeNode(typeAttribute);
	}

	/**
	 * Removes the attribute. Nothing happens if the attribute does not exist.
	 *
	 * @param baseNode
	 *            the base node
	 * @param attributeName
	 *            the attribute name
	 */
	public static void removeAttribute(final Element baseNode, final String attributeName) {
		baseNode.removeAttribute(attributeName);
	}

	/**
	 * Adds the comment to document.
	 *
	 * @param pDocument
	 *            the p document
	 * @param pComment
	 *            the p comment
	 * @return the document
	 */
	public static Document addCommentToDocument(final Document pDocument, final String pComment) {
		// The first child may be a DOCTYPE or a comment, so the comment is added to the root element
		final Node rootElement = pDocument.getDocumentElement();
		if (rootElement != null) {
			rootElement.appendChild(pDocument.createComment(pComment));
		} else {
			pDocument.appendChild(pDocument.createComment(pComment));
		}
		return pDocument;
	}

	/**
	 * Gets the encoding of xml byte array. A byte order mark (UTF-8, UTF-16) takes precedence, otherwise the encoding
	 * attribute of the XML declaration is used.
	 *
	 * @param xmlData
	 *            the xml data
	 * @return the encoding of xml byte array
	 * @throws Exception
	 *             if there is neither a byte order mark nor an XML declaration with a supported encoding
	 */
	public static Charset getEncodingOfXmlByteArray(final byte[] xmlData) throws Exception {
		if (xmlData == null) {
			throw new Exception("XmlByteArray-Encoding nicht ermittelbar");
		} else if (xmlData.length >= 3 && (xmlData[0] & 0xFF) == 0xEF && (xmlData[1] & 0xFF) == 0xBB && (xmlData[2] & 0xFF) == 0xBF) {
			return StandardCharsets.UTF_8;
		} else if (xmlData.length >= 2 && (xmlData[0] & 0xFF) == 0xFE && (xmlData[1] & 0xFF) == 0xFF) {
			return StandardCharsets.UTF_16BE;
		} else if (xmlData.length >= 2 && (xmlData[0] & 0xFF) == 0xFF && (xmlData[1] & 0xFF) == 0xFE) {
			return StandardCharsets.UTF_16LE;
		}

		// The XML declaration consists of ASCII characters only, so the encoding name can be read in any ASCII compatible encoding
		final String xmlDataStart = new String(xmlData, 0, Math.min(xmlData.length, 1024), StandardCharsets.ISO_8859_1);
		if (!xmlDataStart.regionMatches(true, 0, "<?xml", 0, 5)) {
			throw new Exception("XmlByteArray-Encoding nicht ermittelbar");
		}
		final int declarationEnd = xmlDataStart.indexOf("?>");
		if (declarationEnd < 0) {
			throw new Exception("XmlByteArray-Encoding nicht ermittelbar");
		}
		final Matcher matcher = XML_DECLARATION_ENCODING_PATTERN.matcher(xmlDataStart.substring(0, declarationEnd));
		if (!matcher.find()) {
			throw new Exception("XmlByteArray-Encoding nicht ermittelbar");
		}
		try {
			return Charset.forName(matcher.group(1));
		} catch (final IllegalArgumentException e) {
			throw new Exception("XmlByteArray-Encoding nicht unterstützt: " + matcher.group(1), e);
		}
	}

	/**
	 * Returns the document of a node, which is the node itself for a document node.
	 */
	private static Document getDocumentOfNode(final Node node) {
		if (node instanceof Document) {
			return (Document) node;
		} else {
			return node.getOwnerDocument();
		}
	}

	/**
	 * Returns all direct simple text value subnodes and their values for a dataNode
	 *
	 * Example XML: {@code <dataNode> <a>1</a> <b>2</b> <c>3</c> </dataNode>} Returns: a=1, b=2, c=3.
	 *
	 * @param dataNode
	 *            the data node
	 * @return the simple values of node
	 */
	public static Map<String, String> getSimpleValuesOfNode(final Node dataNode) {
		final Map<String, String> returnMap = new LinkedHashMap<>();

		final NodeList list = dataNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			final Node node = list.item(i);
			if (node.getFirstChild() != null && node.getFirstChild().getNodeType() == Node.TEXT_NODE && node.getChildNodes().getLength() == 1) {
				returnMap.put(node.getNodeName(), node.getFirstChild().getNodeValue());
			}
		}

		return returnMap;
	}

	/**
	 * Gets the sub nodes.
	 *
	 * @param dataNode
	 *            the data node
	 * @return the sub nodes
	 */
	public static List<Node> getSubNodes(final Node dataNode) {
		final List<Node> returnList = new ArrayList<>();

		final NodeList list = dataNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			final Node node = list.item(i);
			returnList.add(node);
		}

		return returnList;
	}

	/**
	 * Returns the direct child elements of a node by their names (text, CDATA, comments and processing instructions
	 * are left out). If several children have the same name, the last one is returned.
	 *
	 * @param dataNode node to get the children of
	 * @return child nodes by name
	 */
	public static Map<String, Node> getChildNodesMap(final Node dataNode) {
		final Map<String, Node> childNodes = new LinkedHashMap<>();
		final NodeList childNodesList = dataNode.getChildNodes();
		for (int i = 0; i < childNodesList.getLength(); i++) {
			final Node childNode = childNodesList.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				childNodes.put(childNode.getNodeName(), childNode);
			}
		}

		return childNodes;
	}

	/**
	 * Returns the names of the direct child elements of a node (text, CDATA, comments and processing instructions
	 * are left out).
	 *
	 * @param dataNode
	 *            the data node
	 * @return the names of the child elements in document order
	 */
	public static List<String> getNodenamesOfChilds(final Node dataNode) {
		final List<String> returnList = new ArrayList<>();

		final NodeList list = dataNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			final Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				returnList.add(node.getNodeName());
			}
		}

		return returnList;
	}

	/**
	 * Removes the enclosing root tag of an XML String.
	 * If the root element has child elements, the first of them is returned as XML String, otherwise the root
	 * element's text value. Data not looking like XML is returned unchanged.
	 *
	 * @param data XML String, may be null or empty
	 * @return content of the root element
	 * @throws Exception if the data looks like XML but cannot be parsed
	 */
	public static String removeFrameTag(final String data) throws Exception {
		if (StringUtilities.isEmpty(data)) {
			return data;
		} else {
			final String changedData = data.trim();
			if (changedData.startsWith("<") && changedData.endsWith(">")) {
				final Document xmlDocument = parseXmlString(changedData);
				final Node rootNode = getRootNodeFromDocument(xmlDocument);
				final List<Node> childNodes = getChildNodes(rootNode);
				if (childNodes.size() > 0) {
					return getNodeAsString(childNodes.get(0), StandardCharsets.UTF_8, true);
				} else {
					return getNodeValue(rootNode);
				}
			} else {
				return data;
			}
		}
	}

	/**
	 * Returns the root element of a document.
	 *
	 * @param document document to read
	 * @return root element, or null if the document has none
	 */
	public static Node getRootNodeFromDocument(final Document document) {
		return document.getDocumentElement();
	}

	/**
	 * Returns the direct child elements of a node (text, CDATA, comments and processing instructions are left out).
	 *
	 * @param dataNode node to get the children of
	 * @return child nodes in document order
	 */
	public static List<Node> getChildNodes(final Node dataNode) {
		final List<Node> returnList = new ArrayList<>();

		final NodeList list = dataNode.getChildNodes();
		for (int i = 0; i < list.getLength(); i++) {
			final Node node = list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				returnList.add(node);
			}
		}

		return returnList;
	}

	/**
	 * Formats a document as indented XML String, with an XML declaration naming the given encoding.
	 *
	 * @param xmlDocument document to format
	 * @param encoding encoding named in the XML declaration
	 * @param throwExceptionOnError true to throw runtime errors, false to return null instead
	 * @return formatted XML, or null on error if throwExceptionOnError is false
	 */
	public static String formatXmlDocument(final Document xmlDocument, final Charset encoding, final boolean throwExceptionOnError) {
		try {
			final StringBuilder result = new StringBuilder();
			result.append("<?xml version=\"");
			result.append(xmlDocument.getXmlVersion());
			result.append("\" encoding=\"");
			result.append(encoding.name().toUpperCase(Locale.ROOT));
			result.append("\" standalone=\"");
			result.append(xmlDocument.getXmlStandalone() ? "yes" : "no");
			result.append("\"?>\n");

			for (int i = 0; i < xmlDocument.getChildNodes().getLength(); i++) {
				final Node xmlNode = xmlDocument.getChildNodes().item(i);
				result.append(formatXmlNode(xmlNode, 0));
			}
			return result.toString();
		} catch (final Exception e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		}
	}

	/**
	 * Formats a document as indented XML String, with an XML declaration naming the document's encoding (UTF-8 if unknown).
	 *
	 * @param xmlDocument document to format
	 * @param throwExceptionOnError true to throw runtime errors, false to return null instead
	 * @return formatted XML, or null on error if throwExceptionOnError is false
	 */
	public static String formatXmlDocument(final Document xmlDocument, final boolean throwExceptionOnError) {
		try {
			final StringBuilder result = new StringBuilder();
			result.append("<?xml version=\"");
			result.append(xmlDocument.getXmlVersion());
			result.append("\" encoding=\"");
			result.append(xmlDocument.getXmlEncoding() == null ? StandardCharsets.UTF_8 : xmlDocument.getXmlEncoding());
			result.append("\" standalone=\"");
			result.append(xmlDocument.getXmlStandalone() ? "yes" : "no");
			result.append("\"?>\n");

			for (int i = 0; i < xmlDocument.getChildNodes().getLength(); i++) {
				final Node xmlNode = xmlDocument.getChildNodes().item(i);
				result.append(formatXmlNode(xmlNode, 0));
			}
			return result.toString();
		} catch (final Exception e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		}
	}

	/**
	 * Formats a node and its children as indented XML String. Elements containing only text are written in one line,
	 * whitespace-only text is left out.
	 *
	 * @param xmlNode node to format
	 * @param indentationLevel number of tabs to indent the node with
	 * @return formatted XML of the node
	 */
	public static String formatXmlNode(final Node xmlNode, final int indentationLevel) {
		if (xmlNode.getNodeType() == Node.TEXT_NODE) {
			if (StringUtilities.isBlank(xmlNode.getNodeValue())) {
				return "";
			} else {
				return escapeTextValue(xmlNode.getNodeValue());
			}
		} else if (xmlNode.getNodeType() == Node.COMMENT_NODE) {
			if (StringUtilities.isBlank(xmlNode.getNodeValue())) {
				return "";
			} else {
				final StringBuilder result = new StringBuilder();
				result.append(StringUtilities.repeat("\t", indentationLevel));
				result.append("<!--");
				// Comment content is not escaped in XML
				result.append(xmlNode.getNodeValue());
				result.append("-->\n");
				return result.toString();
			}
		} else if (xmlNode.getNodeType() == Node.CDATA_SECTION_NODE) {
			return "<![CDATA[" + (xmlNode.getNodeValue() == null ? "" : xmlNode.getNodeValue().replace("]]>", "]]]]><![CDATA[>")) + "]]>";
		} else if (xmlNode.getNodeType() == Node.PROCESSING_INSTRUCTION_NODE) {
			return StringUtilities.repeat("\t", indentationLevel) + "<?" + xmlNode.getNodeName() + (StringUtilities.isEmpty(xmlNode.getNodeValue()) ? "" : " " + xmlNode.getNodeValue()) + "?>\n";
		} else if (xmlNode.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
			final DocumentType documentType = (DocumentType) xmlNode;
			final StringBuilder result = new StringBuilder();
			result.append(StringUtilities.repeat("\t", indentationLevel));
			result.append("<!DOCTYPE ");
			result.append(documentType.getName());
			if (documentType.getPublicId() != null) {
				result.append(" PUBLIC \"").append(documentType.getPublicId()).append("\"");
				if (documentType.getSystemId() != null) {
					result.append(" \"").append(documentType.getSystemId()).append("\"");
				}
			} else if (documentType.getSystemId() != null) {
				result.append(" SYSTEM \"").append(documentType.getSystemId()).append("\"");
			}
			if (StringUtilities.isNotBlank(documentType.getInternalSubset())) {
				// Entity and element declarations of the document itself must be kept
				result.append(" [").append(documentType.getInternalSubset()).append("]");
			}
			result.append(">\n");
			return result.toString();
		} else if (xmlNode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
			return "&" + xmlNode.getNodeName() + ";";
		} else if (xmlNode.getChildNodes().getLength() == 1 && xmlNode.getChildNodes().item(0).getNodeType() == Node.CDATA_SECTION_NODE) {
			// A single CDATA child is written inline, so no whitespace is added to its content
			return StringUtilities.repeat("\t", indentationLevel) + "<" + xmlNode.getNodeName() + formatAttributes(xmlNode, indentationLevel) + ">"
					+ formatXmlNode(xmlNode.getChildNodes().item(0), 0)
					+ "</" + xmlNode.getNodeName() + ">\n";
		} else if (xmlNode.getChildNodes().getLength() == 1 && xmlNode.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
			if (StringUtilities.isBlank(xmlNode.getChildNodes().item(0).getNodeValue())) {
				final StringBuilder result = new StringBuilder();
				result.append(StringUtilities.repeat("\t", indentationLevel));
				result.append("<");
				result.append(xmlNode.getNodeName());
				result.append(formatAttributes(xmlNode, indentationLevel));
				result.append(" />\n");
				return result.toString();
			} else {
				final StringBuilder result = new StringBuilder();
				result.append(StringUtilities.repeat("\t", indentationLevel));
				result.append("<");
				result.append(xmlNode.getNodeName());
				result.append(formatAttributes(xmlNode, indentationLevel));
				result.append(">");
				result.append(escapeTextValue(xmlNode.getChildNodes().item(0).getNodeValue()));
				result.append("</");
				result.append(xmlNode.getNodeName());
				result.append(">\n");
				return result.toString();
			}
		} else {
			final StringBuilder innerXml = new StringBuilder();
			if (xmlNode.getChildNodes() != null && xmlNode.getChildNodes().getLength() > 0) {
				for (int nodeIndex = 0; nodeIndex < xmlNode.getChildNodes().getLength(); nodeIndex++) {
					final Node xmlSubNode = xmlNode.getChildNodes().item(nodeIndex);
					if (xmlSubNode.getNodeType() == Node.TEXT_NODE) {
						if (StringUtilities.isNotBlank(xmlSubNode.getNodeValue())) {
							innerXml.append(escapeTextValue(xmlSubNode.getNodeValue()));
						}
					} else {
						innerXml.append(formatXmlNode(xmlSubNode, indentationLevel + 1));
					}
				}
			}

			final StringBuilder result = new StringBuilder();
			result.append(StringUtilities.repeat("\t", indentationLevel));
			result.append("<");
			result.append(xmlNode.getNodeName());
			result.append(formatAttributes(xmlNode, indentationLevel));
			if (innerXml.length() == 0) {
				result.append(" />\n");
			} else {
				result.append(">\n");
				result.append(innerXml);
				result.append(StringUtilities.repeat("\t", indentationLevel));
				result.append("</");
				result.append(xmlNode.getNodeName());
				result.append(">\n");
			}
			return result.toString();
		}
	}

	/**
	 * Escapes text content. "&gt;" is needed for the sequence "]]&gt;", a carriage return would otherwise be lost by
	 * line break normalization.
	 */
	private static String escapeTextValue(final String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\r", "&#13;");
	}

	/**
	 * Escapes an attribute value. Line breaks and tabs are written as character references, because a parser would
	 * otherwise normalize them to spaces.
	 */
	private static String escapeAttributeValue(final String value) {
		return value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
				.replace("\n", "&#10;").replace("\r", "&#13;").replace("\t", "&#9;");
	}

	private static String formatAttributes(final Node xmlNode, final int indentationLevel) {
		final StringBuilder result = new StringBuilder();
		if (xmlNode.getAttributes() != null && xmlNode.getAttributes().getLength() > 0) {
			if (xmlNode.getAttributes().getLength() <= 5) {
				for (int attributeIndex = 0; attributeIndex < xmlNode.getAttributes().getLength(); attributeIndex++) {
					final Node attributeNode = xmlNode.getAttributes().item(attributeIndex);
					result.append(" ");
					result.append(attributeNode.getNodeName());
					result.append("=\"");
					result.append(escapeAttributeValue(attributeNode.getNodeValue()));
					result.append("\"");
				}
			} else {
				result.append("\n");
				for (int attributeIndex = 0; attributeIndex < xmlNode.getAttributes().getLength(); attributeIndex++) {
					if (attributeIndex > 0) {
						result.append("\n");
					}
					final Node attributeNode = xmlNode.getAttributes().item(attributeIndex);
					result.append(StringUtilities.repeat("\t", indentationLevel + 1));
					result.append(attributeNode.getNodeName());
					result.append("=\"");
					result.append(escapeAttributeValue(attributeNode.getNodeValue()));
					result.append("\"");
				}
			}
		}
		return result.toString();
	}
}
