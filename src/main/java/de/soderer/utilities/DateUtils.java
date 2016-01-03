package de.soderer.utilities;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
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
	public static SimpleDateFormat MMM_D_HHMM = new SimpleDateFormat("MMM d HH:mm", Locale.ENGLISH);
	public static SimpleDateFormat MMM_D_YYYY = new SimpleDateFormat("MMM d yyyy", Locale.ENGLISH);

	public static String getWochenTagNamensKuerzel(GregorianCalendar datum) {
		int tagesInt = datum.get(GregorianCalendar.DAY_OF_WEEK);
		String dayString = DateFormatSymbols.getInstance().getWeekdays()[tagesInt];
		return dayString.substring(0, 2);
	}
	
	public static int getWeekdayIndex(String weekday) {
		if (Utilities.isBlank(weekday)) {
			return -1;
		} else {
			weekday = weekday.toLowerCase().trim();
			String[] localeWeekdays = DateFormatSymbols.getInstance().getWeekdays();
			for (int i = 0; i < localeWeekdays.length; i++) {
				if (localeWeekdays[i].toLowerCase().startsWith(weekday)) {
					return i;
				}
			}
			
			if (weekday.startsWith("so") || weekday.startsWith("su")) {
				return Calendar.SUNDAY;
			} else if (weekday.startsWith("mo")) {
				return Calendar.MONDAY;
			} else if (weekday.startsWith("di") || weekday.startsWith("tu")) {
				return Calendar.TUESDAY;
			} else if (weekday.startsWith("mi") || weekday.startsWith("we")) {
				return Calendar.WEDNESDAY;
			} else if (weekday.startsWith("do") || weekday.startsWith("th")) {
				return Calendar.THURSDAY;
			} else if (weekday.startsWith("fr")) {
				return Calendar.FRIDAY;
			} else if (weekday.startsWith("sa")) {
				return Calendar.SATURDAY;
			} else {
				return -1;
			}
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
	
	public static Date getDay(int daysToAdd) {
		GregorianCalendar returnValue = new GregorianCalendar();
		returnValue.set(Calendar.HOUR_OF_DAY, 0);
		returnValue.set(Calendar.MINUTE, 0);
		returnValue.set(Calendar.SECOND, 0);
		returnValue.set(Calendar.MILLISECOND, 0);
		if (daysToAdd != 0) {
			returnValue.add(Calendar.DAY_OF_MONTH, daysToAdd);
		}
		return returnValue.getTime();
	}
	
	public static String replaceDatePatternInString(String stringWithPatter, Date date) {
		String returnString = stringWithPatter;
		returnString = returnString.replace("yyyy", new SimpleDateFormat("yyyy").format(date));
		returnString = returnString.replace("YYYY", new SimpleDateFormat("yyyy").format(date));
		returnString = returnString.replace("MM", new SimpleDateFormat("MM").format(date));
		returnString = returnString.replace("dd", new SimpleDateFormat("dd").format(date));
		returnString = returnString.replace("DD", new SimpleDateFormat("dd").format(date));
		returnString = returnString.replace("HH", new SimpleDateFormat("HH").format(date));
		returnString = returnString.replace("hh", new SimpleDateFormat("HH").format(date));
		returnString = returnString.replace("mm", new SimpleDateFormat("mm").format(date));
		returnString = returnString.replace("SS", new SimpleDateFormat("ss").format(date));
		returnString = returnString.replace("ss", new SimpleDateFormat("ss").format(date));
		return returnString;
	}

	public static Date calculateETA(Date start, long itemsToDo, long itemsDone) {
		if (start == null || itemsToDo <= 0 || itemsDone <= 0) {
			return null;
		} else {
			Date now = new Date();
			long millisStartToNow = now.getTime() - start.getTime();
			long millisStartToEnd = itemsToDo * millisStartToNow / itemsDone;
			return new Date(start.getTime() + millisStartToEnd);
		}
	}
	
	public static String getShortHumanReadableTimespan(long valueInMillis, boolean showMillis) {
		String returnValue = "";
		long rest = valueInMillis;
		
		long millis = rest % 1000;
		rest = rest / 1000;
		
		long seconds = rest % 60;
		rest = rest / 60;
		
		long minutes = rest % 60;
		rest = rest / 60;
		
		long hours = rest % 24;
		rest = rest / 24;
		
		long days = rest % 7;
		rest = rest / 7;
		
		long weeks = rest % 52;
		rest = rest / 52;
		
		long years = rest;
		
		if (millis != 0 && showMillis) {
			returnValue = millis + "ms";
		}
		
		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = seconds + "s" + returnValue;
		}
		
		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = minutes + "m" + returnValue;
		}
		
		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = hours + "h" + returnValue;
		}
		
		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = days + "d" + returnValue;
		}
		
		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = weeks + "w" + returnValue;
		}
		
		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = years + "y" + returnValue;
		}
		
		if (returnValue.length() > 0) {
			return returnValue;
		} else if (!showMillis) {
			return "0s";
		} else {
			return "0ms";
		}
	}
	
	public static String getHumanReadableTimespan(long valueInMillis, boolean showMillis) {
		String returnValue = "";
		long rest = valueInMillis;
		
		long millis = rest % 1000;
		rest = rest / 1000;
		
		long seconds = rest % 60;
		rest = rest / 60;
		
		long minutes = rest % 60;
		rest = rest / 60;
		
		long hours = rest % 24;
		rest = rest / 24;
		
		long days = rest % 7;
		rest = rest / 7;
		
		long weeks = rest % 52;
		rest = rest / 52;
		
		long years = rest;
		
		if (millis != 0 && showMillis) {
			returnValue = millis + " millis";
		}
		
		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = seconds + " seconds" + returnValue;
		}
		
		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = minutes + " minutes" + returnValue;
		}
		
		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = hours + " hours" + returnValue;
		}
		
		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = days + " days" + returnValue;
		}
		
		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = weeks + " weeks" + returnValue;
		}
		
		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue = " " + returnValue;
			}
			returnValue = years + " years" + returnValue;
		}
		
		if (returnValue.length() > 0) {
			return returnValue;
		} else if (!showMillis) {
			return "0s";
		} else {
			return "0ms";
		}
	}
}
