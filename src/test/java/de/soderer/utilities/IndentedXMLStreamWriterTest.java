package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import de.soderer.utilities.xml.IndentedXMLStreamWriter;
import de.soderer.utilities.xml.XmlUtilities;

@SuppressWarnings("static-method")
public class IndentedXMLStreamWriterTest {
	/**
	 * Output stream that remembers whether it was closed.
	 */
	private static class CloseTrackingOutputStream extends ByteArrayOutputStream {
		boolean closed = false;

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}

	@Test
	public void testIndentedOutput() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.writeStartDocument();
			writer.writeStartElement("root");
			writer.writeAttribute("id", "1");
			writer.writeStartElement("name");
			writer.writeCharacters("value");
			writer.writeEndElement();
			writer.writeEmptyElement("empty");
			writer.writeAttribute("x", "y");
			writer.writeStartElement("list");
			writer.writeStartElement("item");
			writer.writeCharacters("1");
			writer.writeEndElement();
			writer.writeComment(" comment ");
			writer.writeEndElement();
			writer.writeStartElement("data");
			writer.writeCData("raw <data>");
			writer.writeEndElement();
			writer.writeEndElement();
			writer.writeEndDocument();
		}

		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<root id=\"1\">\n"
				+ "\t<name>value</name>\n"
				+ "\t<empty x=\"y\"/>\n"
				+ "\t<list>\n"
				+ "\t\t<item>1</item>\n"
				+ "\t\t<!-- comment -->\n"
				+ "\t</list>\n"
				+ "\t<data><![CDATA[raw <data>]]></data>\n"
				+ "</root>\n", outputStream.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testEscaping() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.writeStartDocument();
			writer.writeStartElement("root");
			writer.writeAttribute("attr", "\"<&>");
			writer.writeCharacters("a<b&c>d");
			writer.writeEndElement();
			writer.writeEndDocument();
		}
		final String xml = outputStream.toString(StandardCharsets.UTF_8);
		assertTrue(xml.contains("a&lt;b&amp;c"), xml);

		final Document document = XmlUtilities.parseXmlString(xml);
		assertEquals("a<b&c>d", document.getDocumentElement().getTextContent());
		assertEquals("\"<&>", document.getDocumentElement().getAttribute("attr"));
	}

	@Test
	public void testCustomIndentation() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream, StandardCharsets.UTF_8, ' ', 2)) {
			writer.writeStartElement("a");
			writer.writeStartElement("b");
			writer.writeEmptyElement("c");
			writer.writeEndElement();
			writer.writeEndElement();
			writer.writeEndDocument();
		}
		assertEquals("<a>\n  <b>\n    <c/>\n  </b>\n</a>\n", outputStream.toString(StandardCharsets.UTF_8));

		final ByteArrayOutputStream outputStream2 = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream2, StandardCharsets.UTF_8, "--")) {
			writer.writeStartElement("a");
			writer.writeEmptyElement("b");
			writer.writeEndElement();
			writer.writeEndDocument();
		}
		assertEquals("<a>\n--<b/>\n</a>\n", outputStream2.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testXmlDeclarationVariants() throws Exception {
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", writeDeclaration(writer -> writer.writeStartDocument()));
		assertEquals("<?xml version=\"1.1\" encoding=\"UTF-8\"?>\n", writeDeclaration(writer -> writer.writeStartDocument("1.1")));
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n", writeDeclaration(writer -> writer.writeStartDocument("ISO-8859-1", "1.0")));
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", writeDeclaration(writer -> writer.writeStartDocument(null, null)));
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n", writeDeclaration(writer -> writer.writeStartDocument(Boolean.TRUE, null, null)));
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n", writeDeclaration(writer -> writer.writeStartDocument(Boolean.FALSE, null, null)));
	}

	private interface WriterAction {
		void run(IndentedXMLStreamWriter writer) throws Exception;
	}

	private static String writeDeclaration(final WriterAction action) throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			action.run(writer);
			writer.flush();
		}
		return outputStream.toString(StandardCharsets.UTF_8);
	}

	@Test
	public void testNonUtf8Encoding() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream, StandardCharsets.ISO_8859_1, "\t")) {
			writer.writeStartDocument();
			writer.writeStartElement("root");
			writer.writeCharacters("\u00E4\u00F6\u00FC");
			writer.writeEndElement();
			writer.writeEndDocument();
		}
		final byte[] data = outputStream.toByteArray();
		assertEquals("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n<root>\u00E4\u00F6\u00FC</root>\n", new String(data, StandardCharsets.ISO_8859_1));
		assertEquals(StandardCharsets.ISO_8859_1, XmlUtilities.getEncodingOfXmlByteArray(data));
		assertEquals("\u00E4\u00F6\u00FC", XmlUtilities.parseXmlFile(data).getDocumentElement().getTextContent());
	}

	@Test
	public void testWriteDocument() throws Exception {
		final Document document = XmlUtilities.parseXmlStringIngoringDtd("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root a=\"1\"><x>t</x><y/><z><w>1</w></z><?pi data?></root>");
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream, StandardCharsets.UTF_8, ' ', 2)) {
			writer.write(document);
		}
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
				+ "<root a=\"1\">\n"
				+ "  <x>t</x>\n"
				+ "  <y/>\n"
				+ "  <z>\n"
				+ "    <w>1</w>\n"
				+ "  </z>\n"
				+ "  <?pi data?>\n"
				+ "</root>\n", outputStream.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testWriteDocumentRoundTrip() throws Exception {
		final Document document = XmlUtilities.getEmptyDocument();
		final Element root = XmlUtilities.createRootTagNode(document, "root");
		XmlUtilities.appendAttribute(root, "attr", "a\"b<c&d");
		XmlUtilities.appendTextValueNode(root, "text", "x<y&z \u20AC");
		final Element cdata = XmlUtilities.appendNode(root, "cdata");
		cdata.appendChild(document.createCDATASection("raw <data>"));
		root.appendChild(document.createComment("comment"));
		final Element empty = XmlUtilities.appendNode(root, "empty");
		XmlUtilities.appendAttribute(empty, "e", "1");

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.write(document);
		}

		final Document reparsed = XmlUtilities.parseXmlFile(outputStream.toByteArray());
		assertNotNull(reparsed, outputStream.toString(StandardCharsets.UTF_8));
		final Element reparsedRoot = reparsed.getDocumentElement();
		assertEquals("a\"b<c&d", reparsedRoot.getAttribute("attr"));
		final Map<String, Node> children = XmlUtilities.getChildNodesMap(reparsedRoot);
		assertEquals("x<y&z \u20AC", children.get("text").getTextContent());
		assertEquals("raw <data>", children.get("cdata").getTextContent());
		assertEquals("1", ((Element) children.get("empty")).getAttribute("e"));
	}

	@Test
	public void testWriteDocumentWithDoctype() throws Exception {
		final Document document = XmlUtilities.parseXmlStringIngoringDtd("<!DOCTYPE root [<!ENTITY e \"v\">]><root>&e;</root>");
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.write(document);
		}
		final String xml = outputStream.toString(StandardCharsets.UTF_8);
		// The parser normalizes the internal subset (quotes, line breaks), its declarations must be kept
		assertTrue(xml.contains("<!DOCTYPE root ["), xml);
		assertTrue(xml.contains("<!ENTITY e 'v'>"), xml);
		assertEquals("v", XmlUtilities.parseXmlStringIngoringDtd(xml).getDocumentElement().getTextContent());
	}

	@Test
	public void testFormattingWhitespaceIsDropped() throws Exception {
		// Whitespace text nodes starting with a line break are formatting and are replaced by the writer's own
		final Document document = XmlUtilities.parseXmlFile("<root>\n    <a>1</a>\n    <b>2</b>\n</root>".getBytes(StandardCharsets.UTF_8));
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.write(document);
		}
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n\t<a>1</a>\n\t<b>2</b>\n</root>\n", outputStream.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testSignificantWhitespaceIsKept() throws Exception {
		final Document document = XmlUtilities.parseXmlFile("<root><a> </a><b>x\n</b></root>".getBytes(StandardCharsets.UTF_8));
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.write(document);
		}
		final Document reparsed = XmlUtilities.parseXmlFile(outputStream.toByteArray());
		final Map<String, Node> children = XmlUtilities.getChildNodesMap(reparsed.getDocumentElement());
		assertEquals(" ", children.get("a").getTextContent());
		assertEquals("x\n", children.get("b").getTextContent());
	}

	@Test
	public void testUnsupportedNodeType() throws Exception {
		final Document document = XmlUtilities.parseXmlString("<root a=\"1\"/>");
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(new ByteArrayOutputStream())) {
			final Exception exception = assertThrows(Exception.class, () -> writer.write(document.getDocumentElement().getAttributeNode("a")));
			assertTrue(exception.getMessage().startsWith("Unexpected xml node type"), exception.getMessage());
		}
	}

	@Test
	public void testProcessingInstructionAndEntityReference() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.writeStartElement("root");
			writer.writeProcessingInstruction("target");
			writer.writeProcessingInstruction("target", "data");
			writer.writeStartElement("e");
			writer.writeEntityRef("amp");
			writer.writeEndElement();
			writer.writeEndElement();
			writer.writeEndDocument();
		}
		assertEquals("<root>\n\t<?target?>\n\t<?target data?>\n\t<e>&amp;</e>\n</root>\n", outputStream.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testNamespaces() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.writeStartElement("ns", "root", "urn:test");
			writer.writeNamespace("ns", "urn:test");
			writer.writeEmptyElement("ns", "child", "urn:test");
			writer.writeAttribute("ns", "urn:test", "attr", "v");
			writer.writeEndElement();
			writer.writeEndDocument();
		}
		final String xml = outputStream.toString(StandardCharsets.UTF_8);
		assertEquals("<ns:root xmlns:ns=\"urn:test\">\n\t<ns:child ns:attr=\"v\"/>\n</ns:root>\n", xml);
		assertNotNull(XmlUtilities.parseXmlFile(xml.getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testCloseDoesNotCloseStream() throws Exception {
		final CloseTrackingOutputStream outputStream = new CloseTrackingOutputStream();
		try (final IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.writeStartElement("root");
			writer.writeEndElement();
			writer.writeEndDocument();
		}
		assertFalse(outputStream.closed);
		assertEquals("<root></root>\n", outputStream.toString(StandardCharsets.UTF_8));
	}

	@Test
	public void testEndDocumentWithoutTrailingNewlineDuplication() throws Exception {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try (IndentedXMLStreamWriter writer = new IndentedXMLStreamWriter(outputStream)) {
			writer.writeStartDocument();
			writer.writeEndDocument();
		}
		assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", outputStream.toString(StandardCharsets.UTF_8));
	}
}
