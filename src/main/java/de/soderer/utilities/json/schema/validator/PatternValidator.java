package de.soderer.utilities.json.schema.validator;

import java.util.regex.Pattern;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;

public class PatternValidator extends BaseJsonSchemaValidator {
    public PatternValidator(JsonSchemaDependencyResolver jsonSchemaDependencyResolver, String jsonSchemaPath, Object validatorData, JsonNode jsonNode, String jsonPath) throws JsonSchemaDefinitionError {
    	super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);
    	
    	if (!(validatorData instanceof String)) {
    		throw new JsonSchemaDefinitionError("Pattern is no string", jsonSchemaPath);
    	}
    }
	
	@Override
	public void validate() throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		Pattern pattern = Pattern.compile((String) validatorData);
		if (jsonNode.isNumber()) {
			if (!pattern.matcher(((Number) jsonNode.getValue()).toString()).find()) {
				throw new JsonSchemaDataValidationError("RegEx pattern '" + (String) validatorData + "' is not matched by data number '" + (String) jsonNode.getValue() + "'", jsonPath);
			}
		} else if (jsonNode.isBoolean()) {
			if (!pattern.matcher(((Boolean) jsonNode.getValue()).toString()).find()) {
				throw new JsonSchemaDataValidationError("RegEx pattern '" + (String) validatorData + "' is not matched by data boolean '" + (String) jsonNode.getValue() + "'", jsonPath);
			}
		} else if (jsonNode.isString()) {
			if (!pattern.matcher((String) jsonNode.getValue()).find()) {
				throw new JsonSchemaDataValidationError("RegEx pattern '" + (String) validatorData + "' is not matched by data string '" + (String) jsonNode.getValue() + "'", jsonPath);
			}
		} else {
			throw new JsonSchemaDataValidationError("Expected data type 'string' but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		}
    }
}
