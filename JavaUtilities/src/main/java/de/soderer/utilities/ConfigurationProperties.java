package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.LookupTranslator;

import de.soderer.utilities.collection.UniqueFifoQueuedList;

public class ConfigurationProperties {
	public static String MAXSIZE_EXTENSION = ".maxSize";
	
	private Properties properties = new Properties();
	private String applicationName;
	
	public ConfigurationProperties(String applicationName) {
		FileInputStream fileInputStream = null;
		try {
			this.applicationName = applicationName;
			fileInputStream = new FileInputStream(System.getProperty("user.home") + File.separator + "." + applicationName + ".config");
			Properties propertiesLoaded = new Properties();
		    propertiesLoaded.load(fileInputStream);
		    for (Entry<Object, Object> entry : propertiesLoaded.entrySet()) {
		    	properties.setProperty((String) entry.getKey(), unEscape((String) entry.getValue()));
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fileInputStream);
		}
	}
	
	public String get(String name) {
		if (properties.containsKey(name))
			return properties.getProperty(name);
		else return null;
	}

	/**
	 * Ensure this key is enclosed in the configuration within the saved file
	 * 
	 * @param name
	 */
	public void ensure(String name) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, "");
		}
	}

	/**
	 * Ensure this key is enclosed in the configuration within the saved file
	 * 
	 * @param name
	 */
	public void ensure(String name, boolean defaultValue) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, defaultValue ? "true" : "false");
		}
	}
	
	public void ensureRecentList(String name, int maxSize) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, "");
		}
		if (!properties.containsKey(name + MAXSIZE_EXTENSION)) {
			properties.setProperty(name + MAXSIZE_EXTENSION, Integer.toString(maxSize));
		}
	}
	
	public void set(String name, String value) {
		properties.setProperty(name, value);
	}
	
	public void set(String name, int value) {
		properties.setProperty(name, Integer.toString(value));
	}
	
	public int getInteger(String name) {
		if (properties.containsKey(name)) {
			return Integer.parseInt(properties.getProperty(name));
		} else {
			return 0;
		}
	}
	
	public boolean getBoolean(String name) {
		if (properties.containsKey(name)) {
			return Utilities.interpretAsBool(properties.getProperty(name));
		} else {
			return false;
		}
	}
	
	public void set(String name, boolean value) {
		properties.setProperty(name, value ? "true" : "false");
	}
	
	public List<String> getList(String name) {
		if (properties.containsKey(name) && StringUtils.isNotEmpty(properties.getProperty(name)) ) {
			try {
				return CsvReader.parseCsvLine(';', '"', properties.getProperty(name));
			} catch (IOException e) {
				return new ArrayList<String>();
			}
		} else {
			return new ArrayList<String>();
		}
	}
	
	public boolean removeFromList(String name, String value) {
		if (properties.containsKey(name) && StringUtils.isNotEmpty(properties.getProperty(name)) ) {
			try {
				List<String> currentList = CsvReader.parseCsvLine(';', '"', properties.getProperty(name));
				boolean removedValue = currentList.remove(value);
				set(name, currentList);
				return removedValue;
			} catch (IOException e) {
				return false;
			}
		} else {
			return false;
		}
	}
	
	public void set(String name, List<String> list) {
		properties.setProperty(name, CsvWriter.getCsvLine(';', '"', list));
	}
	
	public void addRecentListEntry(String name, String value) {
		List<String> valueList = new UniqueFifoQueuedList<String>(getInteger(name + MAXSIZE_EXTENSION), getList(name));
		valueList.add(value);
		set(name, valueList);
	}
	
	public void addListEntry(String name, String value) {
		List<String> valueList = getList(name);
		valueList.add(value);
		set(name, valueList);
	}
	
	public void addSetEntry(String name, String value) {
		Set<String> valueSet = new HashSet<String>(getList(name));
		valueSet.add(value);
		set(name, new ArrayList<String>(valueSet));
	}
	
	public String getLatestRecentValue(String name) {
		return new UniqueFifoQueuedList<String>(getInteger(name + MAXSIZE_EXTENSION), getList(name)).getLatestAdded();
	}
	
	public void save() {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(System.getProperty("user.home") + File.separator + "." + applicationName + ".config");
			Properties propertiesToStore = new Properties();
			for (Entry<Object, Object> entry : properties.entrySet()) {
				propertiesToStore.setProperty((String) entry.getKey(), escape((String) entry.getValue()));
			}
			propertiesToStore.store(fileOutputStream, applicationName + ".config");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(fileOutputStream);
		}
	}
	
	public List<String> getKeyList() {
		List<String> returnList = new ArrayList<String>();
		for (Object key : properties.keySet()) {
			returnList.add(key.toString());
		}
		return returnList;
	}
	
	public boolean containsKey(String name) {
		return properties.containsKey(name);
	}
	
	public static String escape(String value) {
		AggregateTranslator escapeScript = new AggregateTranslator(
			new LookupTranslator(new String[][] { { "\\", "\\\\" }, { "\r", "\\r" }, { "\n", "\\n" }, { "\t", "\\t" } })
		);
		
		return escapeScript.translate(value);
	}
	
	public static String unEscape(String value) {
		AggregateTranslator unescapeScript = new AggregateTranslator(
			new LookupTranslator(new String[][] { { "\\\\", "\\" }, { "\\r", "\r" }, { "\\n", "\n" }, { "\\t", "\t" } })
		);
		
		return unescapeScript.translate(value);
	}
}
