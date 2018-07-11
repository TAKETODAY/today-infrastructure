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

/**
 * @author Today
 * @date 2018年6月26日 下午9:19:09
 */
public abstract class StringUtil {

	/**
	 * 判断是否是空
	 * 
	 * @param str
	 * @return
	 */
	public final static boolean isEmpty(String str) {
		return (str == null || "".equals(str.trim()));
	}

	/**
	 * 判断是否不是空
	 * 
	 * @param str
	 * @return
	 */
	public final static boolean isNotEmpty(String str) {
		return (str != null) && !"".equals(str.trim());
	}

	public static int toInt(final String str, final int defaultValue) {
		if (str == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(str);
		} catch (final NumberFormatException nfe) {
			return defaultValue;
		}
	}

}