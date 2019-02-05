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
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.StringUtils;

import java.lang.reflect.Field;
import java.util.Properties;

/**
 * @author Today <br>
 * 
 *         2018-08-04 15:58
 */
public class ValuePropertyResolver implements PropertyValueResolver {

	/**
	 * Resolve {@link Value} annotation property.
	 */
	@Override
	public PropertyValue resolveProperty(ApplicationContext applicationContext, Field field) {

		String value_ = null;

		Value annotation = field.getAnnotation(Value.class);
		String value = annotation.value();

		Properties properties = applicationContext.getEnvironment().getProperties();
		if (StringUtils.isNotEmpty(value)) {
			value_ = ContextUtils.resolvePlaceholder(properties, value, annotation.required());
		}
		else {// use field name
			value_ = properties.getProperty(field.getName());
		}

		if (value_ == null) {
			return null;
		}
		return new PropertyValue(ConvertUtils.convert(value_, field.getType()), field);
	}

}
