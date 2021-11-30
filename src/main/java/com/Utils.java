package com;

public class Utils {

	public static <T extends Enum<?>> T searchEnum(Class<T> enumeration, String search) throws Exception {
		for (T each : enumeration.getEnumConstants()) {
			if (each.name().compareToIgnoreCase(search) == 0) {
				return each;
			}
		}
		throw new Exception("Non existed element: " + search + " in enum: " + enumeration.getName());
	}
}
