package de.soderer.utilities;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class LangResources {
	private static PropertyResourceBundle propertyResourceBundle;
	
	public static String get(String resourceKey) {
		if (propertyResourceBundle == null) {
			try {
				propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle("LanguageProperties");
			} catch (Exception e) {
				e.printStackTrace();
				return "Missing resourceBundle: " + resourceKey;
			}
		}
	
		if (propertyResourceBundle.containsKey(resourceKey))
			return propertyResourceBundle.getString(resourceKey);
		else
			return "Missing resourceKey: " + resourceKey;
	}
}
