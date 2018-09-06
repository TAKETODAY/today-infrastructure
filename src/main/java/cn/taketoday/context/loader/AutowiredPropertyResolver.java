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

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.PropertyResolver;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.factory.BeanDefinitionRegistry;

/**
 * @author Today <br>
 * 	
 *		2018-08-04 15:56
 */
@PropertyResolver
public class AutowiredPropertyResolver implements PropertyValueResolver {

	@Override
	public PropertyValue resolveProperty(BeanDefinitionRegistry registry, Field field) {
		
		Autowired autowired = field.getAnnotation(Autowired.class);	//auto wired
		if (!"".equals(autowired.value())) {
			return new PropertyValue(new BeanReference(autowired.value()), field);// bean name
		} 
		
		if (!(autowired.class_() == Class.class)) {
			return new PropertyValue(new BeanReference(autowired.class_().getName()), field); // class name
		}
		
		return new PropertyValue(new BeanReference(field.getType().getName()), field); // // field type name
	}
	
}
