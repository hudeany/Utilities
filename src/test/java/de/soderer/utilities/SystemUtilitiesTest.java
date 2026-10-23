package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class SystemUtilitiesTest {
	@Test
	public void getOsName() {
		assertEquals(System.getProperty("os.name"), SystemUtilities.getOsName());
	}

	@Test
	public void isWindowsOrLinuxSystem() {
		final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);

		assertEquals(osName.contains("windows"), SystemUtilities.isWindowsSystem());
		assertEquals(osName.contains("linux") || osName.contains("unix"), SystemUtilities.isLinuxSystem());
		assertFalse(SystemUtilities.isWindowsSystem() && SystemUtilities.isLinuxSystem());
	}

	@Test
	public void getProcessId() {
		assertEquals((int) ProcessHandle.current().pid(), SystemUtilities.getProcessId());
	}

	@Test
	public void getJavaBinPath() {
		final String javaBinPath = SystemUtilities.getJavaBinPath();

		assertNotNull(javaBinPath);
		final File javaBinFile = new File(javaBinPath);
		assertTrue(javaBinFile.isFile(), javaBinPath);
		assertTrue(Arrays.asList("java", "java.exe", "javaw.exe").contains(javaBinFile.getName()), javaBinPath);
		assertEquals(new File(System.getProperty("java.home"), "bin").getAbsolutePath(), javaBinFile.getParentFile().getAbsolutePath());
		if (!SystemUtilities.isWindowsSystem()) {
			assertEquals("java", javaBinFile.getName());
		}
	}

	@Test
	public void getUptimeAndStarttime() {
		final long uptime1 = SystemUtilities.getUptimeInMillis();
		final long uptime2 = SystemUtilities.getUptimeInMillis();
		final LocalDateTime starttime = SystemUtilities.getStarttime();
		final LocalDateTime now = LocalDateTime.now();

		assertTrue(uptime1 >= 0);
		assertTrue(uptime2 >= uptime1);
		assertFalse(starttime.isAfter(now), starttime + " is after " + now);
		// Start time and uptime must fit together, with some tolerance for the time between the calls
		assertFalse(starttime.isBefore(now.minusNanos((uptime2 + 60_000) * 1_000_000)), starttime.toString());
	}

	@Test
	public void getJavaStartupArguments() {
		assertEquals(ManagementFactory.getRuntimeMXBean().getInputArguments(), SystemUtilities.getJavaStartupArguments());
	}

	@Test
	public void getRuntimeProperties_plainNumbers() {
		final Map<String, String> properties = SystemUtilities.getRuntimeProperties(false);

		for (final String key : new String[] { "memory.total", "memory.free", "memory.used", "memory.max", "process.uptime" }) {
			assertTrue(Long.parseLong(properties.get(key)) >= 0, key);
		}
		assertEquals(Integer.toString(Runtime.getRuntime().availableProcessors()), properties.get("processors.available"));
		assertEquals(Integer.toString(SystemUtilities.getProcessId()), properties.get("process.id"));
		assertEquals(SystemUtilities.getJavaStartupArguments().toString(), properties.get("process.javastartuparguments"));
		assertNotNull(properties.get("process.starttime"));
		assertEquals(Boolean.toString(SystemUtilities.isUnlimitedKeyStrengthAllowed()), properties.get("cryptography.unlimitedKeyStrength"));
		// The map is case insensitive
		assertEquals(properties.get("memory.max"), properties.get("MEMORY.MAX"));
		// At least the file system of the working directory has a root with storage space information
		assertTrue(properties.keySet().stream().anyMatch(key -> key.startsWith("storageSpace.") && key.endsWith(".total")), properties.keySet().toString());
	}

	@Test
	public void getRuntimeProperties_humanReadable() {
		final Map<String, String> properties = SystemUtilities.getRuntimeProperties(true);

		for (final String key : new String[] { "memory.total", "memory.free", "memory.used", "memory.max" }) {
			assertTrue(properties.get(key).endsWith("Byte"), key + ": " + properties.get(key));
		}
	}

	@Test
	public void getRuntimeProperties_runningJar() {
		final String originalValue = System.getProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
		try {
			System.setProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR, "/opt/app/app.jar");
			assertEquals("/opt/app/app.jar", SystemUtilities.getRuntimeProperties(false).get("process.jar"));

			System.clearProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
			if (System.getenv(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR) == null) {
				assertEquals("<unknown>", SystemUtilities.getRuntimeProperties(false).get("process.jar"));
			}
		} finally {
			restoreSystemProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR, originalValue);
		}
	}

	@Test
	public void getCurrentlyRunningJarFilePath() {
		final String originalValue = System.getProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
		try {
			System.setProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR, "/opt/app/app.jar");
			assertEquals("/opt/app/app.jar", SystemUtilities.getCurrentlyRunningJarFilePath());

			// Without system property the environment variable is used
			System.clearProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
			assertEquals(System.getenv(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR), SystemUtilities.getCurrentlyRunningJarFilePath());
		} finally {
			restoreSystemProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR, originalValue);
		}
	}

	@Test
	public void getProcessListRaw_containsOwnProcess() {
		final List<String> lines = SystemUtilities.getProcessListRaw();

		// "ps" may be missing in minimal containers
		Assumptions.assumeTrue(lines != null, "Process list command is not available");
		assertNotNull(lines);
		assertFalse(lines.isEmpty());
		final String processId = Integer.toString(SystemUtilities.getProcessId());
		assertTrue(lines.stream().anyMatch(line -> Arrays.asList(line.trim().split("\\s+")).contains(processId)), "Process id " + processId + " not listed");
	}

	@Test
	public void isUnlimitedKeyStrengthAllowed() {
		// Unlimited key strength is the default since Java 9
		assertTrue(SystemUtilities.isUnlimitedKeyStrengthAllowed());
	}

	@Test
	public void getCurrentJvmParameters() {
		final List<String> jvmParameters = SystemUtilities.getCurrentJvmParameters();
		final List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();

		assertTrue(inputArguments.containsAll(jvmParameters), jvmParameters.toString());
		for (final String parameter : jvmParameters) {
			// For example the agent of a coverage tool like JaCoCo is not passed on
			assertFalse(parameter.startsWith("-javaagent:") || parameter.startsWith("-agentlib:") || parameter.startsWith("-agentpath:"), parameter);
		}
		// Options of JAVA_TOOL_OPTIONS are not passed on, the restarted process gets them from the environment again
		final String javaToolOptions = System.getenv("JAVA_TOOL_OPTIONS");
		if (javaToolOptions != null && !javaToolOptions.isBlank()) {
			for (final String option : javaToolOptions.trim().split("\\s+")) {
				if (inputArguments.indexOf(option) == inputArguments.lastIndexOf(option)) {
					assertFalse(jvmParameters.contains(option), option);
				}
			}
		}
		// The list is modifiable
		jvmParameters.add("-Xmx1g");
	}

	@Test
	public void isExcludedJvmParameter() throws Exception {
		final Method method = SystemUtilities.class.getDeclaredMethod("isExcludedJvmParameter", String.class);
		method.setAccessible(true);

		for (final String excluded : new String[] { "-agentlib:jdwp=transport=dt_socket", "-agentpath:/lib/agent.so", "-javaagent:/lib/jacocoagent.jar", "-Xrunjdwp:transport=dt_socket", "-Xdebug", "-Xnoagent" }) {
			assertTrue((Boolean) method.invoke(null, excluded), excluded);
		}
		for (final String kept : new String[] { "-Xmx1g", "-Dfile.encoding=UTF-8", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-XstartOnFirstThread", "-Xdebugx" }) {
			assertFalse((Boolean) method.invoke(null, kept), kept);
		}
	}

	@Test
	public void removeEnvironmentProvidedOptions() throws Exception {
		final Method method = SystemUtilities.class.getDeclaredMethod("removeEnvironmentProvidedOptions", List.class, String.class);
		method.setAccessible(true);

		final List<String> arguments = new ArrayList<>(List.of("-Xmx1g", "-Da=1", "-Db=2", "-Da=1"));
		method.invoke(null, arguments, "  -Da=1 \t -Dc=3 ");
		// Only one occurrence is removed, the other one was given on the command line
		assertEquals(List.of("-Xmx1g", "-Db=2", "-Da=1"), arguments);

		method.invoke(null, arguments, null);
		method.invoke(null, arguments, " ");
		assertEquals(List.of("-Xmx1g", "-Db=2", "-Da=1"), arguments);
	}

	private static void restoreSystemProperty(final String name, final String value) {
		if (value == null) {
			System.clearProperty(name);
		} else {
			System.setProperty(name, value);
		}
	}
}
