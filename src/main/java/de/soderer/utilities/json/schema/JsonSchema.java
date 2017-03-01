package de.soderer.utilities.json.schema;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.soderer.utilities.json.Json5Reader;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.JsonReader;
import de.soderer.utilities.json.schema.validator.AllOfValidator;
import de.soderer.utilities.json.schema.validator.AnyOfValidator;
import de.soderer.utilities.json.schema.validator.BaseJsonSchemaValidator;
import de.soderer.utilities.json.schema.validator.DependenciesValidator;
import de.soderer.utilities.json.schema.validator.EnumValidator;
import de.soderer.utilities.json.schema.validator.FormatValidator;
import de.soderer.utilities.json.schema.validator.ItemsValidator;
import de.soderer.utilities.json.schema.validator.MaxItemsValidator;
import de.soderer.utilities.json.schema.validator.MaxLengthValidator;
import de.soderer.utilities.json.schema.validator.MaxPropertiesValidator;
import de.soderer.utilities.json.schema.validator.MaximumValidator;
import de.soderer.utilities.json.schema.validator.MinItemsValidator;
import de.soderer.utilities.json.schema.validator.MinLengthValidator;
import de.soderer.utilities.json.schema.validator.MinPropertiesValidator;
import de.soderer.utilities.json.schema.validator.MinimumValidator;
import de.soderer.utilities.json.schema.validator.MultipleOfValidator;
import de.soderer.utilities.json.schema.validator.NotValidator;
import de.soderer.utilities.json.schema.validator.OneOfValidator;
import de.soderer.utilities.json.schema.validator.PatternPropertiesValidator;
import de.soderer.utilities.json.schema.validator.PatternValidator;
import de.soderer.utilities.json.schema.validator.PropertiesValidator;
import de.soderer.utilities.json.schema.validator.ReferenceValidator;
import de.soderer.utilities.json.schema.validator.RequiredValidator;
import de.soderer.utilities.json.schema.validator.TypeValidator;
import de.soderer.utilities.json.schema.validator.UniqueItemsValidator;

public class JsonSchema {
	/**
	 * Url describing the JSON schema standard and version a JSON schema was written for in compliance
	 * Example: "http://json-schema.org/schema#" 
	 */
	private String schemaVersionUrl;

	private String id;
	private String title;
	private String description;
	private JsonObject jsonSchemaDefinition;
	private JsonSchemaDependencyResolver jsonSchemaDependencies;
	
	public JsonSchema(InputStream jsonSchemaInputStream) throws JsonSchemaDefinitionError  {
		JsonNode jsonNode;
		try (JsonReader jsonReader = new Json5Reader(jsonSchemaInputStream)) {
			jsonNode = jsonReader.read();
		} catch (Exception e) {
			throw new JsonSchemaDefinitionError("Cannot read JSON-Schema: " + e.getMessage(), null);
		}
		
		if (jsonNode == null) {
			throw new JsonSchemaDefinitionError("Contains null data", null);
		} if (jsonNode.isJsonObject()) {
			jsonSchemaDefinition = (JsonObject) jsonNode.getValue();

			if (jsonSchemaDefinition.containsPropertyKey("id")) {
				if (jsonSchemaDefinition.get("id") == null) {
					throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'id'", "$");
				} else if (!(jsonSchemaDefinition.get("id") instanceof String)) {
					throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinition.get("id").getClass().getSimpleName() + "' for key 'id'", "$");
				} else {
					id = (String) jsonSchemaDefinition.get("id");
				}
			}
			
			if (jsonSchemaDefinition.containsPropertyKey("$schema")) {
				if (jsonSchemaDefinition.get("$schema") == null) {
					throw new JsonSchemaDefinitionError("Invalid data type 'null' for key '$schema'", "$");
				} else if (!(jsonSchemaDefinition.get("$schema") instanceof String)) {
					throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinition.get("$schema").getClass().getSimpleName() + "' for key '$schema'", "$");
				} else {
					schemaVersionUrl = (String) jsonSchemaDefinition.get("$schema");
				}
			}
			
