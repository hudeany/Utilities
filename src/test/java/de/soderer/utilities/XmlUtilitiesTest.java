package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.soderer.utilities.xml.XmlUtilities;

@SuppressWarnings("static-method")
public class XmlUtilitiesTest {
	private static final String SECRET = "TOP-SECRET-FILE-CONTENT";

	/**
	 * Creates a temporary file with the given content, deleted on JVM exit.
	 */
	private static File createTempFile(final String suffix, final String content) throws Exception {
		final File file = File.createTempFile("XmlUtilitiesTest", suffix);
		file.deleteOnExit();
		Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return file;
	}

	/**
	 * XML with an external entity pointing to a local file. Must never expose the file content.
	 */
	private static String createXxeXml(final File secretFile) {
		return "<?xml version=\"1.0\"?><!DOCTYPE root [<!ENTITY xxe SYSTEM \"" + secretFile.toURI() + "\">]><root>&xxe;</root>";
	}

	/** Classic "billion laughs" entity expansion attack. */
	private static final String BILLION_LAUGHS = "<?xml version=\"1.0\"?><!DOCTYPE lolz ["
			+ "<!ENTITY lol \"lol\">"
			+ "<!ENTITY lol1 \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\">"
			+ "<!ENTITY lol2 \"&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;&lol1;\">"
			+ "<!ENTITY lol3 \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">"
			+ "<!ENTITY lol4 \"&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;&lol3;\">"
			+ "<!ENTITY lol5 \"&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;&lol4;\">"
			+ "<!ENTITY lol6 \"&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;&lol5;\">"
			+ "<!ENTITY lol7 \"&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;&lol6;\">"
			+ "<!ENTITY lol8 \"&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;&lol7;\">"
			+ "<!ENTITY lol9 \"&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;&lol8;\">"
			+ "]><lolz>&lol9;</lolz>";

