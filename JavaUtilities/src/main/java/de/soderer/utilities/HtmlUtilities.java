package de.soderer.utilities;

public class HtmlUtilities {
	public static String htmlEncodeString(String value) {
        return value
        	.replaceAll("Ä", "&Auml;")
        	.replaceAll("ä", "&auml;")
        	.replaceAll("Ö", "&Ouml;")
        	.replaceAll("ö", "&ouml;")
        	.replaceAll("Ü", "&Uuml;")
        	.replaceAll("ü", "&uuml;")
        	.replaceAll("ß", "&slig;");
    }
}
