package de.soderer.utilities.json.schema.validator;

import java.util.List;
import java.util.Map.Entry;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;

public class PropertiesValidator extends ExtendedBaseJsonSchemaValidator {
	public PropertiesValidator(JsonObject parentValidatorData, JsonSchemaDependencyResolver jsonSchemaDependencyResolver, String jsonSchemaPath, Object validatorData, JsonNode jsonNode, String jsonPath) throws JsonSchemaDefinitionError {
		super(parentValidatorData, jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);
		
		if (!(validatorData instanceof JsonObject)) {
			throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath);
    	}
	}
	
	@Override
	public void validate() throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		if (!(jsonNode.isJsonObject())) {
			throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		}
		
		for (Entry<String, Object> entry : ((JsonObject) validatorData).entrySet()) {
			if (!(entry.getValue() instanceof JsonObject)) {
				throw new JsonSchemaDefinitionError("Properties data is not a JsonObject", jsonSchemaPath);
	    	}
			if (((JsonObject) jsonNode.getValue()).containsPropertyKey(entry.getKey())) {
				JsonNode newJsonNode;
				try {
					newJsonNode = new JsonNode(((JsonObject) jsonNode.getValue()).get(entry.getKey()));
				} catch (Exception e) {
					throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonObject) jsonNode.getValue()).get(entry.getKey()).getClass().getSimpleName() + "'", jsonPath + "." + entry.getKey());
				}
				List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators(((JsonObject) entry.getValue()), jsonSchemaDependencyResolver, jsonSchemaPath, newJsonNode, jsonPath + "." + entry.getKey());
				for (BaseJsonSchemaValidator subValidator : subValidators) {
					subValidator.validate();
				}
			}
		}

		if (parentValidatorData.containsPropertyKey("additionalProperties")) {
			Object additionalPropertiesRaw = parentValidatorData.get("additionalProperties");
			if (additionalPropertiesRaw == null) {
				throw new JsonSchemaDefinitionError("Property 'additionalProperties' is 'null'", jsonSchemaPath);
			} else if (additionalPropertiesRaw instanceof Boolean) {
				if (!(Boolean) additionalPropertiesRaw) {
					for (String propertyKey : ((JsonObject) jsonNode.getValue()).keySet()) {
						if (!((JsonObject) validatorData).containsPropertyKey(propertyKey)) {
							throw new JsonSchemaDataValidationError("Invalid property key. Expected one of '" + Utilities.join(((JsonObject) validatorData).keySet(), "', '") + "' but was '" + propertyKey + "'", jsonPath + "." + propertyKey);
						}
					}
				}
			} else if (additionalPropertiesRaw instanceof JsonObject) {
				for (String propertyKey : ((JsonObject) jsonNode.getValue()).keySet()) {
					if (!((JsonObject) validatorData).containsPropertyKey(propertyKey)) {
						JsonNode newJsonNode;
						try {
							newJsonNode = new JsonNode(((JsonObject) jsonNode.getValue()).get(propertyKey));
						} catch (Exception e) {
							throw new JsonSchemaDataValidationError("Invalid data type '" + ((JsonObject) jsonNode.getValue()).get(propertyKey).getClass().getSimpleName() + "'", jsonPath + "." + propertyKey);
						}
						List<BaseJsonSchemaValidator> subValidators = JsonSchema.createValidators(((JsonObject) additionalPropertiesRaw), jsonSchemaDependencyResolver, jsonSchemaPath, newJsonNode, jsonPath + "." + propertyKey);
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
