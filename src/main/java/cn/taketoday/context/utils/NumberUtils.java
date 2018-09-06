/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
 * 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import cn.taketoday.context.exception.ConversionException;

/**
 * @author Today
 * @date 2018年7月6日 下午1:36:29
 */
public abstract class NumberUtils {

	/**
	 * to array object
	 * 
	 * @param source
	 *            String array
	 * @param targetClass
	 *            target class
	 * @return array object
	 * @throws ConversionException
	 */
	public final static Object toArrayObject(String source[], Class<?> targetClass) throws ConversionException {

		final int length = source.length;
		if (int[].class == targetClass) {
			int[] newInstance = new int[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Integer.parseInt(source[j]);
			return newInstance;
		} else if (Integer[].class == targetClass) {
			Integer[] newInstance = new Integer[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Integer.parseInt(source[j]);
			return newInstance;
		} else if (long[].class == targetClass) {
			long[] newInstance = new long[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Long.parseLong(source[j]);
			return newInstance;
		} else if (Long[].class == targetClass) {
			Long[] newInstance = new Long[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Long.parseLong(source[j]);
			return newInstance;
		} else if (short[].class == targetClass) {
			short[] newInstance = new short[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Short.parseShort(source[j]);
			return newInstance;
		} else if (Short[].class == targetClass) {
			Short[] newInstance = new Short[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Short.parseShort(source[j]);
			return newInstance;
		} else if (byte[].class == targetClass) {
			byte[] newInstance = new byte[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Byte.parseByte(source[j]);
			return newInstance;
		} else if (Byte[].class == targetClass) {
			Byte[] newInstance = new Byte[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Byte.parseByte(source[j]);
			return newInstance;
		} else if (float[].class == targetClass) {
			float[] newInstance = new float[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Float.parseFloat(source[j]);
			return newInstance;
		} else if (Float[].class == targetClass) {
			Float[] newInstance = new Float[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Float.parseFloat(source[j]);
			return newInstance;
		} else if (double[].class == targetClass) {
			double[] newInstance = new double[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Double.parseDouble(source[j]);
			return newInstance;
		} else if (Double[].class == targetClass) {
			Double[] newInstance = new Double[length];
			for (short j = 0; j < length; j++)
				newInstance[j] = Double.parseDouble(source[j]);
			return newInstance;
		}
		throw new ConversionException(
				"can't convert [" + Arrays.toString(source) + "] to [" + targetClass.getName() + "]");
	}

	@SuppressWarnings("unchecked")
	public final static <T> T parseArray(String source[], Class<T> targetClass) throws ConversionException {
		return (T) toArrayObject(source, targetClass);
	}

	@SuppressWarnings("unchecked")
	public final static <T extends Number> T parseNumber(String text, Class<T> targetClass) throws ConversionException {

		return (T) parseDigit(text, targetClass);
	}

	public static final Object parseDigit(String text, Class<?> targetClass) throws ConversionException {

		if (Byte.class == targetClass || byte.class == targetClass) {
			return Byte.parseByte(text);
		} else if (Short.class == targetClass || short.class == targetClass) {
			return Short.parseShort(text);
		} else if (Integer.class == targetClass || int.class == targetClass) {
			return Integer.parseInt(text);
		} else if (Long.class == targetClass || long.class == targetClass) {
			return Long.parseLong(text);
		} else if (BigInteger.class == targetClass) {
			return new BigInteger(text);
		} else if (Float.class == targetClass || float.class == targetClass) {
			return Float.parseFloat(text);
		} else if (Double.class == targetClass || double.class == targetClass) {
			return Double.parseDouble(text);
		} else if (BigDecimal.class == targetClass || Number.class == targetClass) {
			return new BigDecimal(text);
		}
		throw new ConversionException("can't convert[" + text + "] to [" + targetClass.getName() + "]");
	}


}
