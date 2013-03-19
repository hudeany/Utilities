package de.soderer.utilities.xml;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.soderer.utilities.TextUtilities;

public class XmlUtilities {
	public static Document parseXmlData(byte[] xmlData, boolean throwExceptionOnError) throws ParserConfigurationException, SAXException, IOException {
		ByteArrayInputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(xmlData);
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(new InputSource(inputStream));
			return document;
		} catch (ParserConfigurationException e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		} catch (SAXException e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		} catch (IOException e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	public static Document parseXmlDataWithoutDtdValidation(byte[] xmlData, boolean throwExceptionOnError) throws ParserConfigurationException, SAXException, IOException {
		ByteArrayInputStream inputStream = null;
		try {
			inputStream = new ByteArrayInputStream(xmlData);
	        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
	        documentBuilderFactory.setValidating(false);
	        documentBuilderFactory.setIgnoringComments(true);
	        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
	        documentBuilderFactory.setCoalescing(true);
	        documentBuilderFactory.setExpandEntityReferences(true);
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			documentBuilder.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                    return new InputSource(new StringReader(""));
                }
            });
			Document document = documentBuilder.parse(new InputSource(inputStream));
			return document;
		} catch (ParserConfigurationException e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		} catch (SAXException e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		} catch (IOException e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
	public static Document downloadAndParseXmlFile(String url) throws IOException {
		BufferedInputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new URL(url).openStream());
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(new InputSource(inputStream));
			return document;
		} catch (Exception e) {
			return null;
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
	
	public static Node getSingleXPathNode(Document document, String xpathExpression) {
		if (document == null) {
			return null;
		} else if (StringUtils.isBlank(xpathExpression)) {
			return null;
		} else {
			try {
				NodeList nodeList = (NodeList) XPathFactory.newInstance().newXPath().evaluate(xpathExpression, document, XPathConstants.NODESET);
				if (nodeList != null && nodeList.getLength() > 0) {
					return nodeList.item(0);
				} else {
					return null;
				}
			} catch (XPathExpressionException e) {
				return null;
			}
		}
	}
	
	public static String formatXmlDocument(Document xmlDocument, String encoding, boolean throwExceptionOnError) throws Exception {
		try {
			StringBuilder result = new StringBuilder();
			result.append("<?xml version=\"");
			result.append(xmlDocument.getXmlVersion());
			result.append("\" encoding=\"");
			result.append(encoding.toUpperCase());
			result.append("\" standalone=\"");
			result.append(xmlDocument.getXmlStandalone() ? "yes" : "no");
			result.append("\"?>\n");
			
			for (int i = 0; i < xmlDocument.getChildNodes().getLength(); i++) {
				Node xmlNode = xmlDocument.getChildNodes().item(i);
				result.append(formatXmlNode(xmlNode, 0));
			}
			return result.toString();
		} catch (Exception e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		}
	}
	
	public static String formatXmlDocument(Document xmlDocument, boolean throwExceptionOnError) throws Exception {
		try {
			StringBuilder result = new StringBuilder();
			result.append("<?xml version=\"");
			result.append(xmlDocument.getXmlVersion());
			result.append("\" encoding=\"");
			result.append(xmlDocument.getXmlEncoding() == null ? "UTF-8" : xmlDocument.getXmlEncoding());
			result.append("\" standalone=\"");
			result.append(xmlDocument.getXmlStandalone() ? "yes" : "no");
			result.append("\"?>\n");
			
			for (int i = 0; i < xmlDocument.getChildNodes().getLength(); i++) {
				Node xmlNode = xmlDocument.getChildNodes().item(i);
				result.append(formatXmlNode(xmlNode, 0));
			}
			return result.toString();
		} catch (Exception e) {
			if (throwExceptionOnError) {
				throw e;
			} else {
				return null;
			}
		}
	}
	
	public static String formatXmlNode(Node xmlNode, int indentationLevel) {
		if (xmlNode.getNodeType() == Node.TEXT_NODE) {
			if (StringUtils.isBlank(xmlNode.getNodeValue())) {
				return "";
			} else {
				return xmlNode.getNodeValue().replace("&", "&amp;").replace("<", "&lt;");
			}
		} else if (xmlNode.getNodeType() == Node.COMMENT_NODE) {
			if (StringUtils.isBlank(xmlNode.getNodeValue())) {
				return "";
			} else {
				StringBuilder result = new StringBuilder();
				result.append(TextUtilities.repeatString("\t", indentationLevel));
				result.append("<!--");
				result.append(xmlNode.getNodeValue().replace("&", "&amp;").replace("<", "&lt;"));
				result.append("-->\n");
				return result.toString();
			}
		}  else if (xmlNode.getNodeType() == Node.DOCUMENT_TYPE_NODE) {
			StringBuilder result = new StringBuilder();
			result.append(TextUtilities.repeatString("\t", indentationLevel));
			result.append("<!DOCTYPE ");
			result.append(((DocumentType) xmlNode).getName());
			result.append(" PUBLIC \"");
			result.append(((DocumentType) xmlNode).getPublicId());
			result.append("\" \"");
			result.append(((DocumentType) xmlNode).getSystemId());
			result.append("\">\n");
			return result.toString();
		} else if (xmlNode.getChildNodes().getLength() == 1 && xmlNode.getChildNodes().item(0).getNodeType() == Node.TEXT_NODE) {
			if (StringUtils.isBlank(xmlNode.getChildNodes().item(0).getNodeValue())) {
				StringBuilder result = new StringBuilder();
				result.append(TextUtilities.repeatString("\t", indentationLevel));
				result.append("<");
				result.append(xmlNode.getNodeName());
				result.append(formatAttributes(xmlNode, indentationLevel));
				result.append(" />\n");
				return result.toString();
			} else {
				StringBuilder result = new StringBuilder();
				result.append(TextUtilities.repeatString("\t", indentationLevel));
				result.append("<");
				result.append(xmlNode.getNodeName());
				result.append(formatAttributes(xmlNode, indentationLevel));
				result.append(">");
				result.append(xmlNode.getChildNodes().item(0).getNodeValue().replace("&", "&amp;").replace("<", "&lt;"));
				result.append("</");
				result.append(xmlNode.getNodeName());
				result.append(">\n");
				return result.toString();
			}
		} else {
			StringBuilder innerXml = new StringBuilder();
			if (xmlNode.getChildNodes() != null && xmlNode.getChildNodes().getLength() > 0) {
				for (int nodeIndex = 0; nodeIndex < xmlNode.getChildNodes().getLength(); nodeIndex++) {
					Node xmlSubNode = xmlNode.getChildNodes().item(nodeIndex);
					if (xmlSubNode.getNodeType() == Node.TEXT_NODE) {
						if (StringUtils.isNotBlank(xmlSubNode.getNodeValue())) {
							innerXml.append(xmlSubNode.getNodeValue().replace("&", "&amp;").replace("<", "&lt;"));
						}
					} else {
						innerXml.append(formatXmlNode(xmlSubNode, indentationLevel + 1));
					}
				}
			}

			StringBuilder result = new StringBuilder();
			result.append(TextUtilities.repeatString("\t", indentationLevel));
			result.append("<");
			result.append(xmlNode.getNodeName());
			result.append(formatAttributes(xmlNode, indentationLevel));
			if (innerXml.length() == 0) {
				result.append(" />\n");
			} else {
				result.append(">\n");
				result.append(innerXml);
				result.append(TextUtilities.repeatString("\t", indentationLevel));
				result.append("</");
				result.append(xmlNode.getNodeName());
				result.append(">\n");
			}
			return result.toString();
		}
	}

	private static String formatAttributes(Node xmlNode, int indentationLevel) {
		StringBuilder result = new StringBuilder();
		if (xmlNode.getAttributes() != null && xmlNode.getAttributes().getLength() > 0) {
			if (xmlNode.getAttributes().getLength() <= 5) {
				for (int attributeIndex = 0; attributeIndex < xmlNode.getAttributes().getLength(); attributeIndex++) {
					Node attributeNode = xmlNode.getAttributes().item(attributeIndex);
					result.append(" ");
					result.append(attributeNode.getNodeName());
					result.append("=\"");
					result.append(attributeNode.getNodeValue().replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll("&", "&amp;"));
					result.append("\"");
				}
			} else {
				result.append("\n");
				for (int attributeIndex = 0; attributeIndex < xmlNode.getAttributes().getLength(); attributeIndex++) {
					if (attributeIndex > 0) {
						result.append("\n");
					}
					Node attributeNode = xmlNode.getAttributes().item(attributeIndex);
					result.append(TextUtilities.repeatString("\t", indentationLevel + 1));
					result.append(attributeNode.getNodeName());
					result.append("=\"");
					result.append(attributeNode.getNodeValue().replaceAll("\"", "&quot;").replaceAll("<", "&lt;").replaceAll("&", "&amp;"));
					result.append("\"");
				}
			}
		}
		return result.toString();
	}
	
	public static String getNodeTypeAsString(short nodeType) {
		switch(nodeType) {
			case Node.ELEMENT_NODE:
				return "ELEMENT_NODE";
			case Node.ATTRIBUTE_NODE:
				return "ATTRIBUTE_NODE";
			case Node.TEXT_NODE:
				return "TEXT_NODE";
			case Node.CDATA_SECTION_NODE:
				return "CDATA_SECTION_NODE";
			case Node.ENTITY_REFERENCE_NODE:
				return "ENTITY_REFERENCE_NODE";
			case Node.ENTITY_NODE:
				return "ENTITY_NODE";
			case Node.PROCESSING_INSTRUCTION_NODE:
				return "PROCESSING_INSTRUCTION_NODE";
			case Node.COMMENT_NODE:
				return "COMMENT_NODE";
			case Node.DOCUMENT_NODE:
				return "DOCUMENT_NODE";
			case Node.DOCUMENT_TYPE_NODE:
				return "DOCUMENT_TYPE_NODE";
			case Node.DOCUMENT_FRAGMENT_NODE:
				return "DOCUMENT_FRAGMENT_NODE";
			case Node.NOTATION_NODE:
				return "NOTATION_NODE";
			default:
				return null;
		}
	}
}
