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

/**
 * 
 * @author Today <br>
 *         2018-07-12 20:43:53
 */
// @Slf4j
public abstract class ConvertUtils {

	
	/**
	 * convert string to target type
	 * 
	 * @param value
	 *            value
	 * @param targetClass
	 *            targetClass
	 * @return converted object
	 */
	public final static Object convert(String value, Class<?> targetClass) {

		if (targetClass.isPrimitive()) {
			switch (targetClass.getName()) //
			{
				case "byte":
					return Byte.parseByte(value);
				case "short":
					return Short.parseShort(value);
				case "int":
					return Integer.parseInt(value);
				case "long":
					return Long.parseLong(value);
				case "float":
					return Float.parseFloat(value);
				case "double":
					return Double.parseDouble(value);
				case "boolean":
					return Boolean.parseBoolean(value);
			}
		}

		if (String.class == targetClass) {
			return value;
		} else if (Byte.class == targetClass) {
			return Byte.parseByte(value);
		} else if (Short.class == targetClass) {
			return Short.parseShort(value);
		} else if (Integer.class == targetClass) {
			return Integer.parseInt(value);
		} else if (Long.class == targetClass) {
			return Long.parseLong(value);
		} else if (BigInteger.class == targetClass) {
			return new BigInteger(value);
		} else if (Float.class == targetClass) {
			return Float.parseFloat(value);
		} else if (Double.class == targetClass) {
			return Double.parseDouble(value);
		} else if (BigDecimal.class == targetClass) {
			return new BigDecimal(value);
		} else if (targetClass == Boolean.class) {
			return Boolean.parseBoolean(value);
		}

		return value;
	}

}
