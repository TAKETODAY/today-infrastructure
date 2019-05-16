/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2019 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.context.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.Duration;

import cn.taketoday.context.Constant;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.io.Resource;

/**
 * 
 * @author Today <br>
 *         2018-07-12 20:43:53
 */
// @Slf4j
public abstract class ConvertUtils {

	/**
	 * Convert string to target type
	 * 
	 * @param value
	 *            value
	 * @param targetClass
	 *            targetClass
	 * @return converted object
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object convert(String value, Class<?> targetClass) {

		if (StringUtils.isEmpty(value)) {
			return value;
		}

		switch (targetClass.getSimpleName()) //
		{
			case "String" :
				return value;
			case "Byte" :
			case "byte" :
				return Byte.parseByte(value);
			case "short" :
			case "Short" :
				return Short.parseShort(value);
			case "int" :
			case "Integer" :
				return Integer.parseInt(value);
			case "long" :
			case "Long" :
				return Long.parseLong(value);
			case "float" :
			case "Float" :
				return Float.parseFloat(value);
			case "double" :
			case "Double" :
				return Double.parseDouble(value);
			case "boolean" :
			case "Boolean" :
				return Boolean.parseBoolean(value);
			case "BigInteger" :
				return new BigInteger(value);
			case "BigDecimal" :
				return new BigDecimal(value);
			case "File" : {
				if (value.startsWith(Constant.CLASS_PATH_PREFIX)) {

					final String resourceString = value.substring(Constant.CLASS_PATH_PREFIX.length());
					final URL resource = ClassUtils.getClassLoader().getResource(resourceString);

					if (resource == null) {
						throw new ContextException("ClassPath Recource: [" + resourceString + "] Doesn't exist");
					}
					return new File(resource.getPath());
				}
				return new File(value);
			}
			case "Charset" :
				return Charset.forName(value);
			case "Class" : {
				try {
					return Class.forName(value);
				}
				catch (ClassNotFoundException e) {
					throw new ContextException(e);
				}
			}
			case "Duration" : {
				return convertDuration(value);
			}
			case "DataSize" : {
				return DataSize.parse(value);
			}
		}
		// @since 2.1.6
		if (Resource.class.isAssignableFrom(targetClass)) {
			try {
				return ResourceUtils.getResource(value);
			}
			catch (IOException e) {
				return null;
			}
		}

		if (targetClass.isEnum()) {
			return Enum.valueOf((Class<Enum>) targetClass, value);
		}
		if (targetClass.isArray()) {
			final Class<?> componentType = targetClass.getComponentType();
			final String[] split = StringUtils.split(value);
			final Object arrayValue = Array.newInstance(componentType, split.length);
			for (int i = 0; i < split.length; i++) {
				Array.set(arrayValue, i, convert(split[i], componentType));
			}
			return arrayValue;
		}
		// use constructor
		try {
			return targetClass.getConstructor(String.class).newInstance(value);
		}
		catch (Throwable e) {
			//
		}
		return null;
	}

	/**
	 * Convert a string to {@link Duration}
	 * 
	 * @param value
	 * @return
	 */
	public static Duration convertDuration(String value) {

		if (value.endsWith("s")) {
			return Duration.ofSeconds(Long.parseLong(value.substring(0, value.length() - 1)));
		}
		if (value.endsWith("h")) {
			return Duration.ofHours(Long.parseLong(value.substring(0, value.length() - 1)));
		}
		if (value.endsWith("ns")) {
			return Duration.ofNanos(Long.parseLong(value.substring(0, value.length() - 2)));
		}
		if (value.endsWith("ms")) {
			return Duration.ofMillis(Long.parseLong(value.substring(0, value.length() - 2)));
		}
		if (value.endsWith("min")) {
			return Duration.ofMinutes(Long.parseLong(value.substring(0, value.length() - 3)));
		}
		if (value.endsWith("d")) {
			return Duration.ofDays(Long.parseLong(value.substring(0, value.length() - 1)));
		}

		return Duration.parse(value);
	}

}
