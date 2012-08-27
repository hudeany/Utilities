package de.soderer.utilities;

import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

public class LangResources {
	private static PropertyResourceBundle propertyResourceBundle;
	
	public static String get(String resourceKey, Object... arguments) {
		if (propertyResourceBundle == null) {
			try {
				propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle("LanguageProperties");
			} catch (Exception e) {
				e.printStackTrace();
				return "Missing resourceBundle: " + resourceKey;
			}
		}
	
		if (propertyResourceBundle.containsKey(resourceKey)) {
			String pattern = propertyResourceBundle.getString(resourceKey);
			return MessageFormat.format(pattern, arguments);
		} else {
			return "Missing resourceKey: " + resourceKey;
		}
	}
}
