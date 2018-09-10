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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.context.annotation.PropertyResolver;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.utils.ClassUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * 
 * @author Today <br>
 * 
 *         2018-06-23 11:29:23
 */
@Slf4j
public class PropertyValuesLoader {

	/** bean definition registry */
	private BeanDefinitionRegistry									registry;

	private Map<Class<? extends Annotation>, PropertyValueResolver>	propertyValueResolvers	= new HashMap<>();

	public PropertyValuesLoader(BeanDefinitionRegistry registry) {
		this.registry = registry;
	}

	/**
	 * supports property?
	 * 
	 * @param field
	 * @return
	 */
	public boolean supportsProperty(Field field) {
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if (propertyValueResolvers.containsKey(annotation.annotationType())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * auto load property resolver.
	 * 
	 * @param actions
	 */
	public void init() {
		log.debug("Start loading property resolver.");

		try {

			Collection<Class<?>> classes = ClassUtils.getClasses(PropertyResolver.class);

			for (Class<?> clazz : classes) {
				if (clazz.isInterface()) {
					continue;
				}
				propertyValueResolvers.put(//
						clazz.getAnnotation(PropertyResolver.class).value(),
						(PropertyValueResolver) clazz.getConstructor().newInstance()//
				);// put
			}
		} catch (Exception ex) {
			log.error("Initialized ERROR -> [{}] caused by {}", ex.getMessage(), ex.getCause(), ex);
		}
	}

	/**
	 * create property value
	 * 
	 * @param field
	 *              property
	 * @return
	 * @throws Exception
	 */
	public PropertyValue create(Field field) throws Exception {
		Annotation[] annotations = field.getAnnotations();
		for (Annotation annotation : annotations) {
			if (!propertyValueResolvers.containsKey(annotation.annotationType())) {
				continue;
			}
			return propertyValueResolvers.get(annotation.annotationType()).resolveProperty(registry, field);
		}
		throw new AnnotationException("Without regulation annotation present.");
	}

}
