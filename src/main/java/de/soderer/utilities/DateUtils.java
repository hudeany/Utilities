package de.soderer.utilities;

import java.sql.Timestamp;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
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

	/**
	 * Get the duration between two timestamps as a string
	 * 
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public static String getDuration(Calendar startTime, Calendar endTime) {
		int durationInMilliSeconds = (int) (endTime.getTimeInMillis() - startTime.getTimeInMillis());
		int milliSecondsPart = durationInMilliSeconds % 1000;
		int secondsPart = durationInMilliSeconds / 1000 % 60;
		int minutesPart = durationInMilliSeconds / 1000 / 60 % 60;
		int hoursPart = durationInMilliSeconds / 1000 / 60 / 60 % 24;
		int days = durationInMilliSeconds / 1000 / 60 / 60 % 24;

		String returnString = milliSecondsPart + "ms";
		if (secondsPart > 0) {
			returnString = secondsPart + "s " + returnString;
		}
		if (minutesPart > 0) {
			returnString = minutesPart + "m " + returnString;
		}
		if (hoursPart > 0) {
			returnString = hoursPart + "h " + returnString;
		}
		if (days > 0) {
			returnString = days + "d " + returnString;
		}
		return returnString;
	}

	/**
	 * Ermitteln des nächsten geplanten Startzeitpunktes Der Timingparameter kann Wochentage, Zeiten, Monats-, Quartalsplanungen und Feiertage enthalten
	 * 
	 * Mögliche Ausprägungen: "ONCE" => nur einmalig (returns null) "0600;0800" => täglich um 06:00 und um 08:00 "MoMi:1700" => Montags und Mittwochs um 17:00 "M05:1600" => jeden 05.ten im Monat um
	 * 16:00 "Q:1600" => Jeden ersten Tag im Quartal um 16:00 "QW:1600" => Jeden ersten Werktag im Quartal um 16:00 "MoDiMiDoFr:1700;!23012011" => Mo bis Fr um 17:00 aber nicht am 23.01.2011
	 * (Feiertagsregelung durch '!'-Zeichen)
	 * 
	 * Alle Ausprägungen können beliebig kombiniert werden, werden dann durch Semikolons getrennt aufgelistet
	 * 
	 * @param timingString
	 * @return
	 * @throws Exception
	 */
	public static Timestamp calculateNextJobStart(String timingString) {
		GregorianCalendar now = new GregorianCalendar();
		GregorianCalendar returnStart = new GregorianCalendar();
		returnStart.add(GregorianCalendar.YEAR, 1);

		// Feiertage die explizit ausgeschlossen werden
		List<GregorianCalendar> excludedDays = new ArrayList<GregorianCalendar>();

		if (timingString.equalsIgnoreCase("once")) {
			return null;
		}

		String[] timingParameterList = timingString.split(";");
		for (String timingParameter : timingParameterList) {
			if (timingParameter.startsWith("!")) {
				try {
					GregorianCalendar exclusionDate = new GregorianCalendar();
					exclusionDate.setTime(DateUtils.DDMMYYYY.parse(timingParameter.substring(1)));
					excludedDays.add(exclusionDate);
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}

		for (String timingParameter : timingParameterList) {
			GregorianCalendar nextStartByThisParameter = new GregorianCalendar();
			nextStartByThisParameter.setTime(now.getTime());

			if (timingParameter.startsWith("!")) {
				// Exclusionen wurden bereits verarbeitet
				continue;
			} else if (!timingParameter.contains(":")) {
				if (Utilities.isNumber(timingParameter)) {
					// tägliche Verarbeitung zur gegebenen Uhrzeit
					nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(timingParameter.substring(0, 2)));
					nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(timingParameter.substring(2)));
					nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
					nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

					// nächsten Start solange in die Zukunft verschieben (+1Tag)
					// bis
					// Parameter-Bedingung erfüllt ist
					// Bei treffender Feiertagseintragung auch um einen Tag
					// weiterschieben
					while (nextStartByThisParameter.before(now) && nextStartByThisParameter.before(returnStart) || dayListIncludes(excludedDays, nextStartByThisParameter)) {
						nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
					}
				} else {
					// wochentägliche Verarbeitung um 00:00 Uhr
					List<Integer> weekdayIndexes = new ArrayList<Integer>();
					for (String weekDay : TextUtilities.chopToChunks(timingParameter, 2)) {
						weekdayIndexes.add(DateUtils.getWeekdayIndex(weekDay));
					}
					nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, 0);
					nextStartByThisParameter.set(GregorianCalendar.MINUTE, 0);
					nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
					nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

					// nächsten Start solange in die Zukunft verschieben (+1Tag)
					// bis
					// Parameter-Bedingung erfüllt ist
					// Bei treffender Feiertagseintragung auch um einen Tag
					// weiterschieben
					while ((nextStartByThisParameter.before(now) || !weekdayIndexes.contains(nextStartByThisParameter.get(Calendar.DAY_OF_WEEK))) && nextStartByThisParameter.before(returnStart)
							|| dayListIncludes(excludedDays, nextStartByThisParameter)) {
						nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
					}
				}
			} else if (timingParameter.length() == 8) {
				// Monatssteuerung "M01:1700"
				String tag = timingParameter.substring(1, timingParameter.indexOf(":"));
				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(GregorianCalendar.DAY_OF_MONTH, Integer.parseInt(tag));
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

				// Den nächsten passenden Monat finden
				while (nextStartByThisParameter.before(now) && nextStartByThisParameter.before(returnStart)) {
					nextStartByThisParameter.add(GregorianCalendar.MONTH, 1);
				}

				// Bei treffender Feiertagseintragung um einen Tag
				// weiterschieben
				while (dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_YEAR, 1);
				}
			} else if (timingParameter.startsWith("Q:")) {
				// Quartalsweise Planung (Q:1200): Start am ersten Kalendertag
				// im Quartal
				if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.APRIL) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.APRIL);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.JULY) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JULY);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.OCTOBER) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.OCTOBER);
				} else {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JANUARY);
					nextStartByThisParameter.add(GregorianCalendar.YEAR, 1);
				}

				nextStartByThisParameter.set(GregorianCalendar.DAY_OF_MONTH, 1);
				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

				// Bei treffender Feiertagseintragung um einen Tag
				// weiterschieben
				while (dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_YEAR, 1);
				}
			} else if (timingParameter.startsWith("QW:")) {
				// Quartalsweise Planung (QW:1200): Start am ersten Werktag im
				// Quartal
				if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.APRIL) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.APRIL);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.JULY) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JULY);
				} else if (nextStartByThisParameter.get(GregorianCalendar.MONTH) < GregorianCalendar.OCTOBER) {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.OCTOBER);
				} else {
					nextStartByThisParameter.set(GregorianCalendar.MONTH, GregorianCalendar.JANUARY);
					nextStartByThisParameter.add(GregorianCalendar.YEAR, 1);
				}

				nextStartByThisParameter.set(GregorianCalendar.DAY_OF_MONTH, 1);

				// Bei treffender Feiertagseintragung oder Wochentagsregelung um
				// einen Tag weiterschieben
				while (nextStartByThisParameter.get(GregorianCalendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY
						|| nextStartByThisParameter.get(GregorianCalendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY || dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
				}

				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);
			} else {
				// WochenTagessteuerung (ermöglicht auch Werktagssteuerung)
				String wochenTage = timingParameter.substring(0, timingParameter.indexOf(":"));
				String zeit = timingParameter.substring(timingParameter.indexOf(":") + 1);
				List<Integer> weekdayIndexes = new ArrayList<Integer>();
				for (String weekDay : TextUtilities.chopToChunks(wochenTage, 2)) {
					weekdayIndexes.add(DateUtils.getWeekdayIndex(weekDay));
				}
				nextStartByThisParameter.set(GregorianCalendar.HOUR_OF_DAY, Integer.parseInt(zeit.substring(0, 2)));
				nextStartByThisParameter.set(GregorianCalendar.MINUTE, Integer.parseInt(zeit.substring(2)));
				nextStartByThisParameter.set(GregorianCalendar.SECOND, 0);
				nextStartByThisParameter.set(GregorianCalendar.MILLISECOND, 0);

				// nächsten Start solange in die Zukunft verschieben (+1Tag) bis
				// Parameter-Bedingung erfüllt ist
				// Bei treffender Feiertagseintragung auch um einen Tag
				// weiterschieben
				while ((nextStartByThisParameter.before(now) || !weekdayIndexes.contains(nextStartByThisParameter.get(Calendar.DAY_OF_WEEK))) && nextStartByThisParameter.before(returnStart)
						|| dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(GregorianCalendar.DAY_OF_MONTH, 1);
				}
			}

			if (nextStartByThisParameter.before(returnStart)) {
				returnStart = nextStartByThisParameter;
			}
		}

		return new Timestamp(returnStart.getTimeInMillis());
	}

	/**
	 * Remove the time part of a GregorianCalendar
	 * 
	 * @param value
	 * @return
	 */
	public static GregorianCalendar getDayWithoutTime(GregorianCalendar value) {
		return new GregorianCalendar(value.get(GregorianCalendar.YEAR), value.get(GregorianCalendar.MONTH), value.get(GregorianCalendar.DAY_OF_MONTH));
	}

	/**
	 * Check if a day is included in a list of days
	 * 
	 * @param listOfDays
	 * @param day
	 * @return
	 */
	public static boolean dayListIncludes(List<GregorianCalendar> listOfDays, GregorianCalendar day) {
		for (GregorianCalendar listDay : listOfDays) {
			if (listDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
				return true;
			}
		}
		return false;
	}
}
