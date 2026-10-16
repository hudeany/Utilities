package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.file.FileStore;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.crypto.Cipher;

import de.soderer.utilities.collection.CaseInsensitiveMap;

/**
 * Information about the operating system, the running JVM and its process.
 */
public class SystemUtilities {
	/**
	 * Name of the system property, which is set to the absolute path of the running application jar file.
	 */
	public static final String SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR = "process.jar";

	private static final String OS_NAME_LOWER_CASE = getOsName() == null ? "" : getOsName().toLowerCase(Locale.ROOT);
	private static final boolean isWindows = OS_NAME_LOWER_CASE.contains("windows");
	private static final boolean isLinux = OS_NAME_LOWER_CASE.contains("unix") || OS_NAME_LOWER_CASE.contains("linux");

	private SystemUtilities() {
		// Utility class
	}

	/**
	 * Name of the operating system (system property "os.name").
	 *
	 * @return operating system name, e.g. "Linux" or "Windows 11"
	 */
	public static String getOsName() {
		return System.getProperty("os.name");
	}

	/**
	 * Whether the operating system is Windows.
	 *
	 * @return true on Windows
	 */
	public static boolean isWindowsSystem() {
		return isWindows;
	}

	/**
	 * Whether the operating system is Linux or another system calling itself "Unix" (macOS is not included).
	 *
	 * @return true on Linux/Unix
	 */
	public static boolean isLinuxSystem() {
		return isLinux;
	}

	/**
	 * Process id of the running JVM.
	 *
	 * @return process id, or -1 if it cannot be determined
	 */
	public static int getProcessId() {
		try {
			return (int) ProcessHandle.current().pid();
		} catch (@SuppressWarnings("unused") final UnsupportedOperationException e) {
			return -1;
		}
	}

	/**
	 * Absolute path of the java executable of the running JVM.
	 * On Windows "java.exe" is used if a console is attached, otherwise "javaw.exe", which runs without console window
	 * (so a restarted GUI application does not open a console window).
	 * On all other systems (Linux, macOS, ...) "java" is used.
	 *
	 * @return path of the java executable, or null if it cannot be found
	 */
	public static String getJavaBinPath() {
		final String javaHomePath = System.getProperty("java.home");
		if (javaHomePath == null) {
			return null;
		} else {
			File javaBinFile;
			if (!SystemUtilities.isWindowsSystem()) {
				javaBinFile = new File(javaHomePath + File.separator + "bin" + File.separator + "java");
			} else if (hasTerminalConsole()) {
				javaBinFile = new File(javaHomePath + File.separator + "bin" + File.separator + "java.exe");
			} else {
				javaBinFile = new File(javaHomePath + File.separator + "bin" + File.separator + "javaw.exe");
			}

			if (javaBinFile.exists() && !javaBinFile.isDirectory()) {
				return javaBinFile.getAbsolutePath();
			} else {
				return null;
			}
		}
	}

	/**
	 * Whether the JVM runs in a terminal. Since Java 22 System.console() also returns a console without terminal
	 * (e.g. when started by javaw.exe), so Console.isTerminal() is checked, if available.
	 */
	private static boolean hasTerminalConsole() {
		final Console console = System.console();
		if (console == null) {
			return false;
		}
		try {
			return (Boolean) Console.class.getMethod("isTerminal").invoke(console);
		} catch (@SuppressWarnings("unused") final ReflectiveOperationException e) {
			// Before Java 22 a console is only available with terminal
			return true;
		}
	}

	/**
	 * Uptime of the running JVM.
	 *
	 * @return uptime in milliseconds
	 */
	public static long getUptimeInMillis() {
		return ManagementFactory.getRuntimeMXBean().getUptime();
	}

	/**
	 * Start time of the running JVM in the local time zone.
	 *
	 * @return start time
	 */
	public static LocalDateTime getStarttime() {
		return DateUtilities.getLocalDateTime(ManagementFactory.getRuntimeMXBean().getStartTime());
	}

	/**
	 * Arguments the running JVM was started with (JVM options only, not the application's arguments).
	 *
	 * @return unmodifiable list of JVM input arguments
	 */
	public static List<String> getJavaStartupArguments() {
		return ManagementFactory.getRuntimeMXBean().getInputArguments();
	}

