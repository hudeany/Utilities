package de.soderer.utilities;

public class HtmlUtilities {
	public static String htmlEncodeString(String value) {
        return value
        	.replaceAll("�", "&Auml;")
        	.replaceAll("�", "&auml;")
        	.replaceAll("�", "&Ouml;")
        	.replaceAll("�", "&ouml;")
        	.replaceAll("�", "&Uuml;")
        	.replaceAll("�", "&uuml;")
        	.replaceAll("�", "&slig;");
    }
}
