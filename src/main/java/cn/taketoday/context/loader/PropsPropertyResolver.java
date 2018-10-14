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
package cn.taketoday.context.loader;

import cn.taketoday.context.annotation.PropertyResolver;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.PropertiesUtils;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * @author Today <br>
 * 
 *         2018-08-04 16:01
 */
@PropertyResolver(Props.class)
public class PropsPropertyResolver implements PropertyValueResolver {

	/**
	 * resolve {@link Props} annotation property.
	 */
	@Override
	public PropertyValue resolveProperty(BeanDefinitionRegistry registry, Field field) throws Exception {

		if (!Properties.class.equals(field.getType())) {
			throw new AnnotationException("Field type must be -> [" + Properties.class.getName() + "]");
		}
		
		Props props = field.getAnnotation(Props.class);
		Properties properties = new Properties(); // property vlaue
		Properties properties_ = new Properties(); // file to be load
		ClassLoader classLoader = ClassUtils.getClassLoader();

		for (String fileName : props.value()) {
			
			try (InputStream inputStream = new FileInputStream(
					classLoader.getResource(checkName(fileName)).getPath())) {
				
				properties_.load(inputStream);
				this.load(props, properties, properties_);
			}
		}
		if (props.value().length == 0) {

			properties_ = registry.getProperties();
			this.load(props, properties, properties_);
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
	 *            property pool
	 */
	private void load(Props props, Properties properties, Properties pool) {

		String[] prefix = props.prefix();
		boolean replace = props.replace();

		Set<Entry<Object, Object>> entrySet = pool.entrySet();

		try {

			for (Entry<Object, Object> entry : entrySet) {
				Object key = entry.getKey();
				Object value = entry.getValue();
				if (key instanceof String) {
					for (String prefix_ : prefix) {
						if (((String) key).startsWith(prefix_)) {

							if (replace) {
								// replace the prefix
								key = ((String) key).replaceFirst(prefix_, "");
							}

							properties.setProperty((String) key,
									PropertiesUtils.findInProperties(pool, (String) value));
						}
					}
				}
			}
		} catch (ConfigurationException e) {
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
		return fileName.endsWith(".properties") ? fileName : fileName + ".properties";
	}

}
