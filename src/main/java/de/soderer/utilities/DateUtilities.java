package de.soderer.utilities;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.mail.internet.InternetAddress;

public class DateUtilities {
	public static final String DD_MM_YYYY_HH_MM_SS = "dd.MM.yyyy HH:mm:ss";
	public static final String DD_MM_YYYY_HH_MM_SS_Z = "dd.MM.yyyy HH:mm:ss z";
	public static final String DD_MM_YYYY_HH_MM = "dd.MM.yyyy HH:mm";
	public static final String DD_MM_YYYY = "dd.MM.yyyy";
	public static final String DDMMYYYY = "ddMMyyyy";
	public static final String YYYYMMDD = "yyyyMMdd";
	public static final String HHMMSS = "HHmmss";
	public static final String YYYY_MM_DD = "yyyy-MM-dd";
	public static final String YYYY_MM_DD_HH_MM = "yyyy-MM-dd HH:mm";
	public static final String DD_MM_YYYY_HH_MM_SS_ForFileName = "dd_MM_yyyy_HH_mm_ss";
	public static final String YYYYMMDDHHMMSS = "yyyyMMddHHmmss";
	public static final String YYYYMMDDHHMMSSSSS = "yyyyMMddHHmmssSSS";
	public static final String YYYY_MM_DD_HHMMSS = "yyyy-MM-dd HH:mm:ss";
	public static final String YYYYMMDD_HHMMSS = "yyyyMMdd-HHmmss";
	public static final String HHMM = "HHmm";

	private static final Pattern MONTH_RULE_PATTERN = Pattern.compile("\\d{0,2}M\\d{2}:\\d{4}");
	private static final Pattern WEEKDAILY_RULE_PATTERN = Pattern.compile("\\d\\D\\D:\\d{4}");

	/** Date format for SOAP Webservices (ISO 8601) */
	public static final String ISO_8601_DATE_FORMAT_NO_TIMEZONE = "yyyy-MM-dd";
	/** Date format for SOAP Webservices (ISO 8601) */
	public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-ddX";
	/** DateTime format for SOAP Webservices (ISO 8601) */
	public static final String ISO_8601_DATETIME_FORMAT_NO_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss";
	/** DateTime format for SOAP Webservices (ISO 8601) */
	public static final String ISO_8601_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
	/** DateTime format for SOAP Webservices (ISO 8601) */
	public static final String ISO_8601_DATETIME_FORMAT_WITH_MILLIS_NO_TIMEZONE = "yyyy-MM-dd'T'HH:mm:ss.SSS";
	/** DateTime format for SOAP Webservices (ISO 8601) */
	public static final String ISO_8601_DATETIME_FORMAT_WITH_MILLIS = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

	/** ANSI SQL standard date time format */
	public static final String ANSI_SQL_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

	/** ANSI SQL standard date format */
	public static final String ANSI_SQL_DATE_FORMAT = "yyyy-MM-dd";

	public static String getWeekdayNameShort(final GregorianCalendar date) {
		final int dayInt = date.get(Calendar.DAY_OF_WEEK);
		final String dayString = DateFormatSymbols.getInstance().getWeekdays()[dayInt];
		return dayString.substring(0, 2);
	}

