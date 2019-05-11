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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */
package cn.taketoday.context.factory;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.AnnotationAttributes;
import cn.taketoday.context.BeanNameCreator;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.MissingBean;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.aware.ApplicationContextAware;
import cn.taketoday.context.aware.Aware;
import cn.taketoday.context.aware.BeanFactoryAware;
import cn.taketoday.context.aware.BeanNameAware;
import cn.taketoday.context.aware.EnvironmentAware;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.bean.DefaultBeanDefinition;
import cn.taketoday.context.bean.PropertyValue;
import cn.taketoday.context.bean.StandardBeanDefinition;
import cn.taketoday.context.event.LoadingMissingBeanEvent;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.ContextException;
import cn.taketoday.context.loader.BeanDefinitionLoader;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.ContextUtils;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2019-03-23 15:00
 */
public class StandardBeanFactory extends AbstractBeanFactory implements ConfigurableBeanFactory {

	private final Collection<Method> missingMethods = new HashSet<>(32, 1.0f);

	private final AbstractApplicationContext applicationContext;

	/** resolve beanDefinition */
	private BeanDefinitionLoader beanDefinitionLoader;

	public StandardBeanFactory(AbstractApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	protected void aware(Object bean, String name) {

		if (bean instanceof Aware) {
			// aware
			if (bean instanceof BeanNameAware) {
				((BeanNameAware) bean).setBeanName(name);
			}
			if (bean instanceof ApplicationContextAware) {
				((ApplicationContextAware) bean).setApplicationContext(applicationContext);
			}
			if (bean instanceof BeanFactoryAware) {
				((BeanFactoryAware) bean).setBeanFactory(this);
			}
			if (bean instanceof EnvironmentAware) {
				((EnvironmentAware) bean).setEnvironment(applicationContext.getEnvironment());
			}
		}
	}

	/**
	 * If {@link BeanDefinition} is {@link StandardBeanDefinition} will create bean
	 * from {@link StandardBeanDefinition#getFactoryMethod()}
	 */
	@Override
	protected Object createBeanInstance(BeanDefinition beanDefinition) throws Throwable {
		final Object bean = getSingleton(beanDefinition.getName());

		if (bean == null) {
			if (beanDefinition instanceof StandardBeanDefinition) {
				final StandardBeanDefinition standardBeanDefinition = (StandardBeanDefinition) beanDefinition;
				final Method factoryMethod = standardBeanDefinition.getFactoryMethod();

				return factoryMethod.invoke(getDeclaringInstance(standardBeanDefinition.getDeclaringName()), //
						ContextUtils.resolveParameter(factoryMethod, this)//
				);
			}
			return ClassUtils.newInstance(beanDefinition, this);
		}
		return bean;
	}

	/**
	 * 
	 */
	@Override
	protected Object doCreate(String currentBeanName, BeanDefinition currentBeanDefinition) throws Throwable {
		// fix: #3 when get annotated beans that StandardBeanDefinition missed
		if (currentBeanDefinition instanceof StandardBeanDefinition) {
			return initializeSingleton(currentBeanName, currentBeanDefinition);
		}
		return super.doCreate(currentBeanName, currentBeanDefinition);
	}

	// -----------------------------------------

	/**
	 * Get declaring instance
	 * 
	 * @param declaringName
	 *            declaring name
	 * @return
	 * @throws Throwable
	 */
	private Object getDeclaringInstance(String declaringName) throws Throwable {
		BeanDefinition declaringBeanDefinition = getBeanDefinition(declaringName);

		if (declaringBeanDefinition.isInitialized()) {
			return getSingleton(declaringName);
		}

		// fix: declaring bean not initialized
		final Object declaringSingleton = super.initializingBean(//
				createBeanInstance(declaringBeanDefinition), declaringName, declaringBeanDefinition//
		);

		// put declaring object
		if (declaringBeanDefinition.isSingleton()) {
			registerSingleton(declaringName, declaringSingleton);
			declaringBeanDefinition.setInitialized(true);
		}
		return declaringSingleton;
	}

	/**
	 * Resolve bean from a class which annotated with @{@link Configuration}
	 * 
	 * @throws Throwable
	 *             when exception occurred
	 */
	public void loadConfigurationBeans() {

		for (Entry<String, BeanDefinition> entry : getBeanDefinitionsMap().entrySet()) {

			final BeanDefinition beanDefinition = entry.getValue();

			final Class<? extends Object> beanClass = beanDefinition.getBeanClass();
			if (!beanClass.isAnnotationPresent(Configuration.class)) {
				continue; // not a @Configuration bean
			}

			for (Method method : beanClass.getDeclaredMethods()) {

				if (!ContextUtils.conditional(method, applicationContext)) {
					continue; // @Profile
				}

				Collection<AnnotationAttributes> components = ClassUtils.getAnnotationAttributes(method, Component.class);

				if (components.isEmpty()) {
					if (method.isAnnotationPresent(MissingBean.class)) {
						missingMethods.add(method);
					}
					continue;
				}
				doRegisterDefinition(method, components);
			}
		}
	}

	/**
	 * Create bean definition, and register it
	 *
	 * @param method
	 *            factory method
	 * @param components
	 *            {@link AnnotationAttributes}
	 */
	private final void doRegisterDefinition(Method method, Collection<AnnotationAttributes> components) //
			throws BeanDefinitionStoreException //
	{

		final Class<?> returnType = method.getReturnType();
		final BeanNameCreator beanNameCreator = getBeanNameCreator();
		final BeanDefinitionLoader beanDefinitionLoader = getBeanDefinitionLoader();

		final String defaultBeanName = beanNameCreator.create(returnType);
		final String declaringBeanName = beanNameCreator.create(method.getDeclaringClass());

		for (final AnnotationAttributes component : components) {
			final Scope scope = component.getEnum(Constant.SCOPE);
			final String[] initMethods = component.getStringArray(Constant.INIT_METHODS);
			final String[] destroyMethods = component.getStringArray(Constant.DESTROY_METHODS);

			for (final String name : ContextUtils.findNames(defaultBeanName, component.getStringArray(Constant.VALUE))) {

				// register
				StandardBeanDefinition beanDefinition = new StandardBeanDefinition();
				beanDefinition.setName(name);//
				beanDefinition.setScope(scope);
				beanDefinition.setBeanClass(returnType);//
				beanDefinition.setDestroyMethods(destroyMethods);//
				beanDefinition.setInitMethods(ContextUtils.resolveInitMethod(returnType, initMethods));//
				beanDefinition.setPropertyValues(ContextUtils.resolvePropertyValue(returnType, applicationContext));

				beanDefinition.setDeclaringName(declaringBeanName)//
						.setFactoryMethod(method);
				// resolve @Props on a bean
				ContextUtils.resolveProps(beanDefinition, applicationContext.getEnvironment());

				beanDefinitionLoader.register(name, beanDefinition);
			}
		}
	}

	/**
	 * Load missing beans, default beans
	 * 
	 * @param beanClasses
	 */
	public final void loadMissingBean(Collection<Class<?>> beanClasses) {

		applicationContext.publishEvent(new LoadingMissingBeanEvent(applicationContext, beanClasses));

		final BeanNameCreator beanNameCreator = getBeanNameCreator();
		final BeanDefinitionLoader beanDefinitionLoader = getBeanDefinitionLoader();

		for (final Class<?> beanClass : beanClasses) {

			final MissingBean missingBean = beanClass.getAnnotation(MissingBean.class);

			if (ContextUtils.isMissedBean(missingBean, beanClass, this)) {
				registerMissingBean(beanDefinitionLoader, beanNameCreator, missingBean, beanClass, new DefaultBeanDefinition());
			}
		}

		for (final Method method : missingMethods) {

			final Class<?> beanClass = method.getReturnType();
			final MissingBean missingBean = method.getAnnotation(MissingBean.class);

			if (ContextUtils.isMissedBean(missingBean, beanClass, this)) {

				// @Configuration use default bean name
				StandardBeanDefinition beanDefinition = new StandardBeanDefinition()//
						.setFactoryMethod(method)//
						.setDeclaringName(beanNameCreator.create(method.getDeclaringClass()));

				if (method.isAnnotationPresent(Props.class)) {
					// @Props on method
					final List<PropertyValue> resolvedProps = //
							ContextUtils.resolveProps(method, applicationContext.getEnvironment().getProperties());

					if (!resolvedProps.isEmpty()) {
						beanDefinition.addPropertyValue(resolvedProps);
					}
				}
				registerMissingBean(beanDefinitionLoader, beanNameCreator, missingBean, beanClass, beanDefinition);
			}
		}
		missingMethods.clear();
	}

	private final void registerMissingBean(final BeanDefinitionLoader beanDefinitionLoader, //
			final BeanNameCreator beanNameCreator, final MissingBean missingBean, //
			final Class<?> beanClass, final BeanDefinition beanDefinition) //
	{
		String beanName = missingBean.value();
		if (StringUtils.isEmpty(beanName)) {
			beanName = beanNameCreator.create(beanClass);
		}

		beanDefinition.setName(beanName);
		beanDefinition.setBeanClass(beanClass)//
				.setScope(missingBean.scope())//
				.setDestroyMethods(missingBean.destroyMethods())//
				.setInitMethods(ContextUtils.resolveInitMethod(beanClass, missingBean.initMethods()))//
				.setPropertyValues(ContextUtils.resolvePropertyValue(beanClass, applicationContext));

		ContextUtils.resolveProps(beanDefinition, applicationContext.getEnvironment());

		// register missed bean
		beanDefinitionLoader.register(beanName, beanDefinition);
	}

	@Override
	public BeanDefinitionLoader getBeanDefinitionLoader() {
		if (beanDefinitionLoader == null) {
			try {
				// fix: when manually load context some properties can't be loaded
				// not initialize
				applicationContext.loadContext(new HashSet<>());
			}
			catch (Throwable e) {
				throw new ContextException(e);
			}
		}
		return beanDefinitionLoader;
	}

	public void setBeanDefinitionLoader(BeanDefinitionLoader beanDefinitionLoader) {
		this.beanDefinitionLoader = beanDefinitionLoader;
	}

}
