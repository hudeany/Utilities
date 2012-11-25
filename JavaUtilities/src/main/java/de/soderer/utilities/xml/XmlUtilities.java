package de.soderer.utilities.xml;

import java.io.BufferedInputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XmlUtilities {
	public static Document downloadAndParseXmlFile(String url) {
		BufferedInputStream bufferedInputStream = null;
		try {
			bufferedInputStream = new BufferedInputStream(new URL(url).openStream());
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.parse(new InputSource(bufferedInputStream));
			return document;
		} catch (Exception e) {
			return null;
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
}