	public static int getWeekdayIndex(String weekday) {
		if (Utilities.isBlank(weekday)) {
			return -1;
		} else {
			weekday = weekday.toLowerCase().trim();
			final String[] localeWeekdays = DateFormatSymbols.getInstance().getWeekdays();
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
	 * Format a timestamp (Nanos in .Net format)
	 *
	 * @param ts
	 *            a Timestamp
	 * @return a String in format 'yyyy-mm-dd HH:MM:SS.NNNNNN'
	 */
	public static String formatTimestamp_yyyyMMdd_HHmmssNNNNNN(final Timestamp ts) {
		String returnString = "";

		if (ts != null) {
			final String s = DateTimeFormatter.ofPattern(YYYY_MM_DD_HHMMSS).format(DateUtilities.getLocalDateForDate(ts));
			String nanosString = Integer.toString(ts.getNanos());

			if (nanosString.length() > 6) {
				nanosString = nanosString.substring(0, 6);
			} else {
				while (nanosString.length() < 6) {
					nanosString += "0";
				}
			}

			returnString = s + nanosString;
		}

		return returnString;
	}

	/**
	 * Format a timestampString from format "dd.MM.yyyy" or "dd-MM-yyyy" to "yyyy-MM-dd"
	 *
	 * @param ddMMyyyyString
	 * @return
	 */
	public static String convert_ddMMyyyy_to_yyyyMMdd(final String ddMMyyyyString) {
		return ddMMyyyyString.substring(6, 10) + "-" + ddMMyyyyString.substring(3, 5) + "-" + ddMMyyyyString.substring(0, 2);
	}

	/**
	 * Format a timestampString from format "yyyy-MM-dd" or "yyyy.MM.dd" to "dd.MM.yyyy"
	 *
	 * @param ddMMyyyyString
	 * @return
	 */
	public static String convert_yyyyMMdd_to_ddMMyyyy(final String yyyyMMddString) {
		return yyyyMMddString.substring(8, 10) + "." + yyyyMMddString.substring(5, 7) + "." + yyyyMMddString.substring(0, 4);
	}

	public static Date getDay(final int daysToAdd) {
		final GregorianCalendar returnValue = new GregorianCalendar();
		returnValue.set(Calendar.HOUR_OF_DAY, 0);
		returnValue.set(Calendar.MINUTE, 0);
		returnValue.set(Calendar.SECOND, 0);
		returnValue.set(Calendar.MILLISECOND, 0);
		if (daysToAdd != 0) {
			returnValue.add(Calendar.DAY_OF_MONTH, daysToAdd);
		}
		return returnValue.getTime();
	}

	public static String replaceDatePatternInString(final String stringWithPatter, final Date date) {
		final LocalDateTime localDateTime = DateUtilities.getLocalDateTimeForDate(date);
		String returnString = stringWithPatter;
		returnString = returnString.replace("[yyyy]", String.format("%04d", localDateTime.getYear()));
		returnString = returnString.replace("[YYYY]", String.format("%04d", localDateTime.getYear()));
		returnString = returnString.replace("[MM]", String.format("%02d", localDateTime.getMonthValue()));
		returnString = returnString.replace("[dd]", String.format("%02d", localDateTime.getDayOfMonth()));
		returnString = returnString.replace("[DD]", String.format("%02d", localDateTime.getDayOfMonth()));
		returnString = returnString.replace("[HH]", String.format("%02d", localDateTime.getHour()));
		returnString = returnString.replace("[hh]", String.format("%02d", localDateTime.getHour()));
		returnString = returnString.replace("[mm]", String.format("%02d", localDateTime.getMinute()));
		returnString = returnString.replace("[SS]", String.format("%02d", localDateTime.getSecond()));
		returnString = returnString.replace("[ss]", String.format("%02d", localDateTime.getSecond()));
		returnString = returnString.replace("\\[", "[");
		returnString = returnString.replace("\\]", "]");
		return returnString;
	}

	public static Date calculateETA(final Date start, final long itemsToDo, final long itemsDone) {
		if (start == null || itemsToDo <= 0 || itemsDone <= 0) {
			return null;
		} else {
			final Date now = new Date();
			final long millisStartToNow = now.getTime() - start.getTime();
			final long millisStartToEnd = itemsToDo * millisStartToNow / itemsDone;
			return new Date(start.getTime() + millisStartToEnd);
		}
	}

	public static String getShortHumanReadableTimespan(final long valueInMillis, final boolean showMillis, final boolean showLeadingZeros) {
		final StringBuilder returnValue = new StringBuilder();
		long rest = valueInMillis;

		final long millis = rest % 1000;
		rest = rest / 1000;

		final long seconds = rest % 60;
		rest = rest / 60;

		final long minutes = rest % 60;
		rest = rest / 60;

		final long hours = rest % 24;
		rest = rest / 24;

		final long days = rest % 7;
		rest = rest / 7;

		final long weeks = rest % 52;
		rest = rest / 52;

		final long years = rest;

		if (millis != 0 && showMillis) {
			returnValue.insert(0, "ms");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%03d", millis));
			} else {
				returnValue.insert(0, millis);
			}
		}

		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "s");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", seconds));
			} else {
				returnValue.insert(0, seconds);
			}
		}

		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "m");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", minutes));
			} else {
				returnValue.insert(0, minutes);
			}
		}

		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "h");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", hours));
			} else {
				returnValue.insert(0, hours);
			}
		}

		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "d");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", days));
			} else {
				returnValue.insert(0, days);
			}
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "w");
			if (showLeadingZeros) {
				returnValue.insert(0, String.format("%02d", weeks));
			} else {
				returnValue.insert(0, weeks);
			}
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, "y");
			returnValue.insert(0, years);
		}

		if (returnValue.length() > 0) {
			return returnValue.toString();
		} else if (!showMillis) {
			if (showLeadingZeros) {
				return "00s";
			} else {
				return "0s";
			}
		} else {
			if (showLeadingZeros) {
				return "000ms";
			} else {
				return "0ms";
			}
		}
	}

	public static String getHumanReadableTimespan(final long valueInMillis, final boolean showMillis) {
		final StringBuilder returnValue = new StringBuilder();
		long rest = valueInMillis;

		final long millis = rest % 1000;
		rest = rest / 1000;

		final long seconds = rest % 60;
		rest = rest / 60;

		final long minutes = rest % 60;
		rest = rest / 60;

		final long hours = rest % 24;
		rest = rest / 24;

		final long days = rest % 7;
		rest = rest / 7;

		final long weeks = rest % 52;
		rest = rest / 52;

		final long years = rest;

		if (millis != 0 && showMillis) {
			returnValue.insert(0, " " + LangResources.get("millis"));
			returnValue.insert(0, millis);
		}

		if (seconds != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("seconds"));
			returnValue.insert(0, seconds);
		}

		if (minutes != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("minutes"));
			returnValue.insert(0, minutes);
		}

		if (hours != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("hours"));
			returnValue.insert(0, hours);
		}

		if (days != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("days"));
			returnValue.insert(0, days);
		}

		if (weeks != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("weeks"));
			returnValue.insert(0, weeks);
		}

		if (years != 0) {
			if (returnValue.length() > 0) {
				returnValue.insert(0, " ");
			}
			returnValue.insert(0, " " + LangResources.get("years"));
			returnValue.insert(0, years);
		}

		if (returnValue.length() > 0) {
			return returnValue.toString();
		} else if (!showMillis) {
			return "0 " + LangResources.get("seconds");
		} else {
			return "0 " + LangResources.get("millis");
		}
	}

	/**
	 * Get the duration between two timestamps as a string
	 *
	 * @param startTime
	 * @param endTime
	 * @return
	 */
	public static String getDuration(final Calendar startTime, final Calendar endTime) {
		final int durationInMilliSeconds = (int) (endTime.getTimeInMillis() - startTime.getTimeInMillis());
		final int milliSecondsPart = durationInMilliSeconds % 1000;
		final int secondsPart = durationInMilliSeconds / 1000 % 60;
		final int minutesPart = durationInMilliSeconds / 1000 / 60 % 60;
		final int hoursPart = durationInMilliSeconds / 1000 / 60 / 60 % 24;
		final int days = durationInMilliSeconds / 1000 / 60 / 60 % 24;

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

	public static Date calculateNextJobStart(final String timingString) throws Exception {
		final GregorianCalendar now = new GregorianCalendar();
		return calculateNextJobStart(now, timingString, TimeZone.getDefault());
	}

	public static Date calculateNextJobStart(final String timingString, final String timeZone) throws Exception {
		final GregorianCalendar now = new GregorianCalendar();
		TimeZone zone;
		if (!Utilities.isEmpty(timeZone)) {
			zone = TimeZone.getTimeZone(timeZone);
		} else {
			zone = TimeZone.getDefault();
		}
		return calculateNextJobStart(now, timingString, zone);
	}

	/**
	 * Calculation of next scheduled job start
	 * Timingparameter may contain weekdays, clocktimes, months, quarters and holidays
	 *
	 * Allowed parameters:
	 * "ONCE"                      => only once (returns null)
	 * "0600;0800"                 => daily at 06:00 and 08:00
	 * "MoMi:1700"                 => Every monday and wednesday at 17:00
	 * "M05:1600"                  => every 05th day of month at 16:00
	 * "Q:1600"                    => every first day of quarter at 16:00
	 * "QW:1600"                   => every first working day of quarter at 16:00
	 * "MoDiMiDoFr:1700;!23012011" => mondays to fridays at 17:00 exept for 23.01.2011 (Holidays marked by '!')
	 *
	 * All values may be combined separated by semicolons.
	 *
	 * @param timingString
	 * @return
	 * @throws Exception
	 */
	public static Date calculateNextJobStart(final GregorianCalendar now, final String timingString, final TimeZone timeZone) throws Exception {
		if (Utilities.isBlank(timingString) || "once".equalsIgnoreCase(timingString)) {
			return null;
		}

		GregorianCalendar returnStart = null;

		// Holidays to exclude
		final List<GregorianCalendar> excludedDays = new ArrayList<>();

		final String[] timingParameterList = timingString.split(";|,| ");
		for (final String timingParameter : timingParameterList) {
			if (timingParameter.startsWith("!")) {
				try {
					final GregorianCalendar exclusionDate = new GregorianCalendar(timeZone);
					exclusionDate.setTime(parseDate("ddMMyyyy", timingParameter.substring(1), timeZone));
					excludedDays.add(exclusionDate);
				} catch (final DateTimeParseException e) {
					throw e;
				}
			}
		}

		for (final String timingParameter : timingParameterList) {
			final GregorianCalendar nextStartByThisParameter = new GregorianCalendar(timeZone);
			nextStartByThisParameter.setTime(now.getTime());
			// Make "week of year" ISO-8601 compliant
			makeWeekOfYearISO8601Compliant(nextStartByThisParameter);

			if (timingParameter.startsWith("!")) {
				// Exclusions are done previously
				continue;
			} else if (!timingParameter.contains(":")) {
				if (NumberUtilities.isDigit(timingParameter)) {
					if (timingParameter.length() == 4) {
						// daily execution on given time
						nextStartByThisParameter.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timingParameter.substring(0, 2)));
						nextStartByThisParameter.set(Calendar.MINUTE, Integer.parseInt(timingParameter.substring(2)));
						nextStartByThisParameter.set(Calendar.SECOND, 0);
						nextStartByThisParameter.set(Calendar.MILLISECOND, 0);

						// Move next start into future (+1 day) until rule is matched
						// Move also when meeting holiday rule
						while (!nextStartByThisParameter.after(now) && (returnStart == null || nextStartByThisParameter.before(returnStart))
								|| dayListIncludes(excludedDays, nextStartByThisParameter)) {
							nextStartByThisParameter.add(Calendar.DAY_OF_MONTH, 1);
						}
					} else if (timingParameter.length() == 8) {
						// execution on given day
						try {
							nextStartByThisParameter.setTime(parseDate("ddMMyyyy", timingParameter, timeZone));
						} catch (final DateTimeParseException e) {
							throw new Exception("Invalid interval description", e);
						}

						if (dayListIncludes(excludedDays, nextStartByThisParameter)) {
							continue;
						}
					}
				} else if (timingParameter.contains("*") && timingParameter.length() == 4) {
					// daily execution on given time with wildcards '*' like '*4*5'
					nextStartByThisParameter.set(Calendar.SECOND, 0);
					nextStartByThisParameter.set(Calendar.MILLISECOND, 0);

					// Move next start into future (+1 minute) until rule is matched
					// Move also when meeting holiday rule
					while (!nextStartByThisParameter.after(now) && (returnStart == null || nextStartByThisParameter.before(returnStart))
							|| dayListIncludes(excludedDays, nextStartByThisParameter)
							|| !checkTimeMatchesPattern(timingParameter, nextStartByThisParameter.getTime())) {
						nextStartByThisParameter.add(Calendar.MINUTE, 1);
					}
				} else {
					// Fr: weekly execution on Friday at 00:00 Uhr
					boolean onlyWithinOddWeeks = false;
					boolean onlyWithinEvenWeeks = false;
					final List<Integer> weekdayIndexes = new ArrayList<>();
					for (final String weekDay : TextUtilities.chopToChunks(timingParameter, 2)) {
						if ("ev".equalsIgnoreCase(weekDay)) {
							onlyWithinEvenWeeks = true;
						} else if ("od".equalsIgnoreCase(weekDay)) {
							onlyWithinOddWeeks = true;
						} else {
							weekdayIndexes.add(getWeekdayIndex(weekDay));
						}
					}
					nextStartByThisParameter.set(Calendar.HOUR_OF_DAY,0);
					nextStartByThisParameter.set(Calendar.MINUTE, 0);
					nextStartByThisParameter.set(Calendar.SECOND, 0);
					nextStartByThisParameter.set(Calendar.MILLISECOND, 0);

					// Move next start into future (+1 day) until rule is matched
					// Move also when meeting holiday rule
					while ((!nextStartByThisParameter.after(now)
							|| !weekdayIndexes.contains(nextStartByThisParameter.get(Calendar.DAY_OF_WEEK))) && (returnStart == null || nextStartByThisParameter.before(returnStart))
							|| dayListIncludes(excludedDays, nextStartByThisParameter)
							|| (onlyWithinOddWeeks && (nextStartByThisParameter.get(Calendar.WEEK_OF_YEAR) % 2 == 0))
							|| (onlyWithinEvenWeeks && (nextStartByThisParameter.get(Calendar.WEEK_OF_YEAR) % 2 != 0))) {
						nextStartByThisParameter.add(Calendar.DAY_OF_MONTH, 1);
					}
				}
			} else if (MONTH_RULE_PATTERN.matcher(timingParameter).matches()) {
				// month rule "M99:1700" (every month at ultimo)
				// month rule "06M01:1700" (every half a year at months first day)
				String xMonth = timingParameter.substring(0, timingParameter.indexOf("M"));
				if (xMonth.length() == 0) {
					xMonth = "1";
				}
				final String day = timingParameter.substring(timingParameter.indexOf("M") + 1, timingParameter.indexOf(":"));
				final String time = timingParameter.substring(timingParameter.indexOf(":") + 1);

				nextStartByThisParameter.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
				nextStartByThisParameter.set(Calendar.MINUTE, Integer.parseInt(time.substring(2)));
				nextStartByThisParameter.set(Calendar.SECOND, 0);
				nextStartByThisParameter.set(Calendar.MILLISECOND, 0);

				if ("99".equals(day)) {
					// special day ultimo
					nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, nextStartByThisParameter.getActualMaximum(Calendar.DAY_OF_MONTH));
					// ensure that the first estimated "next time" is in the past, before making forward steps
					if (nextStartByThisParameter.after(now)) {
						nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, 1);
						nextStartByThisParameter.add(Calendar.MONTH, -1);
						nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, nextStartByThisParameter.getActualMaximum(Calendar.DAY_OF_MONTH));
					}
				} else {
					nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
					// ensure that the first estimated "next time" is in the past, before making forward steps
					if (nextStartByThisParameter.after(now)) {
						nextStartByThisParameter.add(Calendar.MONTH, -1);
					}
				}

				// Make forward step
				if ("99".equals(day)) {
					// special day ultimo
					nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, 1);
					nextStartByThisParameter.add(Calendar.MONTH, Integer.parseInt(xMonth));
					nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, nextStartByThisParameter.getActualMaximum(Calendar.DAY_OF_MONTH));
				} else {
					nextStartByThisParameter.add(Calendar.MONTH, Integer.parseInt(xMonth));
				}

				// Move also when meeting holiday rule
				while (dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(Calendar.DAY_OF_YEAR, 1);
				}
			} else if (timingParameter.startsWith("Q:")) {
				// quarterly execution (Q:1200) at first day of month
				if (nextStartByThisParameter.get(Calendar.MONTH) < Calendar.APRIL) {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.APRIL);
				} else if (nextStartByThisParameter.get(Calendar.MONTH) < Calendar.JULY) {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.JULY);
				} else if (nextStartByThisParameter.get(Calendar.MONTH) < Calendar.OCTOBER) {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.OCTOBER);
				} else {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.JANUARY);
					nextStartByThisParameter.add(Calendar.YEAR, 1);
				}

				nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, 1);
				final String time = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
				nextStartByThisParameter.set(Calendar.MINUTE, Integer.parseInt(time.substring(2)));
				nextStartByThisParameter.set(Calendar.SECOND, 0);
				nextStartByThisParameter.set(Calendar.MILLISECOND, 0);

				// Move also when meeting holiday rule
				while (dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(Calendar.DAY_OF_YEAR, 1);
				}
			} else if (timingParameter.startsWith("QW:")) {
				// quarterly execution (QW:1200) at first workingday of month
				if (nextStartByThisParameter.get(Calendar.MONTH) < Calendar.APRIL) {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.APRIL);
				} else if (nextStartByThisParameter.get(Calendar.MONTH) < Calendar.JULY) {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.JULY);
				} else if (nextStartByThisParameter.get(Calendar.MONTH) < Calendar.OCTOBER) {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.OCTOBER);
				} else {
					nextStartByThisParameter.set(Calendar.MONTH, Calendar.JANUARY);
					nextStartByThisParameter.add(Calendar.YEAR, 1);
				}

				nextStartByThisParameter.set(Calendar.DAY_OF_MONTH, 1);

				// Move also when meeting holiday rule
				while (nextStartByThisParameter.get(Calendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY
						|| nextStartByThisParameter.get(Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY
						|| dayListIncludes(excludedDays, nextStartByThisParameter)) {
					nextStartByThisParameter.add(Calendar.DAY_OF_MONTH, 1);
				}

				final String time = timingParameter.substring(timingParameter.indexOf(":") + 1);
				nextStartByThisParameter.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
				nextStartByThisParameter.set(Calendar.MINUTE, Integer.parseInt(time.substring(2)));
				nextStartByThisParameter.set(Calendar.SECOND, 0);
				nextStartByThisParameter.set(Calendar.MILLISECOND, 0);
			} else if (WEEKDAILY_RULE_PATTERN.matcher(timingParameter).matches()) {
				// every xth of a weekday in a month
				final int weekDayOrder = Integer.parseInt(timingParameter.substring(0, 1));
				if (weekDayOrder < 1 || 5 < weekDayOrder) {
					throw new Exception("Invalid interval description");
				}
				final String weekDaySign = timingParameter.substring(1, 3);
				final String time = timingParameter.substring(timingParameter.indexOf(":") + 1);
				final int weekdayIndex = getWeekdayIndex(weekDaySign);
				nextStartByThisParameter.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
				nextStartByThisParameter.set(Calendar.MINUTE, Integer.parseInt(time.substring(2)));
				nextStartByThisParameter.set(Calendar.SECOND, 0);
				nextStartByThisParameter.set(Calendar.MILLISECOND, 0);

				// Move next start into future (+1 day) until rule is matched
				// Move also when meeting holiday rule
				while ((!nextStartByThisParameter.after(now)
						|| weekdayIndex != nextStartByThisParameter.get(Calendar.DAY_OF_WEEK)) && (returnStart == null || nextStartByThisParameter.before(returnStart))
						|| dayListIncludes(excludedDays, nextStartByThisParameter)
						|| weekDayOrder != getNumberOfWeekdayInMonth(nextStartByThisParameter.get(Calendar.DAY_OF_MONTH))) {
					nextStartByThisParameter.add(Calendar.DAY_OF_MONTH, 1);
				}
			} else {
				// weekday execution (also allows workingday execution, german: "Werktagssteuerung" by "MoTuWeThFr:0000")
				final String weekDays = timingParameter.substring(0, timingParameter.indexOf(":"));
				boolean onlyWithinOddWeeks = false;
				boolean onlyWithinEvenWeeks = false;
				final String time = timingParameter.substring(timingParameter.indexOf(":") + 1);
				final List<Integer> weekdayIndexes = new ArrayList<>();
				for (final String weekDay : TextUtilities.chopToChunks(weekDays, 2)) {
					if ("ev".equalsIgnoreCase(weekDay)) {
						onlyWithinEvenWeeks = true;
					} else if ("od".equalsIgnoreCase(weekDay)) {
						onlyWithinOddWeeks = true;
					} else {
						final int weekdayIndex = getWeekdayIndex(weekDay);
						if (weekdayIndex < 0) {
							throw new Exception("Invalid weekday in timing data: " + timingString);
						}
						weekdayIndexes.add(weekdayIndex);
					}
				}
				if (weekdayIndexes.isEmpty()) {
					throw new Exception("Invalid timing data: " + timingString);
				}
				nextStartByThisParameter.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
				nextStartByThisParameter.set(Calendar.MINUTE, Integer.parseInt(time.substring(2)));
				nextStartByThisParameter.set(Calendar.SECOND, 0);
				nextStartByThisParameter.set(Calendar.MILLISECOND, 0);

				// Move next start into future (+1 day) until rule is matched
				// Move also when meeting holiday rule
				while ((!nextStartByThisParameter.after(now)
						|| !weekdayIndexes.contains(nextStartByThisParameter.get(Calendar.DAY_OF_WEEK))) && (returnStart == null || nextStartByThisParameter.before(returnStart))
						|| dayListIncludes(excludedDays, nextStartByThisParameter)
						|| (onlyWithinOddWeeks && (nextStartByThisParameter.get(Calendar.WEEK_OF_YEAR) % 2 == 0))
						|| (onlyWithinEvenWeeks && (nextStartByThisParameter.get(Calendar.WEEK_OF_YEAR) % 2 != 0))) {
					nextStartByThisParameter.add(Calendar.DAY_OF_MONTH, 1);
				}
			}

			if (returnStart == null || nextStartByThisParameter.before(returnStart)) {
				returnStart = nextStartByThisParameter;
			}
		}

		if (returnStart == null) {
			throw new Exception("Invalid interval description");
		}

		return returnStart.getTime();
	}

	/**
	 * Make a calendars "week of year" ISO-8601 compliant
	 */
	public static Calendar makeWeekOfYearISO8601Compliant(final Calendar calendar) {
		calendar.setMinimalDaysInFirstWeek(4);
		calendar.setFirstDayOfWeek(Calendar.MONDAY);
		return calendar;
	}

	public static boolean checkTimeMatchesPattern(final String pattern, final Date time) {
		final Pattern timePattern = Pattern.compile(pattern.replace("*", "."));
		final String timeString = formatDate(HHMM, time);
		return timePattern.matcher(timeString).matches();
	}

	/**
	 * Remove the time part of a GregorianCalendar
	 *
	 * @param value
	 * @return
	 */
	public static GregorianCalendar getDayWithoutTime(final GregorianCalendar value) {
		return new GregorianCalendar(value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DAY_OF_MONTH));
	}

	/**
	 * Check if a day is included in a list of days
	 *
	 * @param listOfDays
	 * @param day
	 * @return
	 */
	public static boolean dayListIncludes(final List<GregorianCalendar> listOfDays, final GregorianCalendar day) {
		for (final GregorianCalendar listDay : listOfDays) {
			if (listDay.get(Calendar.DAY_OF_YEAR) == day.get(Calendar.DAY_OF_YEAR)) {
				return true;
			}
		}
		return false;
	}

	public static Date parseUnknownDateFormat(final String value) throws Exception {
		try {
			return parseDate(DD_MM_YYYY_HH_MM_SS, value);
		} catch (@SuppressWarnings("unused") final DateTimeParseException e1) {
			try {
				return parseDate(DD_MM_YYYY_HH_MM, value);
			} catch (@SuppressWarnings("unused") final DateTimeParseException e2) {
				try {
					return parseDate(DD_MM_YYYY, value);
				} catch (@SuppressWarnings("unused") final DateTimeParseException e3) {
					try {
						return parseDate(YYYY_MM_DD_HH_MM, value);
					} catch (@SuppressWarnings("unused") final DateTimeParseException e4) {
						try {
							return parseDate(YYYYMMDDHHMMSS, value);
						} catch (@SuppressWarnings("unused") final DateTimeParseException e5) {
							try {
								return parseDate(DDMMYYYY, value);
							} catch (@SuppressWarnings("unused") final DateTimeParseException e6) {
								throw new Exception("Unknown date format");
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("deprecation")
	public static LocalDateTime getLocalDateTimeForDate(final Date date) {
		if (date == null) {
			return null;
		} else {
			try {
				final long milliseconds = date.getTime();
				final long epochSeconds = milliseconds / 1000;
				final int nanoseconds = ((int) (milliseconds % 1000)) * 1000000;
				final LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(epochSeconds, nanoseconds, ZoneOffset.ofTotalSeconds(date.getTimezoneOffset() * -60));
				return localDateTime;
			} catch (final Exception e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	public static LocalDate getLocalDateForDate(final Date date) {
		// new Date(date.getTime()) to convert value of java.sql.Date
		return (new Date(date.getTime())).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	public static Date getDateForLocalDateTime(final LocalDateTime localDateTime) {
		return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
	}

	public static Date getDateForZonedDateTime(final ZonedDateTime zonedDateTime) {
		return Date.from(zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * Parse DateTime strings for ISO 8601
	 *
	 * @param dateValue
	 * @return
	 * @throws Exception
	 */
	public static ZonedDateTime parseIso8601DateTimeString(final String dateValue) throws Exception {
		return parseIso8601DateTimeString(dateValue, ZoneId.systemDefault());
	}

	/**
	 * Parse DateTime strings for ISO 8601
	 *
	 * @param dateValue
	 * @return
	 * @throws Exception
	 */
	public static ZonedDateTime parseIso8601DateTimeString(String dateValue, final ZoneId defaultZoneId) throws Exception {
		if (Utilities.isBlank(dateValue)) {
			return null;
		}

		dateValue = dateValue.toUpperCase();

		if (dateValue.endsWith("Z")) {
			// Standardize UTC time
			dateValue = dateValue.replace("Z", "+00:00");
		}

		boolean hasTimezone = false;
		if (dateValue.length() > 6 && dateValue.charAt(dateValue.length() - 3) == ':' && (dateValue.charAt(dateValue.length() - 6) == '+' || dateValue.charAt(dateValue.length() - 6) == '-')) {
			hasTimezone = true;
		} else if (dateValue.length() > 6 && (dateValue.charAt(dateValue.length() - 3) == '+')) {
			hasTimezone = true;
		}

		if (dateValue.contains("T")) {
			if (dateValue.contains(".")) {
				if (hasTimezone) {
					if (dateValue.substring(dateValue.indexOf(".")).length() > 10 ) {
						// Date with time and fractals
						final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSSXXXXX");
						dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
						return ZonedDateTime.parse(dateValue, dateTimeFormatter);
					} else {
						// Date with time and milliseconds
						final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXXXX");
						dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
						return ZonedDateTime.parse(dateValue, dateTimeFormatter);
					}
				} else {
					if (dateValue.substring(dateValue.indexOf(".")).length() > 4 ) {
						// Date with time and fractals
						final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS");
						return LocalDateTime.parse(dateValue, dateTimeFormatter).atZone(defaultZoneId);
					} else {
						// Date with time and milliseconds
						final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS");
						dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
						return LocalDateTime.parse(dateValue, dateTimeFormatter).atZone(defaultZoneId);
					}
				}
			} else {
				// Date with time
				if (hasTimezone) {
					final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
					dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
					return ZonedDateTime.parse(dateValue, dateTimeFormatter);
				} else {
					final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
					dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
					return LocalDateTime.parse(dateValue, dateTimeFormatter).atZone(defaultZoneId);
				}
			}
		} else {
			// Date only
			if (hasTimezone) {
				dateValue = dateValue.replace("+", "T00:00:00+");
				final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
				dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
				return ZonedDateTime.parse(dateValue, dateTimeFormatter);
			} else {
				dateValue = dateValue + "T00:00:00";
				final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
				dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
				return LocalDateTime.parse(dateValue, dateTimeFormatter).atZone(defaultZoneId);
			}
		}
	}

	/**
	 * Get the ordinal of occurence of the given weekdy in its month
	 * @param dayOfMonth
	 * @return
	 */
	public static int getNumberOfWeekdayInMonth(final int dayOfMonth) {
		final float ordinalFloat = dayOfMonth / 7.0f;
		final int ordinalInt = (int) Math.round(Math.ceil(ordinalFloat));
		return ordinalInt;
	}

	public static Date changeDateTimeZone(final Date date, TimeZone sourceTimeZone, TimeZone destinationTimeZone) {
		if (date == null) {
			return null;
		} else {
			if (sourceTimeZone == null) {
				sourceTimeZone = TimeZone.getDefault();
			}
			if (destinationTimeZone == null) {
				destinationTimeZone = TimeZone.getDefault();
			}
			if (sourceTimeZone.equals(destinationTimeZone)) {
				return date;
			} else {
				long fromTZDst = 0;
				if (sourceTimeZone.inDaylightTime(date)) {
					fromTZDst = sourceTimeZone.getDSTSavings();
				}

				final long fromTZOffset = sourceTimeZone.getRawOffset() + fromTZDst;

				long toTZDst = 0;
				if (destinationTimeZone.inDaylightTime(date)) {
					toTZDst = destinationTimeZone.getDSTSavings();
				}
				final long toTZOffset = destinationTimeZone.getRawOffset() + toTZDst;

				return new Date(date.getTime() + (toTZOffset - fromTZOffset));
			}
		}
	}

	public static Date changeDateTimeZone(final Date date, ZoneId sourceZoneId, ZoneId destinationZoneId) {
		if (date == null) {
			return null;
		} else {
			if (sourceZoneId == null) {
				sourceZoneId = ZoneId.systemDefault();
			}
			if (destinationZoneId == null) {
				destinationZoneId = ZoneId.systemDefault();
			}
			if (sourceZoneId.equals(destinationZoneId)) {
				return date;
			} else {
				final LocalDateTime localDateTime = getLocalDateTimeForDate(date);
				final ZonedDateTime sourceZonedDateTime = localDateTime.atZone(sourceZoneId);
				final ZonedDateTime destinationZonedDateTime = sourceZonedDateTime.withZoneSameInstant(destinationZoneId);
				final Date rezonedDate = new Date(destinationZonedDateTime.withZoneSameLocal(ZoneId.systemDefault()).toInstant().toEpochMilli());
				return rezonedDate;
			}
		}
	}

	public static DateTimeFormatter getDateFormatter(final Locale locale) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateFormatPattern(locale));
		dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		return dateTimeFormatter;
	}

	public static DateTimeFormatter getDateFormatter(final Locale locale, final ZoneId zoneId) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateFormatPattern(locale));
		dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		dateTimeFormatter.withZone(zoneId);
		return dateTimeFormatter;
	}

	public static String getDateFormatPattern(final Locale locale) {
		final SimpleDateFormat dateTimeFormat = (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT, locale);
		return dateTimeFormat.toPattern().replaceFirst("y+", "yyyy");
	}

	public static String getDateTimeFormatPattern(final Locale locale) {
		return getDateFormatPattern(locale) + " HH:mm";
	}

	public static String getDateTimeFormatWithSecondsPattern(final Locale locale) {
		return getDateFormatPattern(locale) + " HH:mm:ss";
	}

	public static DateTimeFormatter getDateTimeFormatter(final Locale locale) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatPattern(locale));
		dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		return dateTimeFormatter;
	}

	public static DateTimeFormatter getDateTimeFormatter(final Locale locale, final ZoneId zoneId) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatPattern(locale));
		dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		dateTimeFormatter.withZone(zoneId);
		return dateTimeFormatter;
	}

	public static DateTimeFormatter getDateTimeFormatterWithSeconds(final Locale locale) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatWithSecondsPattern(locale));
		dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		return dateTimeFormatter;
	}

	public static DateTimeFormatter getDateTimeFormatterWithSeconds(final Locale locale, final ZoneId zoneId) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(getDateTimeFormatPattern(locale));
		dateTimeFormatter.withResolverStyle(ResolverStyle.STRICT);
		dateTimeFormatter.withZone(zoneId);
		return dateTimeFormatter;
	}

	public static String formatDate(final String format, final Date date) {
		if (date == null) {
			return null;
		} else {
			return DateTimeFormatter.ofPattern(format).format(getLocalDateTimeForDate(date));
		}
	}

	public static Date parseDate(final String format, final String dateString) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		final LocalDateTime localDateTime = LocalDateTime.parse(dateString, dateTimeFormatter);
		return getDateForLocalDateTime(localDateTime);
	}

	public static Date parseDate(final String format, final String dateString, final TimeZone timeZone) {
		final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
		dateTimeFormatter.withZone(timeZone.toZoneId());
		final LocalDateTime localDateTime = LocalDateTime.parse(dateString, dateTimeFormatter);
		return getDateForLocalDateTime(localDateTime);
	}

	public static Date addDaysToDate(final Date initDate, final int daysToAdd) {
		final GregorianCalendar returnDate = new GregorianCalendar();
		returnDate.setTime(initDate);
		returnDate.add(Calendar.DAY_OF_MONTH, daysToAdd);
		return returnDate.getTime();
	}

	public static Date addMinutesToDate(final Date initDate, final int minutesToAdd) {
		final GregorianCalendar returnDate = new GregorianCalendar();
		returnDate.setTime(initDate);
		returnDate.add(Calendar.MINUTE, minutesToAdd);
		return returnDate.getTime();
	}

	/**
	 * OpenJDK 15 doesn't recognize german three letter months by "MMM" in SimpleDateFormat anymore.
	 * So here is a helper to cope with that problem.
	 */
	public static int parseThreeLetterMonth(final String threeLetterMonth) throws Exception {
		switch(threeLetterMonth.toUpperCase()) {
			case "JAN":
				return 1;
			case "FEB":
				return 2;
			case "MAR":
			case "MÄR":
				return 3;
			case "APR":
				return 4;
			case "MAY":
			case "MAI":
				return 5;
			case "JUN":
				return 6;
			case "JUL":
				return 7;
			case "AUG":
				return 8;
			case "SEP":
				return 9;
			case "OCT":
			case "OKT":
				return 10;
			case "NOV":
				return 11;
			case "DEC":
			case "DEZ":
				return 12;
			default:
				throw new Exception("Unknown three letter month: " + threeLetterMonth);
		}
	}

	public static String createICalEventInvitation(final String shortname, final String description, final ZonedDateTime startDateTime, final ZonedDateTime endDateTime, final String location, final InternetAddress organizer, final List<InternetAddress> participants) {
		String returnValue =
				"BEGIN:VCALENDAR\n";
		returnValue += "METHOD:REQUEST" + "\n";
		returnValue += "VERSION:2.0" + "\n";

		returnValue += "BEGIN:VEVENT" + "\n";

		returnValue += "DTSTAMP;VALUE=DATE-TIME:" + DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(ZonedDateTime.now().withZoneSameInstant(ZoneOffset.UTC)) + "\n";
		returnValue += "UID:" + UUID.randomUUID().toString().toUpperCase() + "\n";

		returnValue += "DTSTART;VALUE=DATE-TIME:" + DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(startDateTime.withZoneSameInstant(ZoneOffset.UTC)) + "\n";

		if (endDateTime != null) {
			returnValue += "DTEND;VALUE=DATE-TIME:" + DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").format(endDateTime.withZoneSameInstant(ZoneOffset.UTC)) + "\n";
		}

		if (location != null) {
			returnValue += "LOCATION;VALUE=TEXT:" + location + "\n";
		}

		returnValue += "SUMMARY;VALUE=TEXT:" + shortname + "\n";

		if (description != null) {
			returnValue += "DESCRIPTION;VALUE=TEXT:" + description + "\n";
		}

		if (organizer != null) {
			if (organizer.getPersonal() != null) {
				returnValue += "ORGANIZER;CN=" + organizer.getPersonal() + ":MAILTO:" + organizer.getAddress() + "\n";
			} else {
				returnValue += "ORGANIZER:MAILTO:" + organizer.getAddress() + "\n";
			}
		}

		if (participants != null) {
			for (final InternetAddress participant : participants) {
				if (participant.getPersonal() != null) {
					returnValue += "ATTENDEE;ROLE=REQ-PARTICIPANT;CN=" + participant.getPersonal() + ":MAILTO:" + participant.getAddress() + "\n";
				} else {
					returnValue += "ATTENDEE;ROLE=REQ-PARTICIPANT:MAILTO:" + participant.getAddress() + "\n";
				}
			}
		}

		returnValue += "END:VEVENT" + "\n";

		returnValue += "END:VCALENDAR\n";

		return returnValue;
	}
}
