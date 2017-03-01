package de.soderer.utilities.json.schema.validator;

import java.text.ParseException;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.NetworkUtilities;
import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.schema.JsonSchemaDataValidationError;
import de.soderer.utilities.json.schema.JsonSchemaDefinitionError;
import de.soderer.utilities.json.schema.JsonSchemaDependencyResolver;

public class FormatValidator extends BaseJsonSchemaValidator {
    public FormatValidator(JsonSchemaDependencyResolver jsonSchemaDependencyResolver, String jsonSchemaPath, Object validatorData, JsonNode jsonNode, String jsonPath) throws JsonSchemaDefinitionError {
    	super(jsonSchemaDependencyResolver, jsonSchemaPath, validatorData, jsonNode, jsonPath);
    	
    	if (validatorData == null) {
    		throw new JsonSchemaDefinitionError("Format value is 'null'", jsonSchemaPath);
    	} else if (!(validatorData instanceof String)) {
    		throw new JsonSchemaDefinitionError("Format value is not a string", jsonSchemaPath);
    	}
    }
	
	@Override
	public void validate() throws JsonSchemaDefinitionError, JsonSchemaDataValidationError {
		if (!jsonNode.isString()) {
			throw new JsonSchemaDataValidationError("Expected a string value for formatcheck but was '" + jsonNode.getJsonDataType().getName() + "'", jsonPath);
		} else if ("email".equalsIgnoreCase((String) validatorData)) {
			if (!jsonNode.isString() || !NetworkUtilities.isValidEmail((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("date-time".equalsIgnoreCase((String) validatorData)) {
			try {
				DateUtilities.ISO_8601_DATETIME_FORMAT.parse((String) jsonNode.getValue());
			} catch (ParseException e) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("hostname".equalsIgnoreCase((String) validatorData)) {
			if (!jsonNode.isString() || !NetworkUtilities.isValidHostname((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("ipv4".equalsIgnoreCase((String) validatorData)) {
			if (!jsonNode.isString() || !NetworkUtilities.isValidIpV4((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("ipv6".equalsIgnoreCase((String) validatorData)) {
			if (!jsonNode.isString() || !NetworkUtilities.isValidIpV6((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else if ("uri".equalsIgnoreCase((String) validatorData)) {
			if (!jsonNode.isString() || !NetworkUtilities.isValidUri((String) jsonNode.getValue())) {
				throw new JsonSchemaDataValidationError("Invalid data for format '" + ((String) validatorData) + "' was '" + jsonNode.getValue() + "'", jsonPath);
			}
		} else {
			throw new JsonSchemaDefinitionError("Unknown format name '" + validatorData + "'", jsonSchemaPath);
		}
    }
}
