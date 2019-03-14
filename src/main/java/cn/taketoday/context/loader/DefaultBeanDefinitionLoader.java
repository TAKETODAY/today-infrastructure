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

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;

import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.Constant;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.env.ConfigurableEnvironment;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ConfigurationException;
import cn.taketoday.context.factory.BeanDefinitionRegistry;
import cn.taketoday.context.factory.BeanFactory;
import cn.taketoday.context.factory.FactoryBean;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.ExceptionUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * Default Bean Definition Loader implements
 * 
 * @author Today <br>
 *         2018-06-23 11:18:22
 */
public class DefaultBeanDefinitionLoader implements BeanDefinitionLoader {

	private final BeanDefinitionRegistry registry;
	/** bean definition registry */
	private final ConfigurableApplicationContext applicationContext;
	/**	 */
	private final BeanNameCreator beanNameCreator;

	public DefaultBeanDefinitionLoader(ConfigurableApplicationContext applicationContext) {

		this.applicationContext = //
				Objects.requireNonNull(applicationContext, "applicationContext can't be null");

		ConfigurableEnvironment environment = applicationContext.getEnvironment();

		this.registry = environment.getBeanDefinitionRegistry();
		this.beanNameCreator = environment.getBeanNameCreator();
	}

	@Override
	public BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	@Override
	public void loadBeanDefinition(Class<?> beanClass) throws BeanDefinitionStoreException {

		if (Modifier.isAbstract(beanClass.getModifiers())) {
			return; // don't load abstract class
		}
		try {
			
			if (ContextUtils.conditional(beanClass, applicationContext)) {
				register(beanClass);
			}
		}
		catch (Throwable ex) {
			throw new BeanDefinitionStoreException(ExceptionUtils.unwrapThrowable(ex));
		}
	}

	@Override
	public void loadBeanDefinitions(Collection<Class<?>> beans) throws BeanDefinitionStoreException {
		for (Class<?> clazz : beans) {
			loadBeanDefinition(clazz);
		}
	}

	@Override
	public void loadBeanDefinition(String name, Class<?> beanClass) throws BeanDefinitionStoreException {
		// register
		try {

			final Collection<AnnotationAttributes> annotationAttributes = ClassUtils.getAnnotationAttributes(beanClass, Component.class);
			if (annotationAttributes.isEmpty()) {
				register(name, build(beanClass, null, name));
			}
			else {
				for (AnnotationAttributes attributes : annotationAttributes) {
					register(name, build(beanClass, attributes, name));
				}
			}
		}
		catch (Throwable e) {
			throw new BeanDefinitionStoreException(ExceptionUtils.unwrapThrowable(e));
		}
	}

	/**
	 * Register with given class
	 * 
	 * @param beanClass
	 *            bean class
	 * @throws BeanDefinitionStoreException
	 */
	@Override
	public void register(Class<?> beanClass) throws BeanDefinitionStoreException {
		try {
			
			Collection<AnnotationAttributes> annotationAttributes = ClassUtils.getAnnotationAttributes(beanClass, Component.class);
			if (annotationAttributes.isEmpty()) {
				return;
			}
			
			final String defaultBeanName = beanNameCreator.create(beanClass);
			for (AnnotationAttributes attributes : annotationAttributes) {
				for (String name : ContextUtils.findNames(defaultBeanName, attributes.getStringArray(Constant.VALUE))) {
					if (!applicationContext.containsBeanDefinition(name)) {
						register(name, build(beanClass, attributes, name));
					}
				}
			}
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			throw new ConfigurationException(//
					"An Exception Occurred When Build Bean Definition: [{}], With Msg: [{}] ", //
					beanClass, ex.getMessage(), ex//
			);
		}
	}

