package de.soderer.utilities.swing;

import java.awt.Toolkit;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;

import javax.swing.UIManager;

import de.soderer.utilities.SystemUtilities;

/**
 * Helper methods for Swing applications (class preloading, Linux window
 * manager class, look and feel).
 */
public class SwingUtilities {
	private SwingUtilities() {
		// Utility class
	}

	/**
	 * Loads and initializes a class and its constructors by calling its first
	 * public constructor with dummy values (false, 0, 'x' or null).
	 *
	 * <p>
	 * Prevents exceptions of type
	 * {@code Exception in thread "AWT-EventQueue-0" java.lang.NoClassDefFoundError}
	 * when classes are first used within AWT threads while the jar-in-jar-loader
	 * is used. Exceptions thrown by the constructor itself because of the dummy
	 * values are ignored, other problems are printed to the console.
	 * </p>
	 *
	 * @param clazz class to initialize
	 */
	public static void forceConstructorInit(final Class<?> clazz) {
		try {
			final Class<?> initClass = Class.forName(clazz.getName(), true, clazz.getClassLoader());
			final Constructor<?>[] constructors = initClass.getConstructors();
			if (constructors.length > 0) {
				final Constructor<?> constructor = constructors[0];
				final Object[] parameters = new Object[constructor.getParameters().length];
				int i = 0;
				for (final Parameter parameter : constructor.getParameters()) {
					if (parameter.getType() == boolean.class) {
						parameters[i] = false;
					} else if (parameter.getType() == byte.class) {
						parameters[i] = (byte) 0;
					} else if (parameter.getType() == short.class) {
						parameters[i] = (short) 0;
					} else if (parameter.getType() == int.class) {
						parameters[i] = 0;
					} else if (parameter.getType() == long.class) {
						parameters[i] = 0L;
					} else if (parameter.getType() == float.class) {
						parameters[i] = 0F;
					} else if (parameter.getType() == double.class) {
						parameters[i] = 0D;
					} else if (parameter.getType() == char.class) {
						parameters[i] = 'x';
					} else {
						parameters[i] = null;
					}
					i++;
				}
				constructor.newInstance(parameters);
			}
		} catch (@SuppressWarnings("unused") final InvocationTargetException e) {
			// Something went wrong inside the constructor because of the dummy values.
			// This doesen't matter, the classloader loaded the class and constructors anyway
			// do nothing
		} catch (final Exception e) {
			System.out.println("forceConstructorInit failed for class: " + clazz.getName());
			e.printStackTrace();
		}
	}

	/**
	 * Sets the AWT WM_CLASS on Linux, so the Gnome window manager can pin the
	 * application to its applications dock. Does nothing on other systems.
	 *
	 * <p>
	 * Needs the java runtime option
	 * {@code --add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED}, otherwise an error
	 * message is printed to System.err.
	 * </p>
	 *
	 * @param applicationName window manager class name of the application
	 */
	public static void setAwtWmClass(final String applicationName) {
		if (SystemUtilities.isLinuxSystem()) {
			try {
				final Toolkit toolkit = Toolkit.getDefaultToolkit();
				final Field awtAppClassNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
				awtAppClassNameField.setAccessible(true);
				awtAppClassNameField.set(toolkit, applicationName);
			} catch (final Exception e) {
				System.err.println("Cannot set AWT WM_CLASS: " + e.getMessage());

				System.err.println("Adding open directive");
				final Module mod = SwingUtilities.class.getClassLoader().getUnnamedModule();
				if (mod == Toolkit.class.getModule()) {
					Toolkit.getDefaultToolkit().getClass().getModule().addOpens("sun.awt.X11", mod);
				}

				try {
					final Toolkit toolkit = Toolkit.getDefaultToolkit();
					final Field awtAppClassNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
					awtAppClassNameField.setAccessible(true);
					awtAppClassNameField.set(toolkit, applicationName);
				} catch (final Exception e2) {
					System.err.println("Cannot set AWT WM_CLASS: " + e2.getMessage());
				}
			}
		}
	}

	/**
	 * Sets the look and feel of the operating system. Errors are printed to the
	 * console and leave the current look and feel unchanged.
	 */
	public static void setSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
