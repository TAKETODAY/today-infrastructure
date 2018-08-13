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

import java.lang.reflect.Field;

import cn.taketoday.context.annotation.PropertyResolver;
import cn.taketoday.context.annotation.Value;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.utils.ConvertUtils;
import cn.taketoday.context.utils.PropertiesUtils;

/**
 * @author Today <br>
 * 	
 *		2018-08-04 15:58
 */
@PropertyResolver(Value.class)
public class ValuePropertyResolver implements PropertyValueResolver{

	/**
	 * resolve {@link Value} annotation property.
	 */
	@Override
	public PropertyValue resolveProperty(BeanDefinitionRegistry registry, Field field) throws Exception {
		
		String value_ = null;
		
		String value = field.getAnnotation(Value.class).value();
		if (!"".equals(value)) {
			
			value_ = PropertiesUtils.findInProperties(registry.getProperties(), value);
			//if it contains an identifier
			value_ = PropertiesUtils.findInProperties(registry.getProperties(), value_);

		} else {// use field name
			
			value_ = registry.getProperties().getProperty(field.getName());
		}
		
		return new PropertyValue(ConvertUtils.convert(value_, field.getType()), field);
	}

	
	
	
}
