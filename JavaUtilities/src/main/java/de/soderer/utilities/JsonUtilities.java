package de.soderer.utilities;

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
}
