package de.soderer.utilities;

import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

public class LangResources {
	private static PropertyResourceBundle propertyResourceBundle;
	
	public static String get(String resourceKey, Object... arguments) {
		if (propertyResourceBundle == null) {
			try {
				propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle("LanguageProperties");
			} catch (Exception e) {
				e.printStackTrace();
				return "Missing resourceBundle: " + resourceKey + (arguments != null && arguments.length > 0 ? " " + StringUtils.join(arguments, ", ") : "");
			}
		}
	
		if (propertyResourceBundle.containsKey(resourceKey)) {
			String pattern = propertyResourceBundle.getString(resourceKey);
			return MessageFormat.format(pattern, arguments);
		} else {
			return "Missing resourceKey: " + resourceKey + (arguments != null && arguments.length > 0 ? " " + StringUtils.join(arguments, ", ") : "");
		}
	}
	
	public static boolean existsKey(String resourceKey) {
		return propertyResourceBundle.containsKey(resourceKey);
	}
}