	/**
	 * Runtime information like memory usage, process id, uptime, start arguments, running jar file and free storage space.
	 *
	 * @param humanReadable true for formatted sizes (e.g. "1.5 GiByte"), false for plain byte numbers
	 * @return case insensitive map of property names and values
	 */
	public static Map<String, String> getRuntimeProperties(final boolean humanReadable) {
		final Runtime runtime = Runtime.getRuntime();
		final Map<String, String> resultMap = new CaseInsensitiveMap<>();
		if (humanReadable) {
			resultMap.put("memory.total", Utilities.getHumanReadableNumber(runtime.totalMemory(), "Byte", false, 5, false, Locale.ENGLISH));
			resultMap.put("memory.free", Utilities.getHumanReadableNumber(runtime.freeMemory(), "Byte", false, 5, false, Locale.ENGLISH));
			resultMap.put("memory.used", Utilities.getHumanReadableNumber(runtime.totalMemory() - runtime.freeMemory(), "Byte", false, 5, false, Locale.ENGLISH));
			resultMap.put("memory.max", Utilities.getHumanReadableNumber(runtime.maxMemory(), "Byte", false, 5, false, Locale.ENGLISH));
		} else {
			resultMap.put("memory.total", Long.toString(runtime.totalMemory()));
			resultMap.put("memory.free", Long.toString(runtime.freeMemory()));
			resultMap.put("memory.used", Long.toString(runtime.totalMemory() - runtime.freeMemory()));
			resultMap.put("memory.max", Long.toString(runtime.maxMemory()));
		}
		resultMap.put("processors.available", Long.toString(runtime.availableProcessors()));
		resultMap.put("process.id", Integer.toString(SystemUtilities.getProcessId()));
		resultMap.put("process.uptime", Long.toString(getUptimeInMillis()));
		resultMap.put("process.starttime", DateUtilities.formatZonedDateTimeWithZuluTimezone(getStarttime().atZone(ZoneId.systemDefault())));
		resultMap.put("process.javastartuparguments", getJavaStartupArguments().toString());
		resultMap.put("process.jar", getCurrentlyRunningJarFilePath() == null ? "<unknown>" : getCurrentlyRunningJarFilePath());

		resultMap.put("cryptography.unlimitedKeyStrength", isUnlimitedKeyStrengthAllowed() ? "true" : "false");

		for (final Path root : FileSystems.getDefault().getRootDirectories()) {
			try {
				final FileStore store = Files.getFileStore(root);
				if (humanReadable) {
					resultMap.put("storageSpace.'" + root + "'.available", Utilities.getHumanReadableNumber(store.getUsableSpace(), "Byte", false, 5, false, Locale.ENGLISH));
					resultMap.put("storageSpace.'" + root + "'.total", Utilities.getHumanReadableNumber(store.getTotalSpace(), "Byte", false, 5, false, Locale.ENGLISH));
				} else {
					resultMap.put("storageSpace.'" + root + "'.available", Long.toString(store.getUsableSpace()));
					resultMap.put("storageSpace.'" + root + "'.total", Long.toString(store.getTotalSpace()));
				}
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}

		return resultMap;
	}

	/**
	 * Path of the currently running jar file, as used by JarInJarLoader in a system property or environment variable.
	 *
	 * @return jar file path, or null if unknown
	 */
	public static String getCurrentlyRunningJarFilePath() {
		String jarFilePath = System.getProperty(SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
		if (jarFilePath == null) {
			jarFilePath = System.getenv(SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
		}
		return jarFilePath;
	}

	/**
	 * Raw output lines of the operating system's process list command ("tasklist.exe" on Windows, "ps -e" otherwise).
	 * Error output of the command is included.
	 *
	 * @return output lines, or null if the command failed
	 */
	public static List<String> getProcessListRaw() {
		try {
			final List<String> data = new ArrayList<>();
			final ProcessBuilder processBuilder;
			if (isWindows) {
				processBuilder = new ProcessBuilder(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
			} else {
				processBuilder = new ProcessBuilder("ps", "-e");
			}
			// Error output must be read too, otherwise a full error output buffer would block the command
			processBuilder.redirectErrorStream(true);
			final Process process = processBuilder.start();
			try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = input.readLine()) != null) {
					data.add(line);
				}
			}
			if (process.waitFor() != 0) {
				return null;
			}
			return data;
		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Whether the JVM allows unlimited AES key lengths (no export restrictions of the JCE policy).
	 *
	 * @return true if unlimited key strength is allowed
	 */
	public static boolean isUnlimitedKeyStrengthAllowed() {
		try {
			return Cipher.getMaxAllowedKeyLength("AES") == Integer.MAX_VALUE;
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			return false;
		}
	}

	/**
	 * Determines the JVM parameters (e.g. -Xmx, -D..., --add-opens, -XstartOnFirstThread)
	 * the currently running application was started with, so they can be passed on
	 * to the restarted application after an update.
	 *
	 * Excluded are:
	 * <ul>
	 * <li>debugger and agent options: a restarted process must not suspend waiting for
	 *   a debugger or load an agent it was not explicitly meant to load</li>
	 * <li>options originating from the environment variables JAVA_TOOL_OPTIONS and
	 *   JDK_JAVA_OPTIONS: the restarted process inherits the environment and would
	 *   otherwise receive them twice</li>
	 * </ul>
	 *
	 * @return modifiable list of JVM parameters, never null (empty if they cannot be determined)
	 */
	public static List<String> getCurrentJvmParameters() {
		final List<String> inputArguments;
		try {
			inputArguments = new ArrayList<>(ManagementFactory.getRuntimeMXBean().getInputArguments());
		} catch (@SuppressWarnings("unused") final Exception | LinkageError e) {
			// Module java.management may be missing (e.g. minimal jlink image) or access may be denied
			return new ArrayList<>();
		}

		removeEnvironmentProvidedOptions(inputArguments, System.getenv("JAVA_TOOL_OPTIONS"));
		removeEnvironmentProvidedOptions(inputArguments, System.getenv("JDK_JAVA_OPTIONS"));

		final List<String> jvmParameters = new ArrayList<>();
		for (final String argument : inputArguments) {
			if (!Utilities.isBlank(argument) && !isExcludedJvmParameter(argument)) {
				jvmParameters.add(argument);
			}
		}
		return jvmParameters;
	}

	private static boolean isExcludedJvmParameter(final String argument) {
		return argument.startsWith("-agentlib:")
				|| argument.startsWith("-agentpath:")
				|| argument.startsWith("-javaagent:")
				|| argument.startsWith("-Xrun") // legacy form, e.g. -Xrunjdwp:...
				|| "-Xdebug".equals(argument)
				|| "-Xnoagent".equals(argument);
	}

	/**
	 * Removes one occurrence of each option contained in the given environment variable value.
	 * Simple whitespace tokenizing is sufficient here, as options containing quoted spaces
	 * are extremely rare in these variables.
	 */
	private static void removeEnvironmentProvidedOptions(final List<String> arguments, final String environmentValue) {
		if (Utilities.isBlank(environmentValue)) {
			return;
		} else {
			for (final String option : environmentValue.trim().split("\\s+")) {
				arguments.remove(option);
			}
		}
	}
}
