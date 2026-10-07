package de.soderer.utilities.code;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Java source file whose content is held in a String, for compiling code at runtime with
 * {@link javax.tools.JavaCompiler}.
 */
public class JavaSourceFromString extends SimpleJavaFileObject {
	private final String code;

	/**
	 * Creates a source file object for the given class.
	 *
	 * @param name fully qualified name of the class, e.g. "de.soderer.test.SimpleWorker"
	 * @param code complete Java source code of the class
	 */
	public JavaSourceFromString(final String name, final String code) {
		super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
		this.code = code;
	}

	@Override
	public CharSequence getCharContent(final boolean ignoreEncodingErrors) {
		return code;
	}

	/**
	 * Example of compiling and executing a class from a source String at runtime.
	 * The class is compiled into "~/tmp/jitClasses".
	 *
	 * @param args not used
	 */
	public static void main(final String[] args) {
		try {
			final JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
			if (javaCompiler == null) {
				throw new Exception("Compiler unavailable");
			}

			final String code = "package de.soderer.test;\n"
					+ "public class SimpleWorker {\n"
					+ "public void runJob() throws Exception {\n"
					+ "System.out.println(\"SimpleJobWorker worked\");\n"
					+ "}\n"
					+ "}\n";

			final JavaSourceFromString javaSourceFromString = new JavaSourceFromString("de.soderer.test.SimpleWorker", code);

			final Iterable<? extends JavaFileObject> fileObjects = Arrays.asList(javaSourceFromString);

			final String compilationPath = System.getProperty("user.home") + "/tmp/jitClasses";
			final File compilationPathFile = new File(compilationPath);
			compilationPathFile.mkdirs();

			final List<String> options = new ArrayList<>();
			options.add("-d");
			options.add(compilationPath);
			options.add("-classpath");
			// Since Java 9 the application class loader is no URLClassLoader anymore, and it must not be closed
			options.add(System.getProperty("java.class.path") + File.pathSeparator + compilationPath);

			final StringWriter output = new StringWriter();
			final boolean success = javaCompiler.getTask(output, null, null, options, null, fileObjects).call();
			if (success) {
				try (URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File(compilationPath).toURI().toURL() })) {
					final Class<?> clazz = Class.forName("de.soderer.test.SimpleWorker", true, classLoader);
					final Constructor<?> constructor = ClassUtilities.getConstructor(clazz);
					constructor.setAccessible(true);
					final Object instance = constructor.newInstance();
					final Method method = clazz.getMethod("runJob");
					method.invoke(instance);
				}
			} else {
				throw new Exception("Compilation failed :" + output);
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}