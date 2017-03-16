package de.soderer.utilities.json;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class JsonSerializer {
	/**
	 * Serialize an object in json data<br />
	 * - Serializes null values<br />
	 * - Excludes static fields<br />
	 * - Excludes transient fields<br />
	 * 
	 * @param dataObject
	 * @return
	 * @throws Exception
	 */
	public static JsonNode serialize(Object dataObject) throws Exception {
		return serializeInternal(dataObject, false, false, false, null);
	}
	
	/**
	 * Serialize an object in json data
	 * 
	 * @param dataObject
	 * @param excludeNull Do not serialize null values except for the dataObject itself
	 * @param includeStatic Serialize values with a "static" modifier
	 * @param includeTransient Serialize values with a "transient" modifier
	 * @return
	 * @throws Exception
	 */
	public static JsonNode serialize(Object dataObject, boolean excludeNull, boolean includeStatic, boolean includeTransient) throws Exception {
		return serializeInternal(dataObject, excludeNull, includeStatic, includeTransient, null);
	}
		
	private static JsonNode serializeInternal(Object dataObject, boolean excludeNull, boolean includeStatic, boolean includeTransient, List<Object> alreadyVisitedObjects) throws Exception {
		if (alreadyVisitedObjects == null) {
			alreadyVisitedObjects = new ArrayList<Object>();
		}
		
		if (dataObject == null) {
			return new JsonNode(dataObject);
		} else if (dataObject instanceof Number
				|| dataObject instanceof String
				|| dataObject instanceof Date
				|| dataObject instanceof Boolean) {
			return new JsonNode(dataObject);
		} else if (dataObject.getClass().isArray()) {
			if (alreadyVisitedObjects.contains(dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				alreadyVisitedObjects.add(dataObject);
			}
		
			JsonArray jsonArray = new JsonArray();
			int length = Array.getLength(dataObject);
		    for (int i = 0; i < length; i ++) {
		        Object item = Array.get(dataObject, i);
		        if (item != null || !excludeNull) {
		        	jsonArray.add(serializeInternal(item, excludeNull, includeStatic, includeTransient, alreadyVisitedObjects).getValue());
		        }
		    }
		    
		    alreadyVisitedObjects.remove(dataObject);
		    
			return new JsonNode(jsonArray);
		} else if (dataObject instanceof Iterable<?>) {
			if (alreadyVisitedObjects.contains(dataObject)) {
				throw new Exception("Cyclic reference detected. Cannot serialize object: " + dataObject.getClass() + " (Hashcode " + System.identityHashCode(dataObject) + ")");
			} else {
				alreadyVisitedObjects.add(dataObject);
			}
			
			JsonArray jsonArray = new JsonArray();
			for (Object item : (Iterable<?>) dataObject) {
				if (item != null || !excludeNull) {
					jsonArray.add(serializeInternal(item, excludeNull, includeStatic, includeTransient, alreadyVisitedObjects).getValue());
				}
			}
		    
		    alreadyVisitedObjects.remove(dataObject);
		    
			return new JsonNode(jsonArray);
		} else {
			if (alreadyVisitedObjects.contains(dataObject)) {
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
						dataJsonObject.add(fieldName, serializeInternal(fieldData, excludeNull, includeStatic, includeTransient, alreadyVisitedObjects).getValue());
					}
				}
			}
		    
		    alreadyVisitedObjects.remove(dataObject);
		    
			return new JsonNode(dataJsonObject);
		}
	}
}