	@Test
	public void testRemoveFrameTag() {
		try {
			final String testString = "<abc def=\"123\">ghi</abc>";
			final String resultString = XmlUtilities.removeFrameTag(testString);
			assertEquals("ghi", resultString);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testRemoveFrameTagVariants() throws Exception {
		assertNull(XmlUtilities.removeFrameTag(null));
		assertEquals("", XmlUtilities.removeFrameTag(""));
		assertEquals("no xml", XmlUtilities.removeFrameTag("no xml"));
		// The first child element is returned as XML
		assertEquals("<b x=\"1\">1</b>", XmlUtilities.removeFrameTag("  <a><b x=\"1\">1</b><c>2</c></a>  "));
		assertNull(XmlUtilities.removeFrameTag("<a/>"));
		assertThrows(Exception.class, () -> XmlUtilities.removeFrameTag("<a><b></a>"));
	}

	@Test
	public void testIsXmlDocument() {
		assertTrue(XmlUtilities.isXmlDocument("<?xml version=\"1.0\"?><a/>".getBytes(StandardCharsets.UTF_8)));
		assertTrue(XmlUtilities.isXmlDocument("<?XML\tversion=\"1.0\"?><a/>".getBytes(StandardCharsets.UTF_8)));
		final byte[] withBom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '<', '?', 'x', 'm', 'l', ' ' };
		assertTrue(XmlUtilities.isXmlDocument(withBom));

		assertFalse(XmlUtilities.isXmlDocument(null));
		assertFalse(XmlUtilities.isXmlDocument(new byte[0]));
		assertFalse(XmlUtilities.isXmlDocument("<?xml".getBytes(StandardCharsets.UTF_8)));
		assertFalse(XmlUtilities.isXmlDocument("<?xml-stylesheet href=\"a\"?>".getBytes(StandardCharsets.UTF_8)));
		assertFalse(XmlUtilities.isXmlDocument("<root/>".getBytes(StandardCharsets.UTF_8)));
		assertFalse(XmlUtilities.isXmlDocument(" <?xml version=\"1.0\"?>".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testGetEmptyDocument() throws Exception {
		final Document document = XmlUtilities.getEmptyDocument();
		assertNotNull(document);
		assertEquals(0, document.getChildNodes().getLength());
	}

	@Test
	public void testParseXmlString() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root><!-- comment --><a>1</a><b><![CDATA[x<y]]></b></root>");
		final Node root = XmlUtilities.getRootNodeFromDocument(document);
		assertEquals("root", root.getNodeName());
		// Comments are ignored
		assertEquals(Arrays.asList("a", "b"), XmlUtilities.getNodenamesOfChilds(root));
		assertEquals(2, root.getChildNodes().getLength());
		// CDATA is converted to text
		assertEquals("x<y", XmlUtilities.getSimpleTextValueFromNode(XmlUtilities.getChildNodesMap(root).get("b")));

		final Exception exception = assertThrows(Exception.class, () -> XmlUtilities.parseXmlString("<root>"));
		assertEquals("Error while parsing xml", exception.getMessage());
		assertNotNull(exception.getCause());
	}

	@Test
	public void testParseXmlStringRejectsDoctype() throws Exception {
		final File secretFile = createTempFile(".txt", SECRET);
		assertThrows(Exception.class, () -> XmlUtilities.parseXmlString(createXxeXml(secretFile)));
		assertThrows(Exception.class, () -> XmlUtilities.parseXmlString(BILLION_LAUGHS));
		assertThrows(Exception.class, () -> XmlUtilities.parseXmlString("<!DOCTYPE root><root/>"));
	}

	@Test
	public void testParseXmlStringIgnoringDtdAllowsXhtmlDoctype() throws Exception {
		// The external DTD must not be downloaded, otherwise this would be slow or fail without network
		final String xhtml = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
				+ "<html><body><p>Text</p></body></html>";
		final Document document = XmlUtilities.parseXmlStringIngoringDtd(xhtml);
		assertEquals("html", document.getDocumentElement().getNodeName());
		assertEquals("Text", XmlUtilities.getSingleXPathNode(document, "/html/body/p").getTextContent());
	}

	@Test
	public void testParseXmlStringIgnoringDtdDoesNotResolveExternalEntities() throws Exception {
		final File secretFile = createTempFile(".txt", SECRET);
		Document document = null;
		try {
			document = XmlUtilities.parseXmlStringIngoringDtd(createXxeXml(secretFile));
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Rejecting the document is fine as well
		}
		if (document != null) {
			assertFalse(document.getDocumentElement().getTextContent().contains(SECRET));
		}
	}

	@Test
	public void testParseXmlStringIgnoringDtdInternalEntities() throws Exception {
		final Document document = XmlUtilities.parseXmlStringIngoringDtd("<!DOCTYPE root [<!ENTITY greeting \"Hello\">]><root>&greeting; World</root>");
		assertEquals("Hello World", document.getDocumentElement().getTextContent());
	}

	@Test
	public void testParseXmlStringIgnoringDtdLimitsEntityExpansion() {
		// Must fail on the JDK entity expansion limit instead of exhausting memory
		assertThrows(Exception.class, () -> XmlUtilities.parseXmlStringIngoringDtd(BILLION_LAUGHS));
	}

	@Test
	public void testParseXmlStringIgnoringDtdInvalidInput() {
		assertEquals("XML-Data is null", assertThrows(Exception.class, () -> XmlUtilities.parseXmlStringIngoringDtd(null)).getMessage());
		assertEquals("XML-Data is empty", assertThrows(Exception.class, () -> XmlUtilities.parseXmlStringIngoringDtd("  ")).getMessage());
		assertThrows(Exception.class, () -> XmlUtilities.parseXmlStringIngoringDtd("<a>"));
	}

	@Test
	public void testParseXmlFile() throws Exception {
		final File file = createTempFile(".xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>\u00E4</root>");
		final Document document = XmlUtilities.parseXmlFile(file);
		assertNotNull(document);
		assertEquals("\u00E4", document.getDocumentElement().getTextContent());

		assertNull(XmlUtilities.parseXmlFile(new File(file.getParentFile(), "does-not-exist-" + System.nanoTime() + ".xml")));
		assertNull(XmlUtilities.parseXmlFile(createTempFile(".xml", "<root>")));

		final File secretFile = createTempFile(".txt", SECRET);
		assertNull(XmlUtilities.parseXmlFile(createTempFile(".xml", createXxeXml(secretFile))));
	}

	@Test
	public void testParseXmlByteArray() throws Exception {
		final Document document = XmlUtilities.parseXmlFile("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><root>\u00E4</root>".getBytes(StandardCharsets.ISO_8859_1));
		assertNotNull(document);
		assertEquals("\u00E4", document.getDocumentElement().getTextContent());

		assertNull(XmlUtilities.parseXmlFile((byte[]) null));
		assertNull(XmlUtilities.parseXmlFile(new byte[0]));
		assertNull(XmlUtilities.parseXmlFile(BILLION_LAUGHS.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testGetSingleXPathNode() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root><item id=\"1\">a</item><item id=\"2\">b</item></root>");
		assertEquals("a", XmlUtilities.getSingleXPathNode(document, "/root/item").getTextContent());
		assertEquals("b", XmlUtilities.getSingleXPathNode(document, "//item[@id='2']").getTextContent());
		assertNull(XmlUtilities.getSingleXPathNode(document, "/root/missing"));
		assertNull(XmlUtilities.getSingleXPathNode(document, "///invalid["));
		assertNull(XmlUtilities.getSingleXPathNode(document, " "));
		assertNull(XmlUtilities.getSingleXPathNode(document, null));
		assertNull(XmlUtilities.getSingleXPathNode(null, "/root"));
	}

	@Test
	public void testGetSimpleTextValueFromNode() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root><a>text</a><b><c>1</c></b><d/></root>");
		final Map<String, Node> children = XmlUtilities.getChildNodesMap(document.getDocumentElement());
		assertEquals("text", XmlUtilities.getSimpleTextValueFromNode(children.get("a")));
		assertNull(XmlUtilities.getSimpleTextValueFromNode(children.get("b")));
		assertNull(XmlUtilities.getSimpleTextValueFromNode(children.get("d")));
		assertNull(XmlUtilities.getSimpleTextValueFromNode(null));
	}

	@Test
	public void testGetAttributeValue() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root Name=\"upper\" other=\"o\"><a ID=\"x\" id=\"y\"/></root>");
		final Node root = document.getDocumentElement();
		assertEquals("upper", XmlUtilities.getAttributeValue(root, "Name"));
		// Case insensitive fallback
		assertEquals("upper", XmlUtilities.getAttributeValue(root, "name"));
		assertNull(XmlUtilities.getAttributeValue(root, "missing"));

		// An exact match takes precedence over a case insensitive one
		final Node a = XmlUtilities.getChildNodes(root).get(0);
		assertEquals("y", XmlUtilities.getAttributeValue(a, "id"));
		assertEquals("x", XmlUtilities.getAttributeValue(a, "ID"));

		// Text nodes have no attributes
		assertNull(XmlUtilities.getAttributeValue(document.createTextNode("t"), "id"));
	}

	@Test
	public void testGetNodeValue() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root a=\"attr\"><b>text</b><c><d>deep</d></c><e/></root>");
		final Map<String, Node> children = XmlUtilities.getChildNodesMap(document.getDocumentElement());
		assertEquals("text", XmlUtilities.getNodeValue(children.get("b")));
		assertEquals("deep", XmlUtilities.getNodeValue(children.get("c")));
		assertNull(XmlUtilities.getNodeValue(children.get("e")));
		assertEquals("attr", XmlUtilities.getNodeValue(document.getDocumentElement().getAttributeNode("a")));
	}

	@Test
	public void testGetNodeAsStringAndRaw() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root><a>\u00E4</a></root>");
		assertEquals("<root><a>\u00E4</a></root>", XmlUtilities.getNodeAsString(document.getDocumentElement(), StandardCharsets.UTF_8, true));

		final String withHeader = XmlUtilities.getNodeAsString(document, null, false);
		assertTrue(withHeader.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\""), withHeader);
		assertTrue(withHeader.endsWith("<root><a>\u00E4</a></root>"), withHeader);

		final byte[] latin1 = XmlUtilities.getNodeAsRaw(document, StandardCharsets.ISO_8859_1, false);
		final String latin1Text = new String(latin1, StandardCharsets.ISO_8859_1);
		assertTrue(latin1Text.contains("encoding=\"ISO-8859-1\""), latin1Text);
		assertTrue(latin1Text.contains("<a>\u00E4</a>"), latin1Text);
		assertEquals(Charset.forName("ISO-8859-1"), XmlUtilities.getEncodingOfXmlByteArray(latin1));
	}

	@Test
	public void testConvertXmlToStringAndByteArray() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root><a>\u00E4</a></root>");
		final String text = XmlUtilities.convertXML2String(document, null);
		assertTrue(text.contains("encoding=\"UTF-8\""), text);
		assertTrue(text.endsWith("<root><a>\u00E4</a></root>"), text);

		final byte[] utf8 = XmlUtilities.convertXML2ByteArray(document, StandardCharsets.UTF_8);
		final byte[] latin1 = XmlUtilities.convertXML2ByteArray(document, StandardCharsets.ISO_8859_1);
		// The umlaut is written as C3 A4 in UTF-8 and as E4 in ISO-8859-1
		assertTrue(new String(utf8, StandardCharsets.ISO_8859_1).contains("<a>\u00C3\u00A4</a>"));
		assertTrue(new String(latin1, StandardCharsets.ISO_8859_1).contains("<a>\u00E4</a>"));
		assertEquals("\u00E4", XmlUtilities.parseXmlFile(utf8).getDocumentElement().getTextContent());
		assertEquals("\u00E4", XmlUtilities.parseXmlFile(latin1).getDocumentElement().getTextContent());
		assertEquals("\u00E4", XmlUtilities.parseXmlFile(XmlUtilities.convertXML2ByteArray(document, null)).getDocumentElement().getTextContent());
	}

	@Test
	public void testWriteDocumentToFile() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root><a>\u00E4</a></root>");
		final File file = createTempFile(".xml", "");
		XmlUtilities.writeDocumentToFile(document, StandardCharsets.UTF_8, file);
		assertEquals("\u00E4", XmlUtilities.parseXmlFile(file).getDocumentElement().getTextContent());

		final File notWritable = new File(file.getParentFile(), "missing-directory-" + System.nanoTime() + "/file.xml");
		assertThrows(Exception.class, () -> XmlUtilities.writeDocumentToFile(document, StandardCharsets.UTF_8, notWritable));
		assertFalse(notWritable.exists());
	}

	@Test
	public void testBuildDocument() throws Exception {
		final Document document = XmlUtilities.getEmptyDocument();
		final Element root = XmlUtilities.createRootTagNode(document, "root");
		XmlUtilities.appendTextValueNode(root, "int", 42);
		XmlUtilities.appendTextValueNode(root, "double", 1.5);
		XmlUtilities.appendTextValueNode(root, "text", "a<b");
		XmlUtilities.appendTextValueNode(root, "null", (String) null);
		final Element child = XmlUtilities.appendNode(root, "child");
		XmlUtilities.appendAttribute(child, "flag", true);
		XmlUtilities.appendAttribute(child, "number", 7);
		XmlUtilities.appendAttribute(child, "text", "x\"y");
		XmlUtilities.appendAttribute(child, "nothing", (String) null);
		XmlUtilities.appendAttribute(child, "removed", "r");
		XmlUtilities.removeAttribute(child, "removed");
		// Removing a missing attribute does nothing
		XmlUtilities.removeAttribute(child, "missing");

		final Map<String, String> values = XmlUtilities.getSimpleValuesOfNode(root);
		assertEquals("42", values.get("int"));
		assertEquals("1.5", values.get("double"));
		assertEquals("a<b", values.get("text"));
		assertEquals("<null>", values.get("null"));
		assertFalse(values.containsKey("child"));

		assertEquals("true", child.getAttribute("flag"));
		assertEquals("7", child.getAttribute("number"));
		assertEquals("x\"y", child.getAttribute("text"));
		assertEquals("<null>", child.getAttribute("nothing"));
		assertFalse(child.hasAttribute("removed"));

		// Survives a round trip with escaping
		final Document reparsed = XmlUtilities.parseXmlString(XmlUtilities.convertXML2String(document, StandardCharsets.UTF_8));
		assertEquals(values, XmlUtilities.getSimpleValuesOfNode(reparsed.getDocumentElement()));
	}

	@Test
	public void testAppendNodeToDocument() throws Exception {
		final Document document = XmlUtilities.getEmptyDocument();
		final Element root = XmlUtilities.appendNode(document, "root");
		assertEquals(root, document.getDocumentElement());
	}

	@Test
	public void testAddCommentToDocument() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root/>");
		XmlUtilities.addCommentToDocument(document, "comment");
		assertEquals(Node.COMMENT_NODE, document.getDocumentElement().getLastChild().getNodeType());
		assertEquals("comment", document.getDocumentElement().getLastChild().getNodeValue());

		final Document emptyDocument = XmlUtilities.getEmptyDocument();
		XmlUtilities.addCommentToDocument(emptyDocument, "only comment");
		assertEquals(Node.COMMENT_NODE, emptyDocument.getFirstChild().getNodeType());
	}

	@Test
	public void testChildNodeAccess() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root><a>1</a><b>2</b><a>3</a><c><d/></c></root>");
		final Node root = XmlUtilities.getRootNodeFromDocument(document);

		assertEquals(Arrays.asList("a", "b", "a", "c"), XmlUtilities.getNodenamesOfChilds(root));
		assertEquals(4, XmlUtilities.getChildNodes(root).size());
		assertEquals(4, XmlUtilities.getSubNodes(root).size());

		final Map<String, Node> childMap = XmlUtilities.getChildNodesMap(root);
		assertEquals(Arrays.asList("a", "b", "c"), Arrays.asList(childMap.keySet().toArray()));
		// The last child of the same name wins
		assertEquals("3", childMap.get("a").getTextContent());

		final Map<String, String> values = XmlUtilities.getSimpleValuesOfNode(root);
		assertEquals("3", values.get("a"));
		assertEquals("2", values.get("b"));
		assertFalse(values.containsKey("c"));
	}

	@Test
	public void testChildNodeAccessSkipsNonElements() throws Exception {
		// Whitespace text and comments are kept by parseXmlFile
		final Document document = XmlUtilities.parseXmlFile("<root>\n  <!-- c -->\n  <a>1</a>\n</root>".getBytes(StandardCharsets.UTF_8));
		final Node root = document.getDocumentElement();
		final List<Node> subNodes = XmlUtilities.getSubNodes(root);
		assertEquals(5, subNodes.size());
		assertEquals(1, XmlUtilities.getChildNodes(root).size());
		assertEquals(Arrays.asList("a"), XmlUtilities.getNodenamesOfChilds(root));
		assertEquals(1, XmlUtilities.getChildNodesMap(root).size());
	}

	@Test
	public void testGetEncodingOfXmlByteArray() throws Exception {
		assertEquals(StandardCharsets.UTF_8, XmlUtilities.getEncodingOfXmlByteArray(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, '<' }));
		assertEquals(StandardCharsets.UTF_16BE, XmlUtilities.getEncodingOfXmlByteArray(new byte[] { (byte) 0xFE, (byte) 0xFF, 0, '<' }));
		assertEquals(StandardCharsets.UTF_16LE, XmlUtilities.getEncodingOfXmlByteArray(new byte[] { (byte) 0xFF, (byte) 0xFE, '<', 0 }));

		assertEquals(StandardCharsets.ISO_8859_1, XmlUtilities.getEncodingOfXmlByteArray("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><a/>".getBytes(StandardCharsets.US_ASCII)));
		assertEquals(StandardCharsets.UTF_8, XmlUtilities.getEncodingOfXmlByteArray("<?xml version='1.0' encoding='utf-8'?><a/>".getBytes(StandardCharsets.US_ASCII)));
		assertEquals(Charset.forName("windows-1252"), XmlUtilities.getEncodingOfXmlByteArray("<?XML version=\"1.0\" ENCODING = \"Windows-1252\" ?><a/>".getBytes(StandardCharsets.US_ASCII)));

		// No declaration, no encoding attribute, no end of the declaration
		assertThrows(Exception.class, () -> XmlUtilities.getEncodingOfXmlByteArray(null));
		assertThrows(Exception.class, () -> XmlUtilities.getEncodingOfXmlByteArray(new byte[0]));
		assertThrows(Exception.class, () -> XmlUtilities.getEncodingOfXmlByteArray("<a/>".getBytes(StandardCharsets.US_ASCII)));
		assertThrows(Exception.class, () -> XmlUtilities.getEncodingOfXmlByteArray("<?xml version=\"1.0\"?><a/>".getBytes(StandardCharsets.US_ASCII)));
		assertThrows(Exception.class, () -> XmlUtilities.getEncodingOfXmlByteArray("<?xml version=\"1.0\" encoding=\"UTF-8\"".getBytes(StandardCharsets.US_ASCII)));
		// An encoding attribute in the content after the declaration is not used
		assertThrows(Exception.class, () -> XmlUtilities.getEncodingOfXmlByteArray("<?xml version=\"1.0\"?><a encoding=\"UTF-8\"/>".getBytes(StandardCharsets.US_ASCII)));

		final Exception unsupported = assertThrows(Exception.class, () -> XmlUtilities.getEncodingOfXmlByteArray("<?xml version=\"1.0\" encoding=\"NO-SUCH-ENCODING\"?>".getBytes(StandardCharsets.US_ASCII)));
		assertTrue(unsupported.getMessage().contains("NO-SUCH-ENCODING"), unsupported.getMessage());
	}

	@Test
	public void testParseXmlDataAndXsdVerify() throws Exception {
		final File xsdFile = createTempFile(".xsd", "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">"
				+ "<xs:element name=\"root\"><xs:complexType><xs:sequence>"
				+ "<xs:element name=\"count\" type=\"xs:int\"/>"
				+ "</xs:sequence></xs:complexType></xs:element></xs:schema>");
		final String xsdPath = xsdFile.getAbsolutePath();

		final Document valid = XmlUtilities.parseXMLDataAndXSDVerifyByDOM("<root><count>5</count></root>".getBytes(StandardCharsets.UTF_8), null, xsdPath);
		assertEquals("5", valid.getDocumentElement().getTextContent());

		final Exception invalidType = assertThrows(Exception.class, () -> XmlUtilities.parseXMLDataAndXSDVerifyByDOM("<root><count>five</count></root>".getBytes(StandardCharsets.UTF_8), null, xsdPath));
		assertTrue(invalidType.getMessage().contains("XML_SCHEMA_ERROR"), invalidType.getMessage());
		assertThrows(Exception.class, () -> XmlUtilities.parseXMLDataAndXSDVerifyByDOM("<root><other/></root>".getBytes(StandardCharsets.UTF_8), null, xsdPath));

		// Without schema only well-formedness is checked
		assertNotNull(XmlUtilities.parseXMLDataAndXSDVerifyByDOM("<anything/>".getBytes(StandardCharsets.UTF_8), null, null));
	}

	@Test
	public void testParseXmlDataAndXsdVerifyInputHandling() throws Exception {
		assertNull(XmlUtilities.parseXMLDataAndXSDVerifyByDOM(null, null, null));
		assertEquals("XML data is empty", assertThrows(Exception.class, () -> XmlUtilities.parseXMLDataAndXSDVerifyByDOM(new byte[0], null, null)).getMessage());

		// Trailing zero bytes are ignored
		final byte[] withZeros = Arrays.copyOf("<root>x</root>".getBytes(StandardCharsets.UTF_8), 20);
		assertEquals("x", XmlUtilities.parseXMLDataAndXSDVerifyByDOM(withZeros, null, null).getDocumentElement().getTextContent());

		// Explicit encoding for data without XML declaration
		final byte[] latin1 = "<root>\u00E4</root>".getBytes(StandardCharsets.ISO_8859_1);
		assertEquals("\u00E4", XmlUtilities.parseXMLDataAndXSDVerifyByDOM(latin1, "ISO-8859-1", null).getDocumentElement().getTextContent());

		final Exception malformed = assertThrows(Exception.class, () -> XmlUtilities.parseXMLDataAndXSDVerifyByDOM("<root>".getBytes(StandardCharsets.UTF_8), null, null));
		assertTrue(malformed.getMessage().contains("XML_PROCESSING"), malformed.getMessage());

		final File secretFile = createTempFile(".txt", SECRET);
		assertThrows(Exception.class, () -> XmlUtilities.parseXMLDataAndXSDVerifyByDOM(createXxeXml(secretFile).getBytes(StandardCharsets.UTF_8), null, null));
	}

	@Test
	public void testParseXmlDataAndXsdVerifyIgnoresSchemaLocationOfData() throws Exception {
		final File xsdFile = createTempFile(".xsd", "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"root\" type=\"xs:string\"/></xs:schema>");
		// The schema location named in the data must neither be loaded nor replace the given schema
		final String xml = "<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://invalid.invalid/schema.xsd\">text</root>";
		final Document document = XmlUtilities.parseXMLDataAndXSDVerifyByDOM(xml.getBytes(StandardCharsets.UTF_8), null, xsdFile.getAbsolutePath());
		assertEquals("text", document.getDocumentElement().getTextContent());
	}

	@Test
	public void testFormatXmlDocument() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root a=\"1\"><x>t</x><y/><z><w>1</w></z><?pi data?></root>");
		final String expectedBody = "<root a=\"1\">\n"
				+ "\t<x>t</x>\n"
				+ "\t<y />\n"
				+ "\t<z>\n"
				+ "\t\t<w>1</w>\n"
				+ "\t</z>\n"
				+ "\t<?pi data?>\n"
				+ "</root>\n";
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n" + expectedBody, XmlUtilities.formatXmlDocument(document, false));
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" standalone=\"no\"?>\n" + expectedBody, XmlUtilities.formatXmlDocument(document, StandardCharsets.ISO_8859_1, false));
	}

	@Test
	public void testFormatXmlDocumentEscapingRoundTrip() throws Exception {
		final Document document = XmlUtilities.getEmptyDocument();
		final Element root = XmlUtilities.createRootTagNode(document, "root");
		XmlUtilities.appendTextValueNode(root, "text", "a<b>&c ]]> \"quoted\"");
		XmlUtilities.appendTextValueNode(root, "cr", "line1\r\nline2");
		XmlUtilities.appendAttribute(root, "attr", "x\"<&>\n\r\ty");
		final Element cdataElement = XmlUtilities.appendNode(root, "cdata");
		cdataElement.appendChild(document.createCDATASection("raw <x> ]]> end"));

		final String formatted = XmlUtilities.formatXmlDocument(document, StandardCharsets.UTF_8, true);
		final Document reparsed = XmlUtilities.parseXmlFile(formatted.getBytes(StandardCharsets.UTF_8));
		assertNotNull(reparsed, formatted);
		final Element reparsedRoot = reparsed.getDocumentElement();
		final Map<String, Node> children = XmlUtilities.getChildNodesMap(reparsedRoot);
		assertEquals("a<b>&c ]]> \"quoted\"", children.get("text").getTextContent());
		assertEquals("line1\r\nline2", children.get("cr").getTextContent());
		assertEquals("x\"<&>\n\r\ty", reparsedRoot.getAttribute("attr"));
		assertEquals("raw <x> ]]> end", children.get("cdata").getTextContent());
	}

	@Test
	public void testFormatXmlDocumentManyAttributes() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root a=\"1\" b=\"2\" c=\"3\" d=\"4\" e=\"5\" f=\"6\"/>");
		final String formatted = XmlUtilities.formatXmlDocument(document, true);
		// More than 5 attributes are written one per line
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n<root\n\ta=\"1\"\n\tb=\"2\"\n\tc=\"3\"\n\td=\"4\"\n\te=\"5\"\n\tf=\"6\" />\n", formatted);
		assertEquals("6", XmlUtilities.parseXmlFile(formatted.getBytes(StandardCharsets.UTF_8)).getDocumentElement().getAttribute("f"));
	}

	@Test
	public void testFormatXmlDocumentKeepsDoctype() throws Exception {
		final Document document = XmlUtilities.parseXmlStringIngoringDtd("<!DOCTYPE root [<!ENTITY e \"v\">]><root>&e;</root>");
		final String formatted = XmlUtilities.formatXmlDocument(document, true);
		// The parser normalizes the internal subset (quotes, line breaks), its declarations must be kept
		assertTrue(formatted.contains("<!DOCTYPE root ["), formatted);
		assertTrue(formatted.contains("<!ENTITY e 'v'>"), formatted);
		assertEquals("v", XmlUtilities.parseXmlStringIngoringDtd(formatted).getDocumentElement().getTextContent());

		final Document xhtml = XmlUtilities.parseXmlStringIngoringDtd("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\"><html/>");
		assertTrue(XmlUtilities.formatXmlDocument(xhtml, true).contains("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"));

		final Document systemOnly = XmlUtilities.parseXmlStringIngoringDtd("<!DOCTYPE root SYSTEM \"root.dtd\"><root/>");
		assertTrue(XmlUtilities.formatXmlDocument(systemOnly, true).contains("<!DOCTYPE root SYSTEM \"root.dtd\">\n"));
	}

	@Test
	public void testFormatXmlNodeSpecialNodes() throws Exception {
		final Document document = XmlUtilities.getEmptyDocument();
		assertEquals("\t\t<!--c-->\n", XmlUtilities.formatXmlNode(document.createComment("c"), 2));
		assertEquals("", XmlUtilities.formatXmlNode(document.createComment("  "), 0));
		assertEquals("", XmlUtilities.formatXmlNode(document.createTextNode("  \n "), 0));
		assertEquals("a&amp;b", XmlUtilities.formatXmlNode(document.createTextNode("a&b"), 0));
		assertEquals("\t<?target?>\n", XmlUtilities.formatXmlNode(document.createProcessingInstruction("target", ""), 1));
		assertEquals("<![CDATA[]]>", XmlUtilities.formatXmlNode(document.createCDATASection(null), 0));
		assertEquals("&ent;", XmlUtilities.formatXmlNode(document.createEntityReference("ent"), 0));

		final Element whitespaceOnly = document.createElement("w");
		whitespaceOnly.appendChild(document.createTextNode("   "));
		assertEquals("<w />\n", XmlUtilities.formatXmlNode(whitespaceOnly, 0));

		// Mixed content: the text is kept, the child element still gets its own indented line
		final Element mixed = document.createElement("m");
		mixed.appendChild(document.createTextNode("text"));
		mixed.appendChild(document.createElement("e"));
		assertEquals("<m>\ntext\t<e />\n</m>\n", XmlUtilities.formatXmlNode(mixed, 0));
	}

	@Test
	public void testFormatXmlDocumentErrorHandling() {
		assertNull(XmlUtilities.formatXmlDocument(null, false));
		assertNull(XmlUtilities.formatXmlDocument(null, StandardCharsets.UTF_8, false));
		assertThrows(NullPointerException.class, () -> XmlUtilities.formatXmlDocument(null, true));
		assertThrows(NullPointerException.class, () -> XmlUtilities.formatXmlDocument(null, StandardCharsets.UTF_8, true));
	}

	@Test
	public void testParsedAndFormattedDocumentsAreEquivalent() throws Exception {
		final String xml = "<root><a x=\"1\">\u00E4\u20AC</a><b><c>2</c><c>3</c></b><d/></root>";
		final Document document = XmlUtilities.parseXmlString(xml);
		final String formatted = XmlUtilities.formatXmlDocument(document, true);
		final Document reparsed = XmlUtilities.parseXmlString(formatted);
		final Node reparsedRoot = reparsed.getDocumentElement();

		// Same structure and values, only the indentation whitespace was added
		assertEquals(XmlUtilities.getNodenamesOfChilds(document.getDocumentElement()), XmlUtilities.getNodenamesOfChilds(reparsedRoot));
		final Map<String, Node> children = XmlUtilities.getChildNodesMap(reparsedRoot);
		assertEquals("\u00E4\u20AC", XmlUtilities.getSimpleTextValueFromNode(children.get("a")));
		assertEquals("1", XmlUtilities.getAttributeValue(children.get("a"), "x"));
		assertEquals(Arrays.asList("c", "c"), XmlUtilities.getNodenamesOfChilds(children.get("b")));
		assertEquals("3", XmlUtilities.getChildNodes(children.get("b")).get(1).getTextContent());
		assertTrue(XmlUtilities.getChildNodes(children.get("d")).isEmpty());

		// Formatting the reparsed document again gives the same result
		assertEquals(formatted, XmlUtilities.formatXmlDocument(reparsed, true));
	}
}
