package de.soderer.utilities.json.schema.validator;

import java.util.ArrayList;
import java.util.List;

import de.soderer.utilities.json.JsonArray;
import de.soderer.utilities.json.JsonDataType;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.schema.JsonSchema;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;

public class TypeValidator extends BaseJsonSchemaValidator {
	private List<BaseJsonSchemaValidator> subValidators = null;
	
    public TypeValidator(JsonSchemaDependencyResolver jsonSchemaDependencyResolver, String jsonSchemaPath, Object validatorData, JsonNode jsonNode, String jsonPath) throws JsonSchemaDefinitionError {
    	super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);
    	
    	if (validatorData instanceof JsonArray) {
    		subValidators = new ArrayList<BaseJsonSchemaValidator>();
    		for (Object subValidationData : ((JsonArray) validatorData)) {
    			if (subValidationData instanceof JsonObject) {
    				subValidators.addAll(JsonSchema.createValidators((JsonObject) subValidationData, jsonSchemaDependencyResolver, jsonSchemaPath, jsonNode, jsonPath));
    			} else {
    				throw new JsonSchemaDefinitionError("Type array contains a non-JsonObject", jsonSchemaPath);
    			}
    		}
    	}
    }

	@Override
	public void validate() throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		if (subValidators == null) {
			JsonDataType jsonDataType;
			try {
				jsonDataType = JsonDataType.getFromString((String) validatorData);
			} catch (Exception e) {
				throw new JsonSchemaDefinitionError("Invalid JSON data type '" + validatorData + "'", jsonSchemaPath);
			}
			switch(jsonDataType) {
				case NULL:
					if (!jsonNode.isNull()) {
						throw new JsonSchemaDataValidationError("Expected data type 'null' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
					break;
				case OBJECT:
					if (jsonNode.isNull()) {
						throw new JsonSchemaDataValidationError("Expected data type 'object' but was 'null'", jsonPath);
					} else if (!jsonNode.isJsonObject()) {
						throw new JsonSchemaDataValidationError("Expected data type 'object' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
					break;
				case ARRAY:
					if (jsonNode.isNull()) {
						throw new JsonSchemaDataValidationError("Expected data type 'array' but was 'null'", jsonPath);
					} else if (!jsonNode.isJsonArray()) {
						throw new JsonSchemaDataValidationError("Expected data type 'array' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
					break;
				case BOOLEAN:
					if (jsonNode.isNull()) {
						throw new JsonSchemaDataValidationError("Expected data type 'boolean' but was 'null'", jsonPath);
					} else if (!jsonNode.isBoolean()) {
						throw new JsonSchemaDataValidationError("Expected data type 'boolean' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
					break;
				case INTEGER:
					if (jsonNode.isNull()) {
						throw new JsonSchemaDataValidationError("Expected data type 'integer' but was 'null'", jsonPath);
					} else if (!jsonNode.isInteger()) {
						throw new JsonSchemaDataValidationError("Expected data type 'integer' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
					break;
				case NUMBER:
					if (jsonNode.isNull()) {
						throw new JsonSchemaDataValidationError("Expected data type 'number' but was 'null'", jsonPath);
					} else if (!jsonNode.isNumber()) {
						throw new JsonSchemaDataValidationError("Expected data type 'number' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
					break;
				case STRING:
					if (jsonNode.isNull()) {
						throw new JsonSchemaDataValidationError("Expected data type 'string' but was 'null'", jsonPath);
					} else if (!jsonNode.isString()) {
						throw new JsonSchemaDataValidationError("Expected data type 'string' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
					}
					break;
				default:
					throw new JsonSchemaDefinitionError("Invalid data type '" + jsonDataType.toString().toLowerCase() + "'", jsonSchemaPath);
			}
		} else {
			for (BaseJsonSchemaValidator subValidator : subValidators) {
				subValidator.validate();
			}
		}
	}
}
