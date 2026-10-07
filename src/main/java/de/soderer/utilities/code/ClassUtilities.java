package de.soderer.utilities.code;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Static helper methods for reflection on classes.
 */
public class ClassUtilities {
	private ClassUtilities() {
		// Utility class with static methods only
	}

	/**
	 * Returns the declared constructor without parameters of the given class, regardless of its visibility.
	 *
	 * @param clazz class to get the constructor of
	 * @return constructor without parameters
	 * @throws NoSuchMethodException if the class has no constructor without parameters
	 * @throws SecurityException if access to the constructor is denied
	 */
	public static Constructor<?> getConstructor(final Class<?> clazz) throws NoSuchMethodException, SecurityException {
		return clazz.getDeclaredConstructor();
	}

	/**
	 * Returns all fields declared in the given class and its superclasses, regardless of their visibility.
	 * The fields of the class itself come first, followed by those of its superclasses.
	 *
	 * @param clazz class to get the fields of
	 * @return all declared fields of the class hierarchy
	 */
	public static List<Field> getAllFields(final Class<?> clazz) {
		final List<Field> fields = new ArrayList<>();

		for (final Field field : clazz.getDeclaredFields()) {
			fields.add(field);
		}

		if (clazz.getSuperclass() != null) {
			fields.addAll(getAllFields(clazz.getSuperclass()));
		}

		return fields;
	}

	/**
	 * Returns the field with the given name, searching the given class first and then its superclasses,
	 * regardless of the field's visibility.
	 *
	 * @param clazz class to start the search at
	 * @param fieldName name of the field
	 * @return field with the given name
	 * @throws NoSuchFieldException if neither the class nor any of its superclasses declares the field
	 */
	public static Field getField(final Class<?> clazz, final String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		} catch (final NoSuchFieldException e) {
			if (clazz.getSuperclass() != null) {
				return getField(clazz.getSuperclass(), fieldName);
			} else {
				throw e;
			}
		} catch (final SecurityException e) {
			throw e;
		}
	}
}
