package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class ApplicationConfiguration {
	private Properties properties = new Properties();
	private String applicationName;
	
	public ApplicationConfiguration(String applicationName) {
		FileInputStream fileInputStream = null;
		try {
			this.applicationName = applicationName;
			fileInputStream = new FileInputStream(System.getProperty("user.home") + File.separator + "." + applicationName + ".config");
		    properties.load(fileInputStream);
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
	
	public void set(String name, String value) {
		properties.setProperty(name, value);
	}
	
	public boolean getBoolean(String name) {
		if (properties.containsKey(name))
			return Utilities.interpretAsBool(properties.getProperty(name));
		else return false;
	}
	
	public void set(String name, boolean value) {
		properties.setProperty(name, value ? "true" : "false");
	}
	
	public List<String> getList(String name) {
		if (properties.containsKey(name))
			return Arrays.asList(properties.getProperty(name).split(";"));
		else return new ArrayList<String>();
	}
	
	public void set(String name, List<String> list) {
		properties.setProperty(name, StringUtils.join(list, ";"));
	}
	
	public void save() {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(System.getProperty("user.home") + File.separator + "." + applicationName + ".config");
			properties.store(fileOutputStream, applicationName + ".config");
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
}
