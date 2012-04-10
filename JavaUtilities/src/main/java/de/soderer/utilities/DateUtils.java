package de.soderer.utilities;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;

public class DateUtils {
	public static SimpleDateFormat DD_MM_YYYY_HH_MM_SS = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
	public static SimpleDateFormat DD_MM_YYYY_HH_MM_SS_Z = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss z");
	public static SimpleDateFormat DD_MM_YYYY_HH_MM = new SimpleDateFormat("dd.MM.yyyy HH:mm");
	public static SimpleDateFormat DDMMYYYY = new SimpleDateFormat("ddMMyyyy");
	public static SimpleDateFormat YYYYMMDD = new SimpleDateFormat("yyyyMMdd");
	public static SimpleDateFormat HHMMSS = new SimpleDateFormat("HHmmss");
	public static SimpleDateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
	public static SimpleDateFormat EE_DD_MM_YYYY = new SimpleDateFormat("EE dd.MM.yyyy", Locale.GERMAN); // EE => Wochentag
	public static SimpleDateFormat DD_MM_YYYY_HH_MM_SS_ForFileName = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
	public static SimpleDateFormat YYYYMMDDHHMMSS = new SimpleDateFormat("yyyyMMddHHmmss");
	public static SimpleDateFormat YYYY_MM_DD_HHMMSS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static SimpleDateFormat YYYYMMDD_HHMMSS = new SimpleDateFormat("yyyyMMdd-HHmmss");
	public static SimpleDateFormat DD_MMM_YYYY_ENG = new SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH);
	public static SimpleDateFormat DD_MMM_YYYY_GER = new SimpleDateFormat("dd-MMM-yy", Locale.GERMAN);

	public static String getWochenTagNamensKuerzel(GregorianCalendar datum) {
		int tagesInt = datum.get(GregorianCalendar.DAY_OF_WEEK);
		switch (tagesInt) {
			case GregorianCalendar.SUNDAY: return "So"; // in GregorianCalendar ist die 1 der Sonntag
			case GregorianCalendar.MONDAY: return "Mo";
			case GregorianCalendar.TUESDAY: return "Di";
			case GregorianCalendar.WEDNESDAY: return "Mi";
			case GregorianCalendar.THURSDAY: return "Do";
			case GregorianCalendar.FRIDAY: return "Fr";
			case GregorianCalendar.SATURDAY: return "Sa";
			default: return "unbekannt"; // kann gem. Definition nicht vorkommen
		}
	}
	
	/**
	 * Formatiert den Timestamp (Achtung Nanos im .Net Format)
	 * 
	 * @param ts ein Timestamp
	 * @return ein String im Format 'yyyy-mm-dd HH:MM:SS.NNNNNN'
	 */
	public static String formatTimestamp_yyyyMMdd_HHmmssNNNNNN(Timestamp ts) {
		String returnString = "";

		if (ts != null) {
			String s = YYYY_MM_DD_HHMMSS.format(ts);
			String nanosString = Integer.toString(ts.getNanos());

			if (nanosString.length() > 6)
				nanosString = nanosString.substring(0, 6);
			else {
				while (nanosString.length() < 6) {
					nanosString += "0";
				}
			}

			returnString = s + nanosString;
		}

		return returnString;
	}
	
	/**
	 * Kovertiert ein Datum der Form "dd.MM.yyyy" oder "dd-MM-yyyy" in "yyyy-MM-dd"
	 * 
	 * @param ddMMyyyyString
	 * @return
	 */
	public static String convert_ddMMyyyy_to_yyyyMMdd(String ddMMyyyyString) {
		return ddMMyyyyString.substring(6, 10) + "-" + ddMMyyyyString.substring(3, 5) + "-"
				+ ddMMyyyyString.substring(0, 2);
	}

	/**
	 * Kovertiert ein Datum der Form "yyyy-MM-dd" oder "yyyy.MM.dd" in "dd.MM.yyyy"
	 * 
	 * @param ddMMyyyyString
	 * @return
	 */
	public static String convert_yyyyMMdd_to_ddMMyyyy(String yyyyMMddString) {
		return yyyyMMddString.substring(8, 10) + "." + yyyyMMddString.substring(5, 7) + "."
				+ yyyyMMddString.substring(0, 4);
	}
}
