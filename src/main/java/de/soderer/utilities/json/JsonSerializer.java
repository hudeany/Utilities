package de.soderer.utilities.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.soderer.utilities.Utilities;

public class JsonSerializer {
	/**
	 * Serialize an object in json data<br />
	 * - Serializes null values<br />
	 * - Excludes static fields<br />
	 * - Excludes transient fields<br />
	 * - Does not show object type infos<br />
	 * 
	 * @param dataObject
	 * @return
	 * @throws Exception
	 */
	public static JsonNode serialize(Object dataObject) throws Exception {
		return serializeInternal(dataObject, false, false, false, false, null);
	}
	
	/**
	 * Serialize an object in json data
	 * 
	 * @param dataObject
	 * @param excludeNull Do not serialize null values except for the dataObject itself
	 * @param includeStatic Serialize values with a "static" modifier
	 * @param includeTransient Serialize values with a "transient" modifier
	 * @param addObjectTypeInfo Add object type info for non-simple objects
	 * @return
	 * @throws Exception
	 */
	public static JsonNode serialize(Object dataObject, boolean excludeNull, boolean includeStatic, boolean includeTransient, boolean addObjectTypeInfo) throws Exception {
		return serializeInternal(dataObject, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, null);
	}
		
	private static JsonNode serializeInternal(Object dataObject, boolean excludeNull, boolean includeStatic, boolean includeTransient, boolean addObjectTypeInfo, List<Object> alreadyVisitedObjects) throws Exception {
		if (alreadyVisitedObjects == null) {
			alreadyVisitedObjects = new ArrayList<Object>();
		}
		
		if (dataObject == null) {
			if (addObjectTypeInfo) {
		    	JsonObject jsonObjectWithTypeInfo = new JsonObject();
		    	jsonObjectWithTypeInfo.add("class", null);
		    	jsonObjectWithTypeInfo.add("value", null);
		    	return new JsonNode(jsonObjectWithTypeInfo);
		    } else {
		    	return new JsonNode(dataObject);
		    }
		} else if (dataObject instanceof Number
				|| dataObject instanceof String
				|| dataObject instanceof Date
				|| dataObject instanceof Boolean) {
			if (addObjectTypeInfo) {
		    	JsonObject jsonObjectWithTypeInfo = new JsonObject();
		    	jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
		    	jsonObjectWithTypeInfo.add("value", dataObject);
		    	return new JsonNode(jsonObjectWithTypeInfo);
		    } else {
		    	return new JsonNode(dataObject);
		    }
		} else if (dataObject.getClass().isArray()) {
			if (Utilities.containsObject(alreadyVisitedObjects, dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				alreadyVisitedObjects.add(dataObject);
			}
		
			JsonArray jsonArray = new JsonArray();
			int length = Array.getLength(dataObject);
		    for (int i = 0; i < length; i ++) {
		        Object item = Array.get(dataObject, i);
		        if (item != null || !excludeNull) {
		        	jsonArray.add(serializeInternal(item, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects).getValue());
		        }
		    }
		    
		    alreadyVisitedObjects.remove(dataObject);
		    
		    if (addObjectTypeInfo) {
		    	JsonObject jsonObjectWithTypeInfo = new JsonObject();
		    	jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
		    	jsonObjectWithTypeInfo.add("value", jsonArray);
		    	return new JsonNode(jsonObjectWithTypeInfo);
		    } else {
		    	return new JsonNode(jsonArray);
		    }
		} else if (dataObject instanceof Iterable<?>) {
			if (Utilities.containsObject(alreadyVisitedObjects, dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				alreadyVisitedObjects.add(dataObject);
			}
			
			JsonArray jsonArray = new JsonArray();
			for (Object item : (Iterable<?>) dataObject) {
				if (item != null || !excludeNull) {
					jsonArray.add(serializeInternal(item, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects).getValue());
				}
			}
		    
		    alreadyVisitedObjects.remove(dataObject);
		    
		    if (addObjectTypeInfo) {
		    	JsonObject jsonObjectWithTypeInfo = new JsonObject();
		    	jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
		    	jsonObjectWithTypeInfo.add("value", jsonArray);
		    	return new JsonNode(jsonObjectWithTypeInfo);
		    } else {
		    	return new JsonNode(jsonArray);
		    }
		} else {
			if (Utilities.containsObject(alreadyVisitedObjects, dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				alreadyVisitedObjects.add(dataObject);
			}
			
			JsonObject dataJsonObject = new JsonObject();
			Class<?> dataClass = dataObject.getClass();
			for (Field dataField : dataClass.getDeclaredFields()) {
				boolean serializeField = true;
				if ((dataField.getModifiers() & Modifier.TRANSIENT) == Modifier.TRANSIENT && !includeTransient) {
					serializeField = false;
				} else if ((dataField.getModifiers() & Modifier.STATIC) == Modifier.STATIC && !includeStatic) {
					serializeField = false;
				}
				
				if (serializeField) {
					dataField.setAccessible(true);
					String fieldName = dataField.getName();
					Object fieldData = dataField.get(dataObject);
					if (fieldData != null || !excludeNull) {
						if (fieldData == null && addObjectTypeInfo) {
							JsonObject jsonObjectWithTypeInfo = new JsonObject();
					    	jsonObjectWithTypeInfo.add("class", dataField.getType().getName());
					    	jsonObjectWithTypeInfo.add("value", null);
							dataJsonObject.add(fieldName, jsonObjectWithTypeInfo);
						} else {
							dataJsonObject.add(fieldName, serializeInternal(fieldData, excludeNull, includeStatic, includeTransient, addObjectTypeInfo, alreadyVisitedObjects).getValue());
						}
					}
				}
			}
		    
		    alreadyVisitedObjects.remove(dataObject);
		    
		    if (addObjectTypeInfo) {
		    	JsonObject jsonObjectWithTypeInfo = new JsonObject();
		    	jsonObjectWithTypeInfo.add("class", dataObject.getClass().getName());
		    	jsonObjectWithTypeInfo.add("value", dataJsonObject);
		    	return new JsonNode(jsonObjectWithTypeInfo);
		    } else {
		    	return new JsonNode(dataJsonObject);
		    }
		}
	}
}
