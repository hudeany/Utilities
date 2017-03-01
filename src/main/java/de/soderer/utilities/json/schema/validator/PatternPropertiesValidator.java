package de.soderer.utilities.json.schema.validator;

import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;

public class PatternPropertiesValidator extends ExtendedBaseJsonSchemaValidator {
    public PatternPropertiesValidator(JsonObject parentValidatorData, JsonSchemaDependencyResolver jsonSchemaDependencyResolver, String jsonSchemaPath, Object validatorData, JsonNode jsonNode, String jsonPath) throws JsonSchemaDefinitionError {
    	super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);
    	
    	if (!(validatorData instanceof JsonObject)) {
    		throw new JsonSchemaDefinitionError("PatternProperties data is not a JsonObject", jsonSchemaPath);
    	}
    }
	
	@Override
	public void validate() throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		}
		
		for (Entry<String, Object> propertyEntry : ((JsonObject) jsonNode.getValue()).entrySet()) {
			for (Entry<String, Object> entry : ((JsonObject) validatorData).entrySet()) {
				if (!(entry.getValue() instanceof JsonObject)) {
					throw new JsonSchemaDefinitionError("PatternProperties data contains a non-JsonObject", jsonSchemaPath);
		    	}
				Pattern propertyKeyPattern;
				try {
					propertyKeyPattern = Pattern.compile(entry.getKey());
				} catch (Exception e1) {
					throw new JsonSchemaDefinitionError("PatternProperties data contains invalid RegEx pattern: " + entry.getKey(), jsonSchemaPath);
				}
				
				if (propertyKeyPattern.matcher(propertyEntry.getKey()).find()) {
					JsonNode jsonNode;
					try {
						jsonNode = new JsonNode(propertyEntry.getValue());
					} catch (Exception e) {
						throw new JsonSchemaDataValidationError("Invalid property data type was '" + propertyEntry.getValue().getClass().getSimpleName() + "'", jsonPath + "." + propertyEntry.getKey());
					}
					List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators(((JsonObject) entry.getValue()), jsonSchemaDependencyResolver, jsonSchemaPath, jsonNode, jsonPath + "." + propertyEntry.getKey());
					for (BaseJsonSchemaValidator subValidator : subValidators) {
						subValidator.validate();
					}
				}
			}
		}

		if (parentValidatorData.containsPropertyKey("additionalProperties")) {
			Object additionalPropertiesRaw = parentValidatorData.get("additionalProperties");
			if (additionalPropertiesRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'additionalProperties' is 'null'", jsonSchemaPath);
			} else if (additionalPropertiesRaw instanceof Boolean) {
				if (!(Boolean) additionalPropertiesRaw) {
					for (Entry<String, Object> propertyEntry : ((JsonObject) jsonNode.getValue()).entrySet()) {
						boolean foundMatchingPattern = false;
						for (Entry<String, Object> entry : ((JsonObject) validatorData).entrySet()) {
							Pattern propertyKeyPattern;
							try {
								propertyKeyPattern = Pattern.compile(entry.getKey());
							} catch (Exception e1) {
								throw new JsonSchemaDefinitionError("PatternProperties data contains invalid RegEx pattern: " + entry.getKey(), jsonSchemaPath);
							}
						
							if (propertyKeyPattern.matcher(propertyEntry.getKey()).find()) {
								foundMatchingPattern = true;
								break;
							}
						}

						if (!foundMatchingPattern) {
							throw new JsonSchemaDataValidationError("Invalid property key found '" + propertyEntry.getKey() + "'", jsonPath + "." + propertyEntry.getKey());
						}
					}
				}
			} else if (additionalPropertiesRaw instanceof JsonObject) {
				for (Entry<String, Object> propertyEntry : ((JsonObject) jsonNode.getValue()).entrySet()) {
					boolean foundMatchingPattern = false;
					for (Entry<String, Object> entry : ((JsonObject) validatorData).entrySet()) {
						Pattern propertyKeyPattern;
						try {
							propertyKeyPattern = Pattern.compile(entry.getKey());
						} catch (Exception e1) {
							throw new JsonSchemaDefinitionError("PatternProperties data contains invalid RegEx pattern: " + entry.getKey(), jsonSchemaPath);
						}
					
						if (propertyKeyPattern.matcher(propertyEntry.getKey()).find()) {
							foundMatchingPattern = true;
							break;
						}
					}

					if (!foundMatchingPattern) {
						JsonNode jsonNode;
						try {
							jsonNode = new JsonNode(propertyEntry.getValue());
						} catch (Exception e) {
							throw new JsonSchemaDefinitionError("Invalid data: " + e.getMessage(), jsonSchemaPath);
						}

						List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators(((JsonObject) additionalPropertiesRaw), jsonSchemaDependencyResolver, jsonSchemaPath, jsonNode, jsonPath + "." + propertyEntry.getKey());
						for (BaseJsonSchemaValidator subValidator : subValidators) {
							subValidator.validate();
						}
					}
				}
			} else {
				throw new JsonSchemaDefinitionError("Properties data is not an 'object' or 'boolean'", jsonSchemaPath);
			}
		}
    }
}