	/**
	 * Build a bean definition
	 * 
	 * @param beanClass
	 *            given bean class
	 * @param attributes
	 *            {@link AnnotationAttributes}
	 * @param name
	 *            bean name
	 * @return
	 * @throws Throwable
	 */
	private BeanDefinition build(Class<?> beanClass, AnnotationAttributes attributes, String name) throws Throwable {

		final BeanDefinition beanDefinition = new DefaultBeanDefinition(name, beanClass);//
		if (attributes == null) {
			beanDefinition.setDestroyMethods(new String[0])//
					.setInitMethods(ContextUtils.resolveInitMethod(beanClass));//
		}
		else {
			beanDefinition.setScope(attributes.getEnum(Constant.SCOPE))//
					.setDestroyMethods(attributes.getStringArray(Constant.DESTROY_METHODS))//
					.setInitMethods(ContextUtils.resolveInitMethod(beanClass, attributes.getStringArray(Constant.INIT_METHODS)));
		}

		beanDefinition.setPropertyValues(ContextUtils.resolvePropertyValue(beanClass, this.applicationContext));
		// fix missing @Props injection
		ContextUtils.resolveProps(beanDefinition, this.applicationContext.getEnvironment());

		return beanDefinition;
	}

	/**
	 * Register bean definition with given name
	 * 
	 * @param name
	 *            bean name
	 * @param beanDefinition
	 *            definition
	 * @throws BeanDefinitionStoreException
	 */
	@Override
	public void register(String name, final BeanDefinition beanDefinition) throws BeanDefinitionStoreException {

		try {

			ContextUtils.validateBeanDefinition(beanDefinition, applicationContext);

			if (FactoryBean.class.isAssignableFrom(beanDefinition.getBeanClass())) {
				// process FactoryBean
				name = registerFactoryBean(name, beanDefinition);
			}
			registry.registerBeanDefinition(name, beanDefinition);
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			throw new BeanDefinitionStoreException("An Exception Occurred When Register Bean Definition: [{}], With Msg: [{}]", //
					name, ex.getMessage(), ex);
		}
	}

	/**
	 * If bean definition is a {@link FactoryBean} register its factory's instance
	 * 
	 * @param beanName
	 *            old bean name
	 * @param beanDefinition
	 *            definition
	 * @return returns a new bean name
	 * @throws Throwable
	 */
	private String registerFactoryBean(String beanName, BeanDefinition beanDefinition) throws Throwable {

		final ConfigurableApplicationContext applicationContext = this.applicationContext;

		FactoryBean<?> $factoryBean = //
				(FactoryBean<?>) applicationContext.getSingleton(BeanFactory.FACTORY_BEAN_PREFIX + beanName);
		boolean registed = true;
		if ($factoryBean == null) { // If not exist declaring instance, create it
			$factoryBean = (FactoryBean<?>) ClassUtils.newInstance(beanDefinition.getBeanClass()); // declaring object
			// not initialized
			registed = false;
		}

		beanName = $factoryBean.getBeanName();
		if (StringUtils.isEmpty(beanName)) {
			beanName = beanNameCreator.create($factoryBean.getBeanClass());
		}

		beanDefinition.setFactoryBean(true);
		if (StringUtils.isEmpty(beanDefinition.getName())) {
			beanDefinition.setName(beanName);
		}

		if (!registed) {// register it
			applicationContext.registerSingleton(beanName, $factoryBean);
		}
		return beanName;
	}

	@Override
	public BeanDefinition createBeanDefinition(Class<?> beanClass) {

		final BeanDefinition beanDefinition = //
				new DefaultBeanDefinition(beanNameCreator.create(beanClass), beanClass)//
						.setDestroyMethods(new String[0])//
						.setAbstract(Modifier.isAbstract(beanClass.getModifiers()))//
						.setFactoryBean(FactoryBean.class.isAssignableFrom(beanClass));

		try {

			ContextUtils.resolveProps(beanDefinition, applicationContext.getEnvironment());

			// process property and init methods
			return beanDefinition.setInitMethods(ContextUtils.resolveInitMethod(beanClass))//
					.setPropertyValues(ContextUtils.resolvePropertyValue(beanClass, this.applicationContext));
		}
		catch (Throwable ex) {
			ex = ExceptionUtils.unwrapThrowable(ex);
			throw new BeanDefinitionStoreException(//
					"An Exception Occurred When Create A Bean Definition, With Msg: [{}]", //
					ex.getMessage(), ex//
			);
		}
	}

}
