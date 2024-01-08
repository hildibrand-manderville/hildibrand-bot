/*
 * Copyright (C) 2011-2016 eu.haruka and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.haruka.jpsfw.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Class used for reflection.
 */
public class AccessEverything {

    /**
     * Gets a field from an Object.
     *
     * @param o         The object from which to retrieve the field.
     * @param fieldname The exact name of the field. (case-sensetive)
     * @return The Field which was given, or null if the field doesn't exist, an exception occurred, the field is final or static.
     */
    public static Object get(Object o, String fieldname) {
        return get(o, fieldname, Object.class);
    }

    /**
     * Gets a field from an Object.
     *
     * @param <T>       The type of the field.
     * @param o         The object from which to retrieve the field.
     * @param fieldname The exact name of the field. (case-sensetive)
     * @param clazz     The class of the field.
     * @return The Field which was given, or null if the field doesn't exist, an exception occurred, the field is final or static.
     */
    public static <T> T get(Object o, String fieldname, Class<? extends T> clazz) {
        try {
            Field f = null;
            try {
                f = o.getClass().getDeclaredField(fieldname);
            } catch (NoSuchFieldException ignored) {
            }
            if (f == null) {
                throw new NullPointerException();
            }
            f.setAccessible(true);
            return clazz.cast(f.get(o));
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Sets a field of an object.
     *
     * @param o         The object of which to set the field.
     * @param fieldname The exact name of the field. (case-sensetive)
     * @param value     The value to set.
     * @throws NoSuchFieldException     If the field does not exist.
     * @throws IllegalArgumentException if the specified object is not an instance of the class or interface declaring the underlying field (or a subclass or implementor thereof), or if an unwrapping conversion fails.
     * @throws IllegalAccessException   If the field is final.
     */
    public static void set(Object o, String fieldname, Object value) throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field f = null;
        try {
            f = o.getClass().getDeclaredField(fieldname);
        } catch (NoSuchFieldException ignored) {
        }
        if (f == null) {
            f = o.getClass().getField(fieldname);
            if (f == null) {
                throw new NullPointerException();
            }
        }
        f.setAccessible(true);
        f.set(o, value);
    }

    /**
     * Executes a method.
     *
     * @param clazz      The class of which to execute the method.
     * @param o          The object of which to execute the method. May be null if the method of clazz is static.
     * @param methodname The exact name of the method. (case-sensetive)
     * @param parameters The arguments of the method.
     * @return The return value of the method.
     * @throws NoSuchMethodException     If the given method does not exist with the given argument types.
     * @throws IllegalArgumentException  if the method is an instance method and the specified object argument is not an instance of the class or interface declaring the underlying method (or of a subclass or implementor thereof); if the number of actual and formal parameters differ; if an unwrapping conversion for primitive arguments fails; or if, after possible unwrapping, a parameter value cannot be converted to the corresponding formal parameter type by a method invocation conversion.
     * @throws InvocationTargetException if the method throws an exception.
     */
    public static Object execute(Class<?> clazz, Object o, String methodname, Object... parameters) throws NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        Class<?>[] cparams = new Class<?>[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            cparams[i] = parameters[i].getClass();
        }
        Method m = clazz.getDeclaredMethod(methodname, cparams);
        m.setAccessible(true);
        try {
            return m.invoke(o, parameters);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void setFinal(Field field, Object obj) throws Exception {
        field.setAccessible(true);
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, obj);
    }
}
