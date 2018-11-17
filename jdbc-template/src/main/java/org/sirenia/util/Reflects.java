package org.sirenia.util;

import java.lang.reflect.Field;

public class Reflects {
	public static Field getFieldByFieldName(Object obj, String fieldName) {
		for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass
				.getSuperclass()) {
			try {
				return superClass.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
			}
		}
		return null;
	}

	public static Object getValueByFieldName(Object obj, String fieldName) {
		try {
			Field field = getFieldByFieldName(obj, fieldName);
			Object value = null;
			if (field != null) {
				if (field.isAccessible()) {
					value = field.get(obj);
				} else {
					field.setAccessible(true);
					value = field.get(obj);
					field.setAccessible(false);
				}
			}
			return value;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void setValueByFieldName(Object obj, String fieldName, Object value) {
		try {
			Field field = getFieldByFieldName(obj, fieldName);
			if (field.isAccessible()) {
				field.set(obj, value);
			} else {
				field.setAccessible(true);
				field.set(obj, value);
				field.setAccessible(false);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
