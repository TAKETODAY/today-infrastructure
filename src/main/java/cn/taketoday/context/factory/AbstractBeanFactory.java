/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.context.factory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.BeanReference;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.loader.BeanDefinitionLoader;

/**
 * @author Today
 * @date 2018年6月23日 上午11:20:58
 */
public abstract class AbstractBeanFactory implements BeanFactory {

	protected final Logger				log						= LoggerFactory.getLogger(AbstractBeanFactory.class);

	/**
	 * storage BeanDefinition
	 */
	protected BeanDefinitionRegistry	beanDefinitionRegistry	= new SimpleBeanDefinitionRegistry();
	/**
	 * resolve beanDefinition which in xml file or It is marked annotation
	 * 
	 */
	protected BeanDefinitionLoader		beanDefinitionLoader;
	/**
	 * Bean Post Processor
	 */
	protected List<BeanPostProcessor>	postProcessors			= new ArrayList<>();

	/**
	 * all the class in classpath
	 */
	protected Set<Class<?>>				actions					= new HashSet<>();

	@Override
	public final Object getBean(String name) throws NoSuchBeanDefinitionException {

		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);
		if (beanDefinition == null) {
			throw new IllegalArgumentException("No bean named " + name + " is defined");
		}
		Object bean = beanDefinition.getBean();
		if (bean == null) {
			try {
				bean = doCreateBean(beanDefinition, name);
			} catch (Exception ex) {
				log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
			}
		}
		return bean;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return (T) getBean(requiredType.getName());
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getBean(String name, Class<T> requiredType) throws NoSuchBeanDefinitionException {
		return (T) getBean(name);
	}

	@Override
	public boolean containsBean(String name) {
		return beanDefinitionRegistry.containsBeanDefinition(name);
	}

	@Override
	public boolean containsBean(Class<?> type) {
		return containsBean(type.getName());
	}

	@Override
	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return !isPrototype(name);
	}

	@Override
	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);
		if (beanDefinition == null) {
			log.error("no such bean exception");
			throw new NoSuchBeanDefinitionException("no such bean exception");
		}
		return !beanDefinition.isSingleton();
	}

	@Override
	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {

		BeanDefinition type = beanDefinitionRegistry.getBeanDefinition(name);

		if (type == null) {
			log.error("no such bean exception");
			throw new NoSuchBeanDefinitionException("no such bean exception");
		}
		return type.getBeanClass();
	}

	@Override
	public Set<String> getAliases(Class<?> type) {
		return beanDefinitionRegistry.getBeanDefinitionNames();
	}

	/**
	 * create bean use default constructor
	 * 
	 * @param beanDefinition
	 * @return
	 * @throws Exception
	 */
	protected Object createBeanInstance(BeanDefinition beanDefinition) throws Exception {
		return beanDefinition.getBeanClass().getConstructor().newInstance();
	}

	/**
	 * 
	 * @param bean
	 * @param propertyValues
	 * @throws Exception
	 */
	protected void applyPropertyValues(Object bean, List<PropertyValue> propertyValues) throws Exception {

		for (PropertyValue propertyValue : propertyValues) {
			Field field = propertyValue.getField();
			Object value = propertyValue.getValue();
			if (value instanceof BeanReference) {
				BeanReference beanReference = (BeanReference) value;
				value = getBean(beanReference.getName());
				propertyValue.setValue(value);
			}
			field.set(bean, value);
		}
	}

	/**
	 * create bean use default constructor
	 * 
	 * @param beanDefinition
	 * @return
	 * @throws Exception
	 */
	protected Object doCreateBean(BeanDefinition beanDefinition, String name) throws Exception {
		Object bean = createBeanInstance(beanDefinition);
		// init
		return initializingBean(bean, name, beanDefinition.getPropertyValues().getPropertyValues());
	}

	/**
	 * 
	 * set singleton bean
	 */
	protected void doCreateSingleton(BeanDefinitionRegistry beanDefinitionRegistry) throws Exception {

		Set<String> names = beanDefinitionRegistry.getBeanDefinitionsMap().keySet();
		for (String name : names) {

			BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinitionsMap().get(name);

			if (!beanDefinition.isSingleton()) {
				log.debug("bean befinition [{}] is PROTOTYPE", beanDefinition.getBeanClass().getName());
				continue;
			}
			if (beanDefinition.getBean() != null) {
				continue;
			}
			//
			Object bean = createBeanInstance(beanDefinition);
			// initializing bean
			beanDefinition.setBean(bean);
			initializingBean(bean, name, beanDefinition.getPropertyValues().getPropertyValues());
		}
	}

	/**
	 * initializing bean
	 * 
	 * @param bean
	 * @param name
	 * @param propertyValues
	 * @throws Exception
	 */
	private final Object initializingBean(Object bean, String name, List<PropertyValue> propertyValues)
			throws Exception {

		// aware
		if (bean instanceof BeanNameAware) {
			((BeanNameAware) bean).setBeanName(name);
		}
		if (bean instanceof ApplicationContextAware) {
			((ApplicationContextAware) bean).setApplicationContext((ApplicationContext) this);
		}

		// before properties
		for (BeanPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeforeInitialization(bean, name);
		}

		// apply properties
		applyPropertyValues(bean, propertyValues);

		if (bean instanceof InitializingBean) {
			((InitializingBean) bean).afterPropertiesSet();
		}

		// after properties
		for (BeanPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessAfterInitialization(bean, name);
		}

		return bean;
	}

	@Override
	public void removeBean(String name) throws NoSuchBeanDefinitionException {
		beanDefinitionRegistry.removeBeanDefinition(name);
	}

}
