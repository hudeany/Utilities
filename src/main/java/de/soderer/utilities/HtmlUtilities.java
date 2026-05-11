package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtilities {
	public static List<List<String>> parseHtmlTable(final String htmlData) {
		final List<List<String>> entries = new ArrayList<>();

		final Matcher rowMatcher = Pattern.compile("<tr>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(htmlData);

		while (rowMatcher.find()) {
			final String row = rowMatcher.group(1);

			final List<String> tableRowData = new ArrayList<>();
			final Matcher cellMatcher = Pattern.compile("<td>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(row);
			while (cellMatcher.find()) {
				tableRowData.add(stripTags(cellMatcher.group(1)).trim());
			}

			entries.add(tableRowData);
		}

		return entries;
	}

	private static String stripTags(final String html) {
		return html.replaceAll("<[^>]+>", "");
	}
}
