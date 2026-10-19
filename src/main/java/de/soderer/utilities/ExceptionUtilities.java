package de.soderer.utilities;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Helpers to convert exceptions and their causes into readable text.
 */
public class ExceptionUtilities {
	/** Maximum number of cause levels included by {@link #throwableToString(Throwable, int)} in any case. */
	private static final int MAX_CAUSE_LEVELS = 100;

	private ExceptionUtilities() {
		// Utility class
	}

	/**
	 * Get a text with class name, message and full stack trace (including causes) of a throwable.
	 *
	 * @param t
	 *            throwable to describe, must not be null
	 * @return multi-line description of the throwable, the message line is left out if the throwable has no message
	 */
	public static String toString(final Throwable t) {
		final StringWriter stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(stringWriter);
		printWriter.println(t.getClass().getName());
		if (t.getMessage() != null) {
			printWriter.println(t.getMessage());
		}
		printWriter.println(ExceptionUtilities.getStackTrace(t));
		printWriter.flush();
		return stringWriter.toString();
	}

	/**
	 * Get the stack trace of a throwable as printed by {@link Throwable#printStackTrace()}, including causes and suppressed exceptions.
	 *
	 * @param t
	 *            throwable, must not be null
	 * @return stack trace text
	 */
	public static String getStackTrace(final Throwable t) {
		final StringWriter stringWriter = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(stringWriter);
		t.printStackTrace(printWriter);
		printWriter.flush();
		return stringWriter.toString();
	}

	/**
	 * Get class names, messages and stack traces of a throwable and up to 5 levels of its causes.
	 *
	 * @param throwable
	 *            throwable, must not be null
	 * @return text with class name, message and stack trace of each level, separated by blank lines
	 */
	public static String throwableToString(final Throwable throwable) {
		return throwableToString(throwable, 5);
	}

	/**
	 * Get class names, messages and stack traces of a throwable and its causes. Suppressed exceptions are not included,
	 * use {@link #getStackTrace(Throwable)} for them.
	 * At most 100 cause levels are included in any case, and cyclic cause chains are stopped.
	 * If causes are left out because of maxLevel or the limit of 100 levels, a "cut after level" note is appended.
	 *
	 * @param throwable
	 *            throwable, must not be null
	 * @param maxLevel
	 *            maximum number of cause levels to include, negative for no limit
	 * @return text with class name, message and stack trace of each level, separated by blank lines
	 */
	public static String throwableToString(final Throwable throwable, final int maxLevel) {
		// Throwable.toString() contains the class name and the message, so the type is also shown without a message
		final StringBuilder returnBuilder = new StringBuilder(throwable.toString() + "\n" + Utilities.stacktraceToString(throwable.getStackTrace()));
		final Set<Throwable> visitedThrowables = Collections.newSetFromMap(new IdentityHashMap<>());
		visitedThrowables.add(throwable);
		int level = 0;
		Throwable subThrowable = throwable.getCause();
		// Stop on cycles anywhere in the cause chain, not only on a cause pointing back to the root throwable
		while ((maxLevel < 0 || level < maxLevel) && level < MAX_CAUSE_LEVELS && subThrowable != null && visitedThrowables.add(subThrowable)) {
			returnBuilder.append("\n\n" + subThrowable.toString() + "\n" + Utilities.stacktraceToString(subThrowable.getStackTrace()));
			level++;
			subThrowable = subThrowable.getCause();
		}
		// Only mention the cut, if there really are further causes left out (not when stopped by a cycle)
		if (subThrowable != null && !visitedThrowables.contains(subThrowable)) {
			returnBuilder.append("\n\n... cut after level " + level + " ...");
		}
		return returnBuilder.toString();
	}
}
