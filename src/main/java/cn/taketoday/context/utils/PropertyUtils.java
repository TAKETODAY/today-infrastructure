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

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Today
 * @date 2018年7月12日 下午8:46:41
 */
@Slf4j
public abstract class PropertyUtils {

	/**
	 * find in property file
	 * 
	 * @param value_
	 * @return
	 */
	public static String findInProperties(Properties properties, String value_) {
		final String key = value_;
		if (value_.startsWith("#{") && value_.endsWith("}")) {
			value_ = properties.getProperty(value_.replaceAll("[{|#|}]+", ""));
			if (value_ == null) {
				log.error("properties file lack -> [{}] , must specify a properties value", key);
				System.exit(0);// exit
			}
		}
		return value_;
	}

}
