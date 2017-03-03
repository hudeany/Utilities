package de.soderer.utilities.json.schema;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.json.Json5Reader;
import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;

public class JsonSchemaDependencyResolver {
	private JsonObject schemaDocumentNode = null;
	private Map<String, JsonObject> additionalSchemaDocumentNodes = new HashMap<String, JsonObject>();

	private String latestJsonPath = null;
	private Set<String> latestDependencies;
	
	public JsonSchemaDependencyResolver(JsonObject schemaDocumentNode) throws JsonSchemaDefinitionError {
		if (schemaDocumentNode == null) {
			throw new JsonSchemaDefinitionError("Invalid data type 'null' for JsonSchemaDependencyResolver", "$");
		}
		this.schemaDocumentNode = schemaDocumentNode;
	}

	public Object getDependencyByReference(String reference) throws Exception {
		if (reference != null) {
			if (!reference.contains("#")) {
				// Dereference simple reference without '#'
				if (schemaDocumentNode.get("definitions") != null && schemaDocumentNode.get("definitions") instanceof JsonObject && ((JsonObject) schemaDocumentNode.get("definitions")).containsPropertyKey(reference)) {
					return ((JsonObject) schemaDocumentNode.get("definitions")).get(reference);
				} else {
					for (JsonObject indirectJsonDefinitions : additionalSchemaDocumentNodes.values()) {
						if (indirectJsonDefinitions.get("definitions") != null && indirectJsonDefinitions.get("definitions") instanceof JsonObject && ((JsonObject) indirectJsonDefinitions.get("definitions")).containsPropertyKey(reference)) {
							return ((JsonObject) indirectJsonDefinitions.get("definitions")).get(reference);
						}
					}
					throw new Exception("Invalid JSON schema reference key '" + reference + "' or reference key not found. Use simple reference keys or this pattern for reference keys: '<referenced packagename or empty>#/definitions/<your reference key>'");
				}
			} else if (reference.startsWith("#")) {
				// Dereference local document reference
				List<String> referencePathParts = Utilities.splitAndTrimList(reference.substring(1), '/');
				referencePathParts.removeAll(Collections.singleton(""));
				JsonObject referencedObject = schemaDocumentNode;
				for (String referencePathPart : referencePathParts) {
					referencePathPart = referencePathPart.replace("~0", "~").replace("~1", "/").replace("%25", "%");
					if (!referencedObject.containsPropertyKey(referencePathPart)) {
						throw new JsonSchemaDefinitionError("JsonSchema does not contain the reference path '" + reference + "'", "$");
					} else if (referencedObject.get(referencePathPart) == null) {
						throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", "$");
					} else if (!(referencedObject.get(referencePathPart) instanceof JsonObject)) {
						throw new JsonSchemaDefinitionError("Invalid data type '" + schemaDocumentNode.get("definitions").getClass().getSimpleName() + "' for reference path '" + reference + "'", "$");
					} else {
						referencedObject = (JsonObject) referencedObject.get(referencePathPart);
					}
				}
				return referencedObject;
			} else {
				// Dereference other document reference
				String packageName = reference.substring(0, reference.lastIndexOf("#"));
				if (!additionalSchemaDocumentNodes.containsKey(packageName)) {
					throw new Exception("Unknown JSON schema reference package name '" + packageName + "'");
				} else if (additionalSchemaDocumentNodes.get(packageName) == null) {
					throw new Exception("Invalid empty JSON schema reference for package name '" + packageName + "'");
				} else {
					List<String> referencePathParts = Utilities.splitAndTrimList(reference.substring(reference.lastIndexOf("#") + 1), '/');
					referencePathParts.removeAll(Collections.singleton(""));
					JsonObject referencedObject = additionalSchemaDocumentNodes.get(packageName);
					for (String referencePathPart : referencePathParts) {
						referencePathPart = referencePathPart.replace("~0", "~").replace("~1", "/").replace("%25", "%");
						if (!referencedObject.containsPropertyKey(referencePathPart)) {
							throw new JsonSchemaDefinitionError("Referenced JsonSchema does not contain the reference path '" + reference + "'", "$");
						} else if (referencedObject.get(referencePathPart) == null) {
							throw new JsonSchemaDefinitionError("Invalid data type 'null' for reference path '" + reference + "'", "$");
						} else if (!(referencedObject.get(referencePathPart) instanceof JsonObject)) {
							throw new JsonSchemaDefinitionError("Invalid data type '" + schemaDocumentNode.get("definitions").getClass().getSimpleName() + "' for reference path '" + reference + "'", "$");
						} else {
							referencedObject = (JsonObject) referencedObject.get(referencePathPart);
						}
					}
					return referencedObject;
				}
			}
		} else {
			throw new Exception("Invalid JSON schema reference key 'null'");
		}
	}

	public void addJsonSchemaDefinition(String definitionPackageName, InputStream jsonSchemaInputStream) throws Exception {
		if (Utilities.isBlank(definitionPackageName)) {
			throw new Exception("Invalid empty JSON schema definition package name");
		} else if (additionalSchemaDocumentNodes.containsKey(definitionPackageName)) {
			throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' was already added before");
		} else {
			try (Json5Reader reader = new Json5Reader(jsonSchemaInputStream)) {
				JsonNode jsonNode = reader.read();
				if (!jsonNode.isJsonObject()) {
					throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' does not contain JSON schema data of type 'object'");
				} else {
					JsonObject jsonSchema = (JsonObject) jsonNode.getValue();
					redirectReferences(jsonSchema, "#", definitionPackageName + "#");
					additionalSchemaDocumentNodes.put(definitionPackageName, jsonSchema);
				}
			}
		}
	}

	private void redirectReferences(JsonObject jsonObject, String referenceDefinitionStart, String referenceDefinitionReplacement) {
		for (Entry<String, Object> entry : jsonObject.entrySet()) {
			if ("$ref".equals(entry.getKey()) && entry.getValue() != null && entry.getValue() instanceof String && ((String) entry.getValue()).startsWith(referenceDefinitionStart)) {
				jsonObject.add("$ref", referenceDefinitionReplacement + ((String) entry.getValue()).substring(referenceDefinitionStart.length()));
			} else if (entry.getValue() instanceof JsonObject) {
				redirectReferences((JsonObject) entry.getValue(), referenceDefinitionStart, referenceDefinitionReplacement);
			} else if (entry.getValue() instanceof JsonArray) {
				redirectReferences((JsonArray) entry.getValue(), referenceDefinitionStart, referenceDefinitionReplacement);
			}
		}
	}

	private void redirectReferences(JsonArray jsonArray, String referenceDefinitionStart, String referenceDefinitionReplacement) {
		for (Object item : jsonArray) {
			if (item instanceof JsonObject) {
				redirectReferences((JsonObject) item, referenceDefinitionStart, referenceDefinitionReplacement);
			} else if (item instanceof JsonArray) {
				redirectReferences((JsonArray) item, referenceDefinitionStart, referenceDefinitionReplacement);
			}
		}
	}

	public void checkCyclicDependency(String jsonPath, String validatorData, String jsonSchemaPath) throws JsonSchemaDefinitionError {
		if (latestJsonPath == null || !latestJsonPath.equals(jsonPath)) {
			latestJsonPath = jsonPath;
			latestDependencies = new HashSet<String>();
		}
		if (latestDependencies.contains((String) validatorData)) {
			throw new JsonSchemaDefinitionError("Cyclic dependency detected: '" + Utilities.join(latestDependencies, "', ") + "'", jsonSchemaPath);
		} else{
			latestDependencies.add((String) validatorData);
		}
	}
}
