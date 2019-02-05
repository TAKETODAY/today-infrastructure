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
package cn.taketoday.context.loader;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

/**
 * @author Today <br>
 * 
 *         2018-08-04 16:01
 */
public class PropsPropertyResolver implements PropertyValueResolver {

	/**
	 * Resolve {@link Props} annotation property.
	 */
	@Override
	public PropertyValue resolveProperty(ApplicationContext applicationContext, Field field) {

		// Must be a Map
		if (!Properties.class.equals(field.getType()) && !Map.class.equals(field.getType())) {
			throw new AnnotationException("Field: [" + field + "] type must be: [" + //
					Properties.class.getName() + "] or [" + Map.class.getName() + "]");
		}

		Props props = field.getAnnotation(Props.class);
		Map<Object, Object> properties = new Properties(); // property value
		Properties properties_ = new Properties(); // file to be load
		ClassLoader classLoader = ClassUtils.getClassLoader();

		for (String fileName : props.value()) {

			try (InputStream inputStream = new FileInputStream(classLoader.getResource(checkName(fileName)).getPath())) {

				properties_.load(inputStream);
				this.load(props, properties, properties_);
			}
			catch (IOException e) {
				throw new ContextException(e);
			}
		}
		if (props.value().length == 0) {
			this.load(props, properties, applicationContext.getEnvironment().getProperties());
		}

		return new PropertyValue(properties, field);
	}

	/**
	 * load properties values.
	 * 
	 * @param props
	 *            Props annotation
	 * @param properties
	 *            property value
	 * @param pool
	 *            all property
	 */
	private void load(Props props, Map<Object, Object> properties, Properties pool) {

		String[] prefix = props.prefix();
		boolean replace = props.replace();

		try {

			for (Entry<Object, Object> entry : pool.entrySet()) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				if (key instanceof String) {
					for (String prefix_ : prefix) {
						if (((String) key).startsWith(prefix_)) { // start with prefix

							if (replace) {
								// replace the prefix
								key = ((String) key).replaceFirst(prefix_, "");
							}
							properties.put((String) key, ContextUtils.resolvePlaceholder(pool, (String) value));
						}
					}
				}
			}
		}
		catch (ConfigurationException e) {
			// shutdown
		}
	}

	/**
	 * get file name.
	 * 
	 * @param fileName
	 *            input file name
	 * @return standard file name
	 */
	private final String checkName(String fileName) {
		return fileName.endsWith(Constant.PROPERTIES_SUFFIX) ? fileName : fileName + Constant.PROPERTIES_SUFFIX;
	}

}
