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

import cn.taketoday.context.Constant;
import cn.taketoday.context.exception.ConfigurationException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * 
 * @author Today <br>
 *         2018-07-12 20:46:41
 */
public abstract class PropertiesUtils {

	/**
	 * find in property file, and system property.
	 * 
	 * @param value_
	 *            the value will as a key, if don't exist return value_
	 * @return
	 * @throws ConfigurationException
	 */
	public static String findInProperties(Properties properties, String value_) throws ConfigurationException {
		return findInProperties(properties, value_, true);
	}

	/**
	 * 
	 * @param properties
	 * @param value_
	 * @param shutdown
	 * @return
	 * @throws ConfigurationException
	 */
	public static String findInProperties(Properties properties, //
			String value_, boolean shutdown) throws ConfigurationException //
	{
		final String key = value_;
		if (value_.startsWith(Constant.PLACE_HOLDER_PREFIX) && value_.endsWith(Constant.PLACE_HOLDER_SUFFIX)) {
			String replaceAll = value_.replaceAll("[{|#|}]+", "");
			value_ = properties.getProperty(replaceAll);
			if (value_ == null) {
				if (shutdown) {
					throw new ConfigurationException("Properties -> [{}] , must specify a value.",
							key.replaceAll("[{|#|}]+", ""));
				}
				return null;
			}
		}
		return value_;
	}

	/**
	 * 
	 * @param loader
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	public static final InputStream getResourceAsStream(String resource) throws IOException {

		InputStream in = ClassUtils.getClassLoader().getResourceAsStream(resource);
		if (in == null) {
			throw new IOException("Could not find resource " + resource);
		}
		return in;
	}

	/**
	 * 
	 * @param resource
	 * @return
	 * @throws IOException
	 */
	public static final Properties getResourceAsProperties(String resource) throws IOException {
		Properties props = new Properties();

		try (InputStream in = ClassUtils.getClassLoader().getResourceAsStream(resource)) {
			props.load(in);
		}

		return props;
	}

	/**
	 * 
	 * @param urlString
	 * @return
	 * @throws IOException
	 */
	public static final InputStream getUrlAsStream(String urlString) throws IOException {
		return new URL(urlString).openConnection().getInputStream();
	}

	/**
	 * 
	 * @param urlString
	 * @return
	 * @throws IOException
	 */
	public static final Properties getUrlAsProperties(String urlString) throws IOException {
		Properties props = new Properties();
		try (InputStream in = getUrlAsStream(urlString)) {
			props.load(in);
		}
		return props;
	}
}
