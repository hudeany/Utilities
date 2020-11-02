package de.soderer.utilities.console;

import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.Utilities;

public class ConsoleUtilities {
	/**
	 * Additional attributes
		Bold 1
		Underline 4
		No underline 24
		Negative(reverse the foreground and background) 7
		Positive(no negative) 27
		Default 0
	 */
	public enum ConsoleColor {
		Black(30, 40),
		Red(31, 41),
		Green(32, 42),
		Yellow(33, 43),
		Blue(34, 44),
		Magenta(35, 45),
		Cyan(36, 46),
		Light_gray(37, 47),
		Dark_gray(90, 100),
		Light_red(91, 101),
		Light_green(92, 102),
		Light_yellow(93, 103),
		Light_blue(94, 104),
		Light_magenta(95, 105),
		Light_cyan(96, 106),
		White(97, 107);

		private final int foreGroundColorCode;
		private final int backGroundColorCode;

		ConsoleColor(final int foreGroundColorCode,final int backGroundColorCode) {
			this.foreGroundColorCode = foreGroundColorCode;
			this.backGroundColorCode = backGroundColorCode;
		}

		public int getForeGroundColorCode() {
			return foreGroundColorCode;
		}

		public int getBackGroundColorCode() {
			return backGroundColorCode;
		}
	}

	/**
	 * Create a progress string for terminal output e.g.: "65% [=================================>                   ] 103.234 200/s eta 5m"
	 *
	 * @param start
	 * @param itemsToDo
	 * @param itemsDone
	 * @return
	 */
	public static String getConsoleProgressString(final int lineLength, final Date start, final long itemsToDo, final long itemsDone) {
		final Date now = new Date();
		String itemsToDoString = "??";
		String percentageString = " 0%";
		String speedString = "???/s";
		String etaString = "eta ???";
		int percentageDone = 0;
		if (itemsToDo > 0 && itemsDone > 0) {
			itemsToDoString = NumberFormat.getNumberInstance(Locale.getDefault()).format(itemsToDo);
			percentageDone = (int) (itemsDone * 100 / itemsToDo);
			percentageString = Utilities.leftPad(percentageDone + "%", 3);
			long elapsedSeconds = (now.getTime() - start.getTime()) / 1000;
			// Prevent division by zero, when start is fast
			if (elapsedSeconds == 0) {
				elapsedSeconds = 1;
			}
			final int speed = (int) (itemsDone / elapsedSeconds);
			speedString = Utilities.getHumanReadableNumber(speed, "", true) + "/s";
			final Date estimatedEnd = DateUtilities.calculateETA(start, itemsToDo, itemsDone);
			etaString = "eta " + DateUtilities.getShortHumanReadableTimespan(estimatedEnd.getTime() - now.getTime(), false, true);
		} else if (itemsToDo > 0) {
			itemsToDoString = NumberFormat.getNumberInstance(Locale.getDefault()).format(itemsToDo);
		}

		final String leftPart = percentageString + " [";
		final String rightPart = "] " + itemsToDoString + " " + speedString + " " + etaString;
		final int barWith = lineLength - (leftPart.length() + rightPart.length());
		int barDone = barWith * percentageDone / 100;
		if (barDone < 1) {
			barDone = 1;
		} else if (barDone >= barWith) {
			barDone = barWith;
		}
		return leftPart + Utilities.repeat("=", barDone - 1) + ">" + Utilities.repeat(" ", barWith - barDone) + rightPart;
	}

	public static void printBoxed(final String text) {
		printBoxed(text, '*');
	}

	public static void printBoxed(final String text, final char boxChar) {
		if (text != null) {
			System.out.println(Utilities.repeat(boxChar, text.length() + 4));
			System.out.println(boxChar + " " + text + " " + boxChar);
			System.out.println(Utilities.repeat(boxChar, text.length() + 4));
		}
	}

	public static void clearScreen() {
		final String os = System.getProperty("os.name");
		if (os.contains("Windows")) {
			try {
				new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
			} catch (@SuppressWarnings("unused") final Exception e) {
				System.err.println("Cannot clear Windows console");
			}
		} else {
			System.out.print("\033[H\033[2J");
			System.out.flush();
		}
	}

	public static void printColoredText(final ConsoleColor foreGroundColor, final String text) {
		System.out.println("\033[" + foreGroundColor.getForeGroundColorCode() + "m" + text + "\033[0m");
	}

	public static void printColoredText(final ConsoleColor foreGroundColor, final ConsoleColor backGroundColor, final String text) {
		System.out.println("\033[" + foreGroundColor.getForeGroundColorCode() + "m\033[" + backGroundColor.getBackGroundColorCode() + "m" + text + "\033[0m");
	}
}
