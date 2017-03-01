package de.soderer.utilities.json.schema;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.json.Json5Reader;
import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;

public class JsonSchemaDependencyResolver {
	private JsonObject directDependencies = null;
	private Map<String, JsonObject> indirectDependencies = new HashMap<String, JsonObject>();

	private String latestJsonPath = null;
	private Set<String> latestDependencies;
	
	public JsonSchemaDependencyResolver(JsonObject directDependencies) {
		this.directDependencies = directDependencies;
	}

	public Object getDependencyByReference(String reference) {
		if (reference != null) {
			if (reference.startsWith("#/definitions/")) {
				if (directDependencies == null) {
					return null;
				} else {
					return directDependencies.get(reference.substring(14));
				}
			} else {
				if (!reference.contains("#/definitions/")) {
					if (directDependencies != null && directDependencies.containsPropertyKey(reference)) {
						return directDependencies.get(reference);
					} else {
						for (JsonObject indirectJsonDefinitions : indirectDependencies.values()) {
							if (indirectJsonDefinitions.containsPropertyKey(reference)) {
								return indirectJsonDefinitions.get(reference);
							}
						}
						return null;
					}
				} else {
					String packageName = reference.substring(0, reference.indexOf("#/definitions/"));
					if (!indirectDependencies.containsKey(packageName)) {
						return null;
					} else {
						String referenceName = reference.substring(reference.indexOf("#/definitions/") + 14);
						return indirectDependencies.get(packageName).get(referenceName);
					}
				}
			}
		} else {
			return null;
		}
	}

	public boolean containsReference(String reference) throws Exception {
		if (reference != null) {
			if (reference.startsWith("#/definitions/")) {
				if (directDependencies == null) {
					throw new Exception("No direct reference definitions found in JSON schema");
				} else {
					return directDependencies.containsPropertyKey(reference.substring(14));
				}
			} else {
				if (!reference.contains("#/definitions/")) {
					if (directDependencies != null && directDependencies.containsPropertyKey(reference)) {
						return true;
					} else {
						for (JsonObject indirectJsonDefinitions : indirectDependencies.values()) {
							if (indirectJsonDefinitions.containsPropertyKey(reference)) {
								return true;
							}
						}
						throw new Exception("Invalid JSON schema reference key '" + reference + "'. Use simple reference keys or this pattern for reference keys: '<referenced packagename or empty>#/definitions/<your reference key>'");
					}
				} else {
					String packageName = reference.substring(0, reference.indexOf("#/definitions/"));
					if (!indirectDependencies.containsKey(packageName)) {
						throw new Exception("Unknown JSON schema reference package name '" + packageName + "'");
					} else {
						String referenceName = reference.substring(reference.indexOf("#/definitions/") + 14);
						return indirectDependencies.get(packageName).containsPropertyKey(referenceName);
					}
				}
			}
		} else {
			return false;
		}
	}

	public void addJsonSchemaDefinition(String definitionPackageName, InputStream jsonSchemaInputStream) throws Exception {
		if (Utilities.isBlank(definitionPackageName)) {
			throw new Exception("Invalid empty JSON schema definition package name");
		} else if (indirectDependencies.containsKey(definitionPackageName)) {
			throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' was already added before");
		} else {
			try (Json5Reader reader = new Json5Reader(jsonSchemaInputStream)) {
				JsonNode jsonNode = reader.read();
				if (!jsonNode.isJsonObject()) {
					throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' does not contain JSON schema data of type 'object'");
				} else if (!((JsonObject) jsonNode.getValue()).containsPropertyKey("definitions")) {
					throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' does not contain JSON schema data key 'definitions'");
				} else if (((JsonObject) jsonNode.getValue()).get("definitions") == null) {
					throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' contains empty JSON schema definitions");
				}  else if (!(((JsonObject) jsonNode.getValue()).get("definitions") instanceof JsonObject)) {
					throw new Exception("Additional JSON schema definition package '" + definitionPackageName + "' contains invalid JSON schema definitions of type '" + ((JsonObject) jsonNode.getValue()).get("definitions").getClass().getSimpleName() + "'");
				} else {
					JsonObject jsonSchema = (JsonObject) ((JsonObject) jsonNode.getValue()).get("definitions");
					redirectReferences(jsonSchema, "#/definitions/", definitionPackageName + "#/definitions/");
					indirectDependencies.put(definitionPackageName, jsonSchema);
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
