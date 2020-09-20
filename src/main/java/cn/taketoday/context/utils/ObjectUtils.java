/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import cn.taketoday.context.exception.ConversionException;

/**
 * @author TODAY <br>
 *         2019-08-23 00:16
 */
public abstract class ObjectUtils {

    /**
     * Test if a array is a null or empty object
     * 
     * @param array
     *            An array to test if its a null or empty object
     * @return If a object is a null or empty object
     */
    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Test if a object is a null or empty object
     * 
     * @param obj
     *            A instance to test if its a null or empty object
     * @return If a object is a null or empty object
     */
    public static boolean isEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof String) {
            return ((String) obj).isEmpty();
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        return obj.getClass().isArray() && Array.getLength(obj) == 0;
    }

    public static boolean isNotEmpty(Object[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(Object obj) {
        return !isEmpty(obj);
    }

    //

    /**
     * To array object
     * 
     * @param source
     *            String array
     * @param targetClass
     *            Target class
     * @return An array object
     * @throws ConversionException
     *             If can't convert source to target type object
     */
    public static Object toArrayObject(String source[], Class<?> targetClass) throws ConversionException {

        // @since 2.1.6 fix: String[].class can't be resolve
        if (String[].class == targetClass) {
            return source;
        }
        final int length = source.length;
        if (int[].class == targetClass) {
            final int[] newInstance = new int[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Integer.parseInt(source[j]);
            return newInstance;
        }
        else if (Integer[].class == targetClass) {
            final Integer[] newInstance = new Integer[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Integer.valueOf(source[j]);
            return newInstance;
        }
        else if (long[].class == targetClass) {
            final long[] newInstance = new long[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Long.parseLong(source[j]);
            return newInstance;
        }
        else if (Long[].class == targetClass) {
            final Long[] newInstance = new Long[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Long.valueOf(source[j]);
            return newInstance;
        }
        else if (short[].class == targetClass) {
            final short[] newInstance = new short[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Short.parseShort(source[j]);
            return newInstance;
        }
        else if (Short[].class == targetClass) {
            final Short[] newInstance = new Short[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Short.valueOf(source[j]);
            return newInstance;
        }
        else if (byte[].class == targetClass) {
            final byte[] newInstance = new byte[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Byte.parseByte(source[j]);
            return newInstance;
        }
        else if (Byte[].class == targetClass) {
            final Byte[] newInstance = new Byte[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Byte.valueOf(source[j]);
            return newInstance;
        }
        else if (float[].class == targetClass) {
            final float[] newInstance = new float[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Float.parseFloat(source[j]);
            return newInstance;
        }
        else if (Float[].class == targetClass) {
            final Float[] newInstance = new Float[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Float.valueOf(source[j]);
            return newInstance;
        }
        else if (double[].class == targetClass) {
            final double[] newInstance = new double[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Double.parseDouble(source[j]);
            return newInstance;
        }
        else if (Double[].class == targetClass) {
            final Double[] newInstance = new Double[length];
            for (short j = 0; j < length; j++)
                newInstance[j] = Double.valueOf(source[j]);
            return newInstance;
        }
        { // fix @since 2.1.6
            if (targetClass.isArray()) {
                targetClass = targetClass.getComponentType();
            }
            final Object newInstance = Array.newInstance(targetClass, length);
            for (short i = 0; i < length; i++) {
                Array.set(newInstance, i, ConvertUtils.convert(source[i], targetClass));
            }
            return newInstance;
        }
    }

    public static <T> T parseArray(String source[], Class<T> targetClass) throws ConversionException {
        return targetClass.cast(toArrayObject(source, targetClass));
    }

    public static String toHexString(final Object obj) {
        return obj == null
                ? "null"
                : new StringBuilder()
                        .append(obj.getClass().getName())
                        .append('@')
                        .append(Integer.toHexString(obj.hashCode())).toString();
    }

}
