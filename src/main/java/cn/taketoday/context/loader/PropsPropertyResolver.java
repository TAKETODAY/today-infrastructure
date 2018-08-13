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

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;

import cn.taketoday.context.annotation.PropertyResolver;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.PropertiesUtils;

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
			throw new AnnotationException("field type must be -> [" + Properties.class.getName() + "]");
		}

		Props props = field.getAnnotation(Props.class);
		Properties properties = new Properties(); // property vlaue
		Properties properties_ = new Properties(); // file to be load
		ClassLoader classLoader = ClassUtils.getClassLoader();

		InputStream inputStream = null;
		try {
			String[] prefix = props.prefix();
			for (String fileName : props.value()) {
				inputStream = new FileInputStream(classLoader.getResource(checkName(fileName)).getPath());
				
				properties_.load(inputStream);
				this.load(prefix, properties, properties_);
			}
			if (props.value().length == 0) {

				properties_ = registry.getProperties();
				this.load(prefix, properties, properties_);
			}
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
		return new PropertyValue(properties, field);
	}

	/**
	 * load property value.
	 * 
	 * @param prefix
	 *            the property key's prefix
	 * @param properties
	 *            property value
	 * @param properties_
	 *            property pool
	 */
	private void load(String[] prefix, Properties properties, Properties properties_) {
		
		properties_.forEach((key, value) -> {
			if (key instanceof String) {
				for (String prefix_ : prefix) {
					if (((String) key).startsWith(prefix_)) {
						try {
							properties.setProperty((String) key, PropertiesUtils.findInProperties(properties_, (String)value));
						} catch (ConfigurationException e) {
							//shutdown
						}
					}
				}
			}
		});
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
