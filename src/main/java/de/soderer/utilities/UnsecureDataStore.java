package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.soderer.utilities.json.JsonNode;
import de.soderer.utilities.json.JsonObject;
import de.soderer.utilities.json.JsonReader;
import de.soderer.utilities.json.JsonReader.JsonToken;
import de.soderer.utilities.json.JsonSerializer;
import de.soderer.utilities.json.JsonWriter;

public class UnsecureDataStore {
	/**
	 * First key is class name of entries grouped by their class name, second key is the entry name
	 */
	protected Map<String, Map<String, Object>> dataEntries = new HashMap<>();

	public void save(final File storeFile) throws Exception {
		if (storeFile == null) {
			throw new Exception("UnsecureDataStore file is undefined");
		} else {
			try (FileOutputStream outputStream = new FileOutputStream(storeFile)) {
				try (JsonWriter jsonWriter = new JsonWriter(outputStream)) {
					jsonWriter.openJsonArray();
					for (final Entry<String, Map<String, Object>> entryMap : dataEntries.entrySet()) {
						for (final Entry<String, Object> entry : entryMap.getValue().entrySet()) {
							final JsonObject entryObject = new JsonObject();
							entryObject.add("class", entryMap.getKey());
							entryObject.add("name", entry.getKey());
							entryObject.add("value", JsonSerializer.serialize(entry.getValue(), false, false, false, true).getValue());
							jsonWriter.add(entryObject);
						}
					}
					jsonWriter.closeJsonArray();
				}
			}
		}
	}

	public void load(final File storeFile) throws Exception {
		if (storeFile == null) {
			throw new Exception("UnsecureDataStore file is undefined");
		} else if (!storeFile.exists()) {
			throw new Exception("UnsecureDataStore file does not exist: " + storeFile.getAbsolutePath());
		} else {
			dataEntries = new HashMap<>();
			try (FileInputStream inputStream = new FileInputStream(storeFile)) {
				try (JsonReader jsonReader = new JsonReader(inputStream)) {
					final JsonToken jsonToken = jsonReader.readNextToken();
					if (jsonToken != JsonToken.JsonArray_Open) {
						throw new Exception("UnsecureDataStore is corrupt");
					} else {
						JsonNode nextJsonNode;
						while ((nextJsonNode = jsonReader.readNextJsonNode()) != null) {
							if (nextJsonNode.isJsonObject()) {
								final JsonObject entryObject = (JsonObject) nextJsonNode.getValue();
								final String className = (String) entryObject.get("class");
								if (!dataEntries.containsKey(className)) {
									dataEntries.put(className, new HashMap<>());
								}
								final String entryName = (String) entryObject.get("name");
								dataEntries.get(className).put(entryName, JsonSerializer.deserialize((JsonObject) entryObject.get("value")));
							} else {
								throw new Exception("Unexpected JSON data in UnsecureDataStore: " + nextJsonNode.getJsonDataType().getName());
							}
						}
					}
				}
			}
		}
	}

	public Set<String> getEntryNames(final Class<?> classType) {
		if (dataEntries.get(classType.getName()) == null) {
			return new HashSet<>();
		} else {
			return dataEntries.get(classType.getName()).keySet();
		}
	}

	public <T> T getEntry(final Class<T> classType, final String entryName) {
		if (dataEntries.get(classType.getName()) == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		final T returnValue = (T) dataEntries.get(classType.getName()).get(entryName);
		return returnValue;
	}

	public Object getEntry(final String entryName) {
		for (final Entry<String, Map<String, Object>> classEntry : dataEntries.entrySet()) {
			for (final Entry<String, Object> entry : classEntry.getValue().entrySet()) {
				if (StringUtilities.equals(entryName, entry.getKey())) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	public void addEntry(final String entryName, final Object entryValue) {
		if (dataEntries.get(entryValue.getClass().getName()) == null) {
			dataEntries.put(entryValue.getClass().getName(), new HashMap<>());
		}
		dataEntries.get(entryValue.getClass().getName()).put(entryName, entryValue);
	}

	public void removeEntry(final Class<?> classType, final String entryName) {
		dataEntries.get(classType.getName()).remove(entryName);
	}

	public void removeEntriesByEntryName(final String entryName) {
		for (final Entry<String, Map<String, Object>> classEntry : dataEntries.entrySet()) {
			classEntry.getValue().remove(entryName);
		}
	}

	public void removeEntriesByEntryClass(final Class<?> classType) {
		dataEntries.remove(classType.getName());
	}
}