			if (jsonSchemaDefinition.containsPropertyKey("title")) {
				if (jsonSchemaDefinition.get("title") == null) {
					throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'title'", "$");
				} else if (!(jsonSchemaDefinition.get("title") instanceof String)) {
					throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinition.get("title").getClass().getSimpleName() + "' for key 'title'", "$");
				} else {
					title = (String) jsonSchemaDefinition.get("title");
				}
			}
			
			if (jsonSchemaDefinition.containsPropertyKey("description")) {
				if (jsonSchemaDefinition.get("description") == null) {
					throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'description'", "$");
				} else if (!(jsonSchemaDefinition.get("description") instanceof String)) {
					throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinition.get("title").getClass().getSimpleName() + "' for key 'description'", "$");
				} else {
					description = (String) jsonSchemaDefinition.get("description");
				}
			}
			
			if (jsonSchemaDefinition.containsPropertyKey("definitions")) {
				if (jsonSchemaDefinition.get("definitions") == null) {
					throw new JsonSchemaDefinitionError("Invalid data type 'null' for key 'definitions'", "$");
				} else if (!(jsonSchemaDefinition.get("definitions") instanceof JsonObject)) {
					throw new JsonSchemaDefinitionError("Invalid data type '" + jsonSchemaDefinition.get("definitions").getClass().getSimpleName() + "' for key 'definitions'", "$");
				} else {
					jsonSchemaDependencies = new JsonSchemaDependencyResolver((JsonObject) jsonSchemaDefinition.get("definitions"));
				}
			} else {
				jsonSchemaDependencies = new JsonSchemaDependencyResolver(null);
			}
		} else {
			throw new JsonSchemaDefinitionError("Does not contain JsonObject", "$");
		}
	}
	
	/**
	 * Add some other JSON schema for usage of its reference definitions
	 * 
	 * @param definitionPackageName
	 * @param jsonSchemaInputStream
	 * @throws Exception 
	 */
	public void addJsonSchemaDefinition(String definitionPackageName, InputStream jsonSchemaInputStream) throws Exception {
		jsonSchemaDependencies.addJsonSchemaDefinition(definitionPackageName, jsonSchemaInputStream);
	}
	
	public String getId() {
		return id;
	}
	
	public String getSchemaVersionUrl() {
		return schemaVersionUrl;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public void validate(InputStream jsonDataInputStream) throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		JsonNode jsonDataNode;
		try (JsonReader jsonReader = new Json5Reader(jsonDataInputStream)) {
			jsonDataNode = jsonReader.read();
		} catch (Exception e) {
			e.printStackTrace();
			throw new JsonSchemaDataValidationError("Cannot read JSON data: " + e.getMessage(), "");
		}
		
		List<BaseJsonSchemaValidator> validators = createValidators(jsonSchemaDefinition, jsonSchemaDependencies, "$", jsonDataNode, "$");
		for (BaseJsonSchemaValidator validator : validators) {
			validator.validate();
		}
	}
	
	public static List<BaseJsonSchemaValidator> createValidators(JsonObject jsonSchemaDefinitionObject, JsonSchemaDependencyResolver jsonSchemaDependencyResolver, String currentJsonSchemaPath, JsonNode jsonNode, String currentJsonPath) throws JsonSchemaDefinitionError {
		List<BaseJsonSchemaValidator> validators = new ArrayList<BaseJsonSchemaValidator>();
		boolean hasPropertiesValidator = false;
		boolean hasPropertiesPatternValidator = false;
		boolean hasAdditionalProperties = false;
		boolean hasMinimumValidator = false;
		boolean hasItemsValidator = false;
		boolean hasExclusiveMinimum = false;
		boolean hasMaximumValidator = false;
		boolean hasExclusiveMaximum = false;
		boolean hasAdditionalItems = false;
		for (Entry<String, Object> entry : jsonSchemaDefinitionObject.entrySet()) {
			switch (entry.getKey()) {
				case "type":
					validators.add(new TypeValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "properties":
					validators.add(new PropertiesValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					hasPropertiesValidator = true;
					break;
				case "required":
					validators.add(new RequiredValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "minProperties":
					validators.add(new MinPropertiesValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "maxProperties":
					validators.add(new MaxPropertiesValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "items":
					validators.add(new ItemsValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					hasItemsValidator = true;
					break;
				case "minItems":
					validators.add(new MinItemsValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "maxItems":
					validators.add(new MaxItemsValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "minLength":
					validators.add(new MinLengthValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "maxLength":
					validators.add(new MaxLengthValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "uniqueItems":
					validators.add(new UniqueItemsValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "minimum":
					validators.add(new MinimumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					hasMinimumValidator = true;
					break;
				case "maximum":
					validators.add(new MaximumValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					hasMaximumValidator = true;
					break;
				case "multipleOf":
					validators.add(new MultipleOfValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					hasMaximumValidator = true;
					break;
				case "anyOf":
					validators.add(new AnyOfValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "allOf":
					validators.add(new AllOfValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "oneOf":
					validators.add(new OneOfValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "not":
					validators.add(new NotValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "enum":
					validators.add(new EnumValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "format":
					validators.add(new FormatValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "pattern":
					validators.add(new PatternValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;
				case "patternProperties":
					validators.add(new PatternPropertiesValidator(jsonSchemaDefinitionObject, jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					hasPropertiesPatternValidator = true;
					break;
				case "dependencies":
					validators.add(new DependenciesValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					break;

				case "additionalProperties":
					// Do nothing, because this is validated by PropertiesValidator, too
					hasAdditionalProperties = true;
					break;
				case "exclusiveMinimum":
					// Do nothing, because this is validated by MinimumValidator, too
					hasExclusiveMinimum = true;
					break;
				case "exclusiveMaximum":
					// Do nothing, because this is validated by MaximumValidator, too
					hasExclusiveMaximum = true;
					break;
				case "additionalItems":
					// Do nothing, because this is validated by ItemsValidator, too
					hasAdditionalItems = true;
					break;
					
				case "$ref":
					validators.add(new ReferenceValidator(jsonSchemaDependencyResolver, currentJsonSchemaPath + "." + entry.getKey(), entry.getValue(), jsonNode, currentJsonPath));
					hasPropertiesPatternValidator = true;
					break;

				case "id":
					// id should be a descriptive url
					if (!"$".equals(currentJsonSchemaPath)) {
						throw new JsonSchemaDefinitionError("JSON schema 'id' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "$schema":
					// $schema should be a descriptive url
					if (!"$".equals(currentJsonSchemaPath)) {
						throw new JsonSchemaDefinitionError("JSON schema '$schema' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
				case "definitions":
					// Defined JSON schema definitions
					if (!"$".equals(currentJsonSchemaPath)) {
						throw new JsonSchemaDefinitionError("JSON schema 'definitions' must be defined on top level of JSON schema", currentJsonSchemaPath);
					}
					break;
					
				case "title":
					// Descriptive title
					if (!(entry.getValue() instanceof String)) {
						throw new JsonSchemaDefinitionError("Invalid data type '" + entry.getValue().getClass().getSimpleName() + "' for key 'description'", currentJsonSchemaPath);
					}
					break;
				case "description":
					// Descriptive comments
					if (!(entry.getValue() instanceof String)) {
						throw new JsonSchemaDefinitionError("Invalid data type '" + entry.getValue().getClass().getSimpleName() + "' for key 'description'", currentJsonSchemaPath);
					}
					break;
				case "default":
					// Default value for processing the given JSON data, which is irrelevant for validation
					break;
				default:
					throw new JsonSchemaDefinitionError("Unexpected data key '" + entry.getKey() + "'", currentJsonSchemaPath);
			}
			if (hasAdditionalProperties && !(hasPropertiesValidator || hasPropertiesPatternValidator)) {
				throw new JsonSchemaDefinitionError("Missing 'properties' rule for 'additionalProperties'", currentJsonSchemaPath);
			}
			if (hasExclusiveMinimum && !hasMinimumValidator) {
				throw new JsonSchemaDefinitionError("Missing 'minimum' rule for 'exclusiveMinimum'", currentJsonSchemaPath);
			}
			if (hasExclusiveMaximum && !hasMaximumValidator) {
				throw new JsonSchemaDefinitionError("Missing 'maximum' rule for 'exclusiveMaximum'", currentJsonSchemaPath);
			}
			if (hasAdditionalItems && !hasItemsValidator) {
				throw new JsonSchemaDefinitionError("Missing 'items' rule for 'additionalItems'", currentJsonSchemaPath);
			}
		}
		return validators;
	}
}
