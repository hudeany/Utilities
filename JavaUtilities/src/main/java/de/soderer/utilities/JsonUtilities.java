package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class JsonUtilities {
	public static JSONObject parseJsonData(String jsonData, boolean throwExceptionOnError) throws Exception {
		JSONObject jsonObject = null;
		try {
			jsonObject = (JSONObject) JSONSerializer.toJSON(jsonData);
			return jsonObject;
		} catch (Exception e) {
			if (throwExceptionOnError) {
				throw new Exception("Invalid JSON data", e);
			} else {
				return null;
			}
		}
	}
	
	public static String formatJsonObject(JSONObject jsonObject, boolean throwExceptionOnError) throws Exception {
		try {
			return TextUtilities.replaceIndentingBlanks(jsonObject.toString(2), 2, "\t");
		} catch (Exception e) {
			if (throwExceptionOnError) {
				throw new Exception("Error formatting JSON data", e);
			} else {
				return null;
			}
		}
	}
	

	public static JSONObject convertXmlDocument(Document xmlDocument, boolean throwExceptionOnError) throws Exception {
		try {
			JSONObject jsonObject = new JSONObject();
			jsonObject.accumulate(xmlDocument.getChildNodes().item(0).getNodeName(), convertXmlNode(xmlDocument.getChildNodes().item(0)));
			return jsonObject;
		} catch (Exception e) {
			if (throwExceptionOnError) {
				throw new Exception("Invalid data", e);
			} else {
				return null;
			}
		}
	}
	
	public static JSONObject convertXmlNode(Node xmlNode) {
		JSONObject jsonObject = new JSONObject();
		if (xmlNode.getAttributes() != null && xmlNode.getAttributes().getLength() > 0) {
			for (int attributeIndex = 0; attributeIndex < xmlNode.getAttributes().getLength(); attributeIndex++) {
				Node attributeNode = xmlNode.getAttributes().item(attributeIndex);
				jsonObject.accumulate(attributeNode.getNodeName(), attributeNode.getNodeValue());
			}
		}
		if (xmlNode.getChildNodes() != null && xmlNode.getChildNodes().getLength() > 0) {
			for (int i = 0; i < xmlNode.getChildNodes().getLength(); i++) {
				if (xmlNode.getChildNodes().item(i).getNodeType() == Node.TEXT_NODE) {
					if (StringUtils.isNotBlank(xmlNode.getChildNodes().item(i).getNodeValue())) {
						jsonObject.accumulate("text", xmlNode.getChildNodes().item(i).getNodeValue());
					}
				} else if (xmlNode.getChildNodes().item(i).getNodeType() == Node.COMMENT_NODE) {
					// do nothing
				} else {
					Node xmlSubNode = xmlNode.getChildNodes().item(i);
					JSONObject nodeJsonObject = convertXmlNode(xmlSubNode);
					if (nodeJsonObject != null) {
						jsonObject.accumulate(xmlSubNode.getNodeName(), nodeJsonObject);
					}
				}
			}
		}
		return jsonObject;
	}
	
	public static Document convertToXmlDocument(JSONObject jsonObject, boolean throwExceptionOnError) throws Exception {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document xmlDocument = documentBuilder.newDocument();
			xmlDocument.setXmlStandalone(true);
			List<Node> mainNodes = convertToXmlNodes(jsonObject, xmlDocument);
			if (mainNodes == null || mainNodes.size() < 1) {
				throw new Exception("No data found");
			} else if (mainNodes.size() == 1) {
				xmlDocument.appendChild(mainNodes.get(0));
			} else {
				Node rootNode = xmlDocument.createElement("root");
				for (Node subNode : mainNodes) {
					rootNode.appendChild(subNode);
				}
				xmlDocument.appendChild(rootNode);
			}
			return xmlDocument;
		} catch (Exception e) {
			if (throwExceptionOnError) {
				throw new Exception("Invalid data", e);
			} else {
				return null;
			}
		}
	}
	
	public static List<Node> convertToXmlNodes(JSONObject jsonObject, Document xmlDocument) {
		List<Node> list = new ArrayList<Node>();
		
		for (Object keyObject : jsonObject.keySet()) {
			String key = (String) keyObject;
			Object subItem = jsonObject.get(key);
			if (subItem instanceof JSONObject) {
				Node newNode = xmlDocument.createElement(key);
				list.add(newNode);
				for (Node subNode : convertToXmlNodes((JSONObject) subItem, xmlDocument)) {
					newNode.appendChild(subNode);
				}
			} else if (subItem instanceof JSONArray) {
				for (Node subNode : convertToXmlNodes((JSONArray) subItem, key, xmlDocument)) {
					list.add(subNode);
				}
			} else {
				Node newNode = xmlDocument.createElement(key);
				list.add(newNode);
				newNode.setTextContent(subItem.toString());
			}
		}
		
		return list;
	}
	
	public static List<Node> convertToXmlNodes(JSONArray jsonArray, String nodeName, Document xmlDocument) {
		List<Node> list = new ArrayList<Node>();
		
		if (jsonArray.size() > 0) {
			for (Object subItem : jsonArray) {
				if (subItem instanceof JSONObject) {
					Node newNode = xmlDocument.createElement(nodeName);
					list.add(newNode);
					for (Node subNode : convertToXmlNodes((JSONObject) subItem, xmlDocument)) {
						newNode.appendChild(subNode);
					}
				} else if (subItem instanceof JSONArray) {
					Node newNode = xmlDocument.createElement(nodeName);
					list.add(newNode);
					for (Node subNode : convertToXmlNodes((JSONArray) subItem, nodeName, xmlDocument)) {
						newNode.appendChild(subNode);
					}
				} else {
					Node newNode = xmlDocument.createElement(nodeName);
					list.add(newNode);
					newNode.setTextContent(subItem.toString());
				}
			}
		} else {
			Node newNode = xmlDocument.createElement(nodeName);
			list.add(newNode);
		}
		
		return list;
	}
}
