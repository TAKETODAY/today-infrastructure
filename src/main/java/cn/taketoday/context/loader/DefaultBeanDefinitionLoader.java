/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://yanghaijian.top
 * Copyright © Today & 2017 - 2018 All Rights Reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.taketoday.context.annotation.ActionProcessor;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Prototype;
import cn.taketoday.context.annotation.Repository;
import cn.taketoday.context.annotation.RestProcessor;
import cn.taketoday.context.annotation.Service;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.core.Scope;
import cn.taketoday.context.exception.AnnotationException;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import lombok.extern.slf4j.Slf4j;

/**
 * Default Bean Definition Loader implements
 * 
 * @author Today <br>
 *         2018-06-23 11:18:22
 */
@Slf4j
public final class DefaultBeanDefinitionLoader implements BeanDefinitionLoader {

	/** bean definition registry */
	private BeanDefinitionRegistry	registry;
	/** load property */
	private PropertyValuesLoader	propertyValuesLoader;

	public DefaultBeanDefinitionLoader(BeanDefinitionRegistry registry) {
		this.registry = registry;
		propertyValuesLoader = new PropertyValuesLoader(registry);
		propertyValuesLoader.init();
	}
	
	@Override
	public BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	@Override
	public void loadBeanDefinition(Class<?> clazz) throws BeanDefinitionStoreException {
		
		if (clazz.isInterface()) {
			return;
		}
		register(clazz);
	}

	@Override
	public void loadBeanDefinitions(Set<Class<?>> beans) throws BeanDefinitionStoreException {

		for (Class<?> clazz : beans) {
			loadBeanDefinition(clazz);
		}
	}

	@Override
	public void loadBeanDefinition(String name, Class<?> clazz) throws BeanDefinitionStoreException {

		if (clazz.isInterface()) {
			log.error("class -> [{}] can't be interface", clazz.getName());
			throw new BeanDefinitionStoreException("class -> " + clazz.getName() + "can't be interface");
		}
		// register
		register(name, new BeanDefinition(name, clazz));
	}

	/**
	 * register with given class
	 * 
	 * @param clazz
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 */
	private void register(Class<?> clazz) throws BeanDefinitionStoreException {

		Map<String, BeanDefinition> beanDefinitions = new HashMap<>();
		
		build(beanDefinitions, clazz);
		
		Set<Entry<String, BeanDefinition>> entrySet = beanDefinitions.entrySet();
		Iterator<Entry<String, BeanDefinition>> iterator = entrySet.iterator();
		while (iterator.hasNext()) {
			Entry<String, BeanDefinition> entry = iterator.next();
			register(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * register bean definition with given name.
	 * 
	 * @param name
	 *            bean name
	 * @param beanDefinition
	 *            definition
	 * @throws BeanDefinitionStoreException
	 */
	private void register(String name, BeanDefinition beanDefinition) throws BeanDefinitionStoreException {
		try {

			Class<? extends Object> beanClass = beanDefinition.getBeanClass();
			// find same property.
			Collection<BeanDefinition> values = registry.getBeanDefinitionsMap().values();
			for (BeanDefinition beanDefinition_ : values) {
				if (beanDefinition_.getBeanClass() == beanClass) {
					// have same property value.
					beanDefinition.setPropertyValues(beanDefinition_.getPropertyValues());
					registry.registerBeanDefinition(name, beanDefinition);
					return;
				}
			}
			// process property
			beanDefinition.setPropertyValues(processProperty(beanDefinition));
			registry.registerBeanDefinition(name, beanDefinition);
		} 
		catch (Exception e) {
			log.error("process property error.", e);
		}
	}

	/**
	 * build definition map with given class
	 * 
	 * @param map
	 *            bean definition map
	 * @param clazz
	 *            bean class
	 */
	private void build(Map<String, BeanDefinition> map, Class<?> clazz) {

		Service service = clazz.getAnnotation(Service.class);
		Singleton singleton = clazz.getAnnotation(Singleton.class);
		Component component = clazz.getAnnotation(Component.class);
		Prototype prototype = clazz.getAnnotation(Prototype.class);
		Repository repository = clazz.getAnnotation(Repository.class);
		// Processor
		RestProcessor restProcessor = clazz.getAnnotation(RestProcessor.class);
		ActionProcessor actionProcessor = clazz.getAnnotation(ActionProcessor.class);
		
		if (service != null) {
			String[] names_ = findNames(clazz, service.value());
			for (String name : names_) {
				map.put(name, new BeanDefinition(name, clazz));
			}
		}
		if (singleton != null) {
			String[] names_ = findNames(clazz, singleton.value());
			for (String name : names_) {
				map.put(name, new BeanDefinition(name, clazz));
			}
		}
		if (prototype != null) {
			String[] names_ = findNames(clazz, prototype.value());
			for (String name : names_) {
				map.put(name, new BeanDefinition(name, clazz, Scope.PROTOTYPE));
			}
		}
		if (repository != null) {
			String[] names_ = findNames(clazz, repository.value());
			for (String name : names_) {
				map.put(name, new BeanDefinition(name, clazz));
			}
		}
		if (restProcessor != null) {
			String[] names_ = findNames(clazz,
					"".equals(restProcessor.value()) ? new String[] { clazz.getSimpleName() } : new String[] { restProcessor.value() });
			for (String name : names_) {
				map.put(name, new BeanDefinition(name, clazz));
			}
		}
		if (actionProcessor != null) {
			String[] names_ = findNames(clazz,
					"".equals(actionProcessor.value()) ? new String[] { clazz.getSimpleName() } : new String[] { actionProcessor.value() });
			for (String name : names_) {
				map.put(name, new BeanDefinition(name, clazz));
			}
		}
		if (component != null) {
			String[] names_ = findNames(clazz, component.value());
			for (String name : names_) {
				map.put(name, new BeanDefinition(name, clazz, component.scope()));
			}
		}
		return;
	}

	/**
	 * find names
	 * 
	 * @param clazz
	 *            bean class
	 * @param names
	 *            annotation values
	 * @return
	 */
	private String[] findNames(Class<?> clazz, String[] names) {
		if (names.length == 0) {
			return new String[] { clazz.getName() };
		}
		return names;
	}

	/**
	 * process bean's property (field)
	 * 
	 * @param beanDefinition
	 *            bean definition
	 * @return property value list
	 * @throws Exception
	 */
	private PropertyValue[] processProperty(BeanDefinition beanDefinition) throws Exception {

		Class<?> clazz = beanDefinition.getBeanClass();
		List<PropertyValue> propertyValues = new ArrayList<>();
		Field[] declaredFields = clazz.getDeclaredFields();

		// self
		try {
			for (Field field : declaredFields) {
				// supports?
				if (!propertyValuesLoader.supportsProperty(field)) {
					continue;
				}
				field.setAccessible(true);
				propertyValues.add(propertyValuesLoader.create(field));
			}
		} catch (AnnotationException ex) {
			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		}
		
		// superclass
		Class<?> superclass = clazz.getSuperclass();
		Field[] declaredFields_ = superclass.getDeclaredFields();
		for (Field field : declaredFields_) {
			if (!propertyValuesLoader.supportsProperty(field)) {
				continue;
			}
			field.setAccessible(true);
			try {
				propertyValues.add(propertyValuesLoader.create(field));
			} catch (AnnotationException ex) {
				log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
			}
		}
		return propertyValues.toArray(new PropertyValue[0]);
	}

}
