package de.soderer.utilities.pac.utilities;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TimeZone;

public class PacScriptMethods {
	private final static String GMT = "GMT";
	private final static List<String> WEEKDAYS_SHORT = Collections
			.unmodifiableList(Arrays.asList("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"));
	private final static List<String> MONTH_SHORT = Collections.unmodifiableList(
			Arrays.asList("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"));

	public static boolean isPlainHostName(final String host) {
		return !host.contains(".");
	}

	public static boolean dnsDomainIs(final String host, final String domain) {
		return host.endsWith(domain);
	}

	public static boolean localHostOrDomainIs(final String host, final String domain) {
		return domain.startsWith(host);
	}

	public static boolean isResolvable(final String host) {
		try {
			InetAddress.getByName(host).getHostAddress();
			return true;
		} catch (@SuppressWarnings("unused") final UnknownHostException e) {
			return false;
		}
	}

	public static boolean isInNet(String host, final String pattern, final String mask) {
		try {
			host = dnsResolve(host);
			if (host == null || host.length() == 0) {
				return false;
			} else {
				final long lhost = parseIpAddressToLong(host);
				final long lpattern = parseIpAddressToLong(pattern);
				final long lmask = parseIpAddressToLong(mask);
				return (lhost & lmask) == (lpattern & lmask);
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}

	public static String dnsResolve(final String host) {
		try {
			return InetAddress.getByName(host).getHostAddress();
		} catch (@SuppressWarnings("unused") final UnknownHostException e) {
			return "";
		}
	}

	public static String myIpAddress() {
		return getLocalAddressOfType(Inet4Address.class);
	}

	public static int dnsDomainLevels(final String host) {
		int count = 0;
		int startPos = 0;
		while ((startPos = host.indexOf(".", startPos + 1)) > -1) {
			count++;
		}
		return count;
	}

	public static boolean shExpMatch(final String str, final String shexp) {
		final StringTokenizer tokenizer = new StringTokenizer(shexp, "*");
		int startPos = 0;
		while (tokenizer.hasMoreTokens()) {
			final String token = tokenizer.nextToken();
			final int temp = str.indexOf(token, startPos);

			if (startPos == 0 && !shexp.startsWith("*") && temp != 0) {
				return false;
			} else if (!tokenizer.hasMoreTokens() && !shexp.endsWith("*") && !str.endsWith(token)) {
				return false;
			} else if (temp == -1) {
				return false;
			} else {
				startPos = temp + token.length();
			}
		}
		return true;
	}

	public static boolean weekdayRange(final String weekdayStart, final String weekdayEnd, final String gmt) {
		final boolean useGmt = GMT.equalsIgnoreCase(gmt);
		final Calendar cal = getCurrentTime(useGmt);

		final int currentDay = cal.get(Calendar.DAY_OF_WEEK) - 1;
		final int from = indexOfCaseInsensitive(WEEKDAYS_SHORT, weekdayStart);
		int to = indexOfCaseInsensitive(WEEKDAYS_SHORT, weekdayEnd);
		if (to == -1) {
			to = from;
		}

		if (to < from) {
			return currentDay >= from || currentDay <= to;
		} else {
			return currentDay >= from && currentDay <= to;
		}
	}

	public static boolean dateRange(final Object dayStart, final Object monthStart, final Object yearStart,
			final Object dayEnd, final Object monthEnd, final Object yearEnd, final Object gmt) {
		final Map<String, Integer> params = new HashMap<>();
		parseDateParam(params, dayStart);
		parseDateParam(params, monthStart);
		parseDateParam(params, yearStart);
		parseDateParam(params, dayEnd);
		parseDateParam(params, monthEnd);
		parseDateParam(params, yearEnd);
		parseDateParam(params, gmt);

		final boolean useGmt = params.get("gmt") != null;
		final Calendar cal = getCurrentTime(useGmt);
		final Date current = cal.getTime();

		if (params.get("day1") != null) {
			cal.set(Calendar.DAY_OF_MONTH, params.get("day1"));
		}
		if (params.get("month1") != null) {
			cal.set(Calendar.MONTH, params.get("month1"));
		}
		if (params.get("year1") != null) {
			cal.set(Calendar.YEAR, params.get("year1"));
		}
		final Date from = cal.getTime();

		Date to;
		if (params.get("day2") != null) {
			cal.set(Calendar.DAY_OF_MONTH, params.get("day2"));
		}
		if (params.get("month2") != null) {
			cal.set(Calendar.MONTH, params.get("month2"));
		}
		if (params.get("year2") != null) {
			cal.set(Calendar.YEAR, params.get("year2"));
		}
		to = cal.getTime();

		if (to.before(from)) {
			cal.add(Calendar.MONTH, +1);
			to = cal.getTime();
		}

		if (to.before(from)) {
			cal.add(Calendar.YEAR, +1);
			cal.add(Calendar.MONTH, -1);
			to = cal.getTime();
		}

		return current.compareTo(from) >= 0 && current.compareTo(to) <= 0;
	}

	public static boolean timeRange(final Object hour1, final Object min1, final Object sec1, final Object hour2,
			final Object min2, final Object sec2, final Object gmt) {
		final boolean useGmt = GMT.equalsIgnoreCase(String.valueOf(min1)) || GMT.equalsIgnoreCase(String.valueOf(sec1))
				|| GMT.equalsIgnoreCase(String.valueOf(min2)) || GMT.equalsIgnoreCase(String.valueOf(gmt));

		final Calendar cal = getCurrentTime(useGmt);
		cal.set(Calendar.MILLISECOND, 0);
		final Date current = cal.getTime();
		Date from;
		Date to;
		if (sec2 instanceof Number) {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, ((Number) min1).intValue());
			cal.set(Calendar.SECOND, ((Number) sec1).intValue());
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour2).intValue());
			cal.set(Calendar.MINUTE, ((Number) min2).intValue());
			cal.set(Calendar.SECOND, ((Number) sec2).intValue());
			to = cal.getTime();
		} else if (hour2 instanceof Number) {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, ((Number) min1).intValue());
			cal.set(Calendar.SECOND, 0);
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) sec1).intValue());
			cal.set(Calendar.MINUTE, ((Number) hour2).intValue());
			cal.set(Calendar.SECOND, 59);
			to = cal.getTime();
		} else if (min1 instanceof Number) {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) min1).intValue());
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			to = cal.getTime();
		} else {
			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			from = cal.getTime();

			cal.set(Calendar.HOUR_OF_DAY, ((Number) hour1).intValue());
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 59);
			to = cal.getTime();
		}

		if (to.before(from)) {
			cal.setTime(to);
			cal.add(Calendar.DATE, +1);
			to = cal.getTime();
		}

		return current.compareTo(from) >= 0 && current.compareTo(to) <= 0;
	}

	public static boolean isResolvableEx(final String host) {
		return isResolvable(host);
	}

	public static boolean isInNetEx(final String ipAddress, final String ipPrefix) {
		try {
			BigInteger ipAddressInteger = new BigInteger(1, InetAddress.getByName(ipAddress).getAddress());
			BigInteger ipPrefixInteger;
			final int bitMaskLength;
			if (ipPrefix.contains("/")) {
				ipPrefixInteger = new BigInteger(1, InetAddress.getByName(ipPrefix.substring(0, ipPrefix.indexOf("/"))).getAddress());
				bitMaskLength = Integer.parseInt(ipPrefix.substring(ipPrefix.indexOf("/") + 1));
			} else {
				ipPrefixInteger = new BigInteger(1, InetAddress.getByName(ipPrefix).getAddress());
				if (ipPrefix.contains(":")) {
					bitMaskLength = 32;
				} else {
					bitMaskLength = 128;
				}
			}

			for (int i = 0; i < ipAddressInteger.bitLength() - bitMaskLength; i++) {
				ipAddressInteger = ipAddressInteger.clearBit(i);
			}
			for (int i = 0; i < ipPrefixInteger.bitLength() - bitMaskLength; i++) {
				ipPrefixInteger = ipPrefixInteger.clearBit(i);
			}

			return ipAddressInteger.equals(ipPrefixInteger);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}

	public static String dnsResolveEx(final String host) {
		final StringBuilder result = new StringBuilder();
		try {
			final InetAddress[] list = InetAddress.getAllByName(host);
			for (final InetAddress inetAddress : list) {
				result.append(inetAddress.getHostAddress());
				result.append("; ");
			}
			return result.toString();
		} catch (@SuppressWarnings("unused") final UnknownHostException e) {
			return result.toString();
		}
	}

	public static String myIpAddressEx() {
		return getLocalAddressOfType(Inet6Address.class);
	}

	public static String sortIpAddressList(final String ipAddressList) {
		if (ipAddressList == null || ipAddressList.trim().length() == 0) {
			return "";
		}
		final List<InetAddress> parsedAddresses = new ArrayList<>();
		for (final String ip : ipAddressList.split(";")) {
			try {
				parsedAddresses.add(InetAddress.getByName(ip));
			} catch (@SuppressWarnings("unused") final UnknownHostException e) {
				// Do nothing
			}
		}
		Collections.sort(parsedAddresses, null);
		return ipAddressList;
	}

	public static String getClientVersion() {
		return "1.0";
	}

	private static long parseIpAddressToLong(final String address) {
		long result = 0;
		long shift = 24;
		for (final String part : address.split("\\.")) {
			final long lpart = Long.parseLong(part);

			result |= lpart << shift;
			shift -= 8;
		}
		return result;
	}

	private static String getLocalAddressOfType(final Class<? extends InetAddress> cl) {
		try {
			final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				final NetworkInterface current = interfaces.nextElement();
				if (!current.isUp() || current.isLoopback() || current.isVirtual()) {
					continue;
				}
				final Enumeration<InetAddress> addresses = current.getInetAddresses();
				while (addresses.hasMoreElements()) {
					final InetAddress adr = addresses.nextElement();
					if (cl.isInstance(adr)) {
						return adr.getHostAddress();
					}
				}
			}
			return "";
		} catch (@SuppressWarnings("unused") final IOException e) {
			return "";
		}
	}

	private static Calendar getCurrentTime(final boolean useGmt) {
		return Calendar.getInstance(useGmt ? TimeZone.getTimeZone(GMT) : TimeZone.getDefault());
	}

	private static void parseDateParam(final Map<String, Integer> params, final Object value) {
		if (value instanceof Number) {
			final int n = ((Number) value).intValue();
			if (n <= 31) {
				// Its a day
				if (params.get("day1") == null) {
					params.put("day1", n);
				} else {
					params.put("day2", n);
				}
			} else {
				// Its a year
				if (params.get("year1") == null) {
					params.put("year1", n);
				} else {
					params.put("year2", n);
				}
			}
		}

		if (value instanceof String) {
			final int n = MONTH_SHORT.indexOf(((String) value).toUpperCase(Locale.ENGLISH));
			if (n > -1) {
				// Its a month
				if (params.get("month1") == null) {
					params.put("month1", n);
				} else {
					params.put("month2", n);
				}
			}
		}

		if (GMT.equalsIgnoreCase(String.valueOf(value))) {
			params.put("gmt", 1);
		}
	}

	private static int indexOfCaseInsensitive(final List<String> data, final String item) {
		for (int i = 0; i < data.size(); i++) {
			final String dataItem = data.get(0);
			if (dataItem == item) {
				return i;
			} else if (dataItem != null && dataItem.equalsIgnoreCase(item)) {
				return i;
			}
		}
		return -1;
	}
}
