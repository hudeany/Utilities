package de.soderer.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtils {

	public static String toString(Throwable t) {
		StringWriter stringWriter = new StringWriter();
		PrintWriter printWriter = new PrintWriter(stringWriter);
		printWriter.println(t.getClass().getName());
		printWriter.println(t.getMessage());
		printWriter.println(ExceptionUtils.getStackTrace(t));
		return stringWriter.toString();
	}

	public static String getStackTrace(Throwable t) {
		StringWriter stringWriter = new StringWriter();
		t.printStackTrace(new PrintWriter(stringWriter));
		return stringWriter.toString();
	}

}
