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

import cn.taketoday.context.annotation.Autowired;
import cn.taketoday.context.annotation.PropertyResolver;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.utils.StringUtils;

import java.lang.reflect.Field;

import javax.annotation.Resource;

/**
 * @author Today <br>
 * 
 *         2018-08-04 15:56
 */
@PropertyResolver({ Autowired.class, Resource.class })
public class AutowiredPropertyResolver implements PropertyValueResolver {

	@Override
	public PropertyValue resolveProperty(BeanDefinitionRegistry registry, Field field) {

		Autowired autowired = field.getAnnotation(Autowired.class); // auto wired
		
		boolean required = true;
		String name = field.getType().getSimpleName();

		if (autowired != null) {
			if (StringUtils.isNotEmpty(autowired.value())) {
				name = autowired.value();
			}else if (!(autowired.class_() == Class.class)) {
				name = autowired.class_().getName();//full class name
			}
			required = autowired.required(); // class name
		}
		// Resource.class
		Resource resource = field.getAnnotation(Resource.class);
		if (resource != null) {
			if (StringUtils.isNotEmpty(resource.name())){
				name = resource.name();
			}else if (!(resource.type() == Object.class)) {
				// Type's simple name
				name = resource.type().getSimpleName();
			}
		}

		return new PropertyValue(new BeanReference(name).setRequired(required), field); // // field type name
	}

}
