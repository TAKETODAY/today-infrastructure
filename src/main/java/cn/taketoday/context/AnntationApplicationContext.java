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
package cn.taketoday.context;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import cn.taketoday.context.annotation.Component;
import cn.taketoday.context.annotation.ComponentImpl;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.bean.BeanDefinition;
import cn.taketoday.context.core.Scope;
import cn.taketoday.context.exception.BeanDefinitionStoreException;
import cn.taketoday.context.exception.NoSuchBeanDefinitionException;
import cn.taketoday.context.utils.ClassUtils;
import cn.taketoday.context.utils.StringUtils;
import lombok.NonNull;

/**
 * @author Today <br>
 * 
 *         2018-09-06 13:47
 */
public class AnntationApplicationContext extends DefaultApplicationContext {

	private static final Map<String, Method> PROTOTYPES = new HashMap<>();

	/**
	 * start with given class set
	 * 
	 * @param actions
	 */
	public AnntationApplicationContext(Set<Class<?>> actions) {
		super(actions);
	}

	/**
	 * start with given package
	 * 
	 * @param package_
	 */
	public AnntationApplicationContext(String package_) {
		super(package_);
	}

	/**
	 * auto load and clear cache?
	 * 
	 * @param clear
	 */
	public AnntationApplicationContext() {
		super(false);
	}

	@Override
	public final Object getBean(String name) throws NoSuchBeanDefinitionException {

		Object bean = beanDefinitionRegistry.getSingleton(name);

		if (bean != null) {
			return bean;
		}

		BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinition(name);

		if (beanDefinition == null) {

			// class full name
			Class<?> beanType = null;
			try {

				beanType = Class.forName(name);
			} catch (ClassNotFoundException e) {
				throw new NoSuchBeanDefinitionException("No bean named " + name + " is defined");
			}

			Set<String> keySet = beanDefinitionRegistry.getBeanDefinitionsMap().keySet();
			for (String name_ : keySet) {
				BeanDefinition beanDefinition_ = beanDefinitionRegistry.getBeanDefinition(name_);
				if (beanType.isAssignableFrom(beanDefinition_.getBeanClass())) {
					beanDefinition = beanDefinition_;
					beanDefinition.setName(name);
					if (beanDefinition.isSingleton()) {
						beanDefinitionRegistry.putSingleton(name, beanDefinitionRegistry.getSingleton(name_));
					}
					break;
				}
			}
			throw new NoSuchBeanDefinitionException("No bean named " + name + " is defined");
		}
		//
		try {

			Method method = PROTOTYPES.get(name);

			if (method == null) {
				return doCreateBean(beanDefinition, name);
			}
			return method.invoke(beanDefinitionRegistry.getSingleton(beanDefinition.getName()));
		} //
		catch (Exception ex) {
			log.error("ERROR -> [{}] caused by [{}]", ex.getMessage(), ex.getCause(), ex);
		}
		return bean;
	}

	@Override
	public void loadContext(@NonNull String path, @NonNull String package_) {

		try {
			// load bean definition
			super.loadBeanDefinition(path, package_);
			// add bean post processor
			super.addBeanPostProcessor();
			// handle dependency
			this.configuration(super.registerFactoryBean());
			super.handleDependency(beanDefinitionRegistry.getBeanDefinitionsMap().entrySet());

			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	/**
	 * 
	 * @param clazz
	 */
	public void loadContext(Set<Class<?>> clazz) {
		try {

			// load bean definition
			this.loadBeanDefinition(clazz);
			// add bean post processor
			super.addBeanPostProcessor();
			// handle dependency
			this.configuration(super.registerFactoryBean());
			super.handleDependency(beanDefinitionRegistry.getBeanDefinitionsMap().entrySet());
			onRefresh();
		} //
		catch (Exception e) {
			log.error("ERROR -> [{}] caused by {}", e.getMessage(), e.getCause(), e);
		}
	}

	/**
	 * register
	 * 
	 * @param beanDefinitions
	 * @throws Exception
	 */
	private void configuration(Set<Entry<String, BeanDefinition>> beanDefinitions) throws Exception {

		for (Entry<String, BeanDefinition> beanDefinition : beanDefinitions) {

			Class<? extends Object> beanClass = beanDefinition.getValue().getBeanClass();

			if (!beanClass.isAnnotationPresent(Configuration.class)) {
				continue;
			}

			Method[] declaredMethods = beanClass.getDeclaredMethods();

			for (Method method : declaredMethods) {

				Component[] components = ClassUtils.getMethodAnntation(method, Component.class, ComponentImpl.class);

				if (components.length == 0) {
					continue;
				}

				create(method, components);
			}
		}
	}

	/**
	 * 
	 * @param method
	 * @param components
	 * @throws BeanDefinitionStoreException
	 */
	private void create(Method method, Component[] components) throws BeanDefinitionStoreException {
		for (Component component : components) {
			String[] names = component.value();
			Scope scope = component.scope();

			if (names.length == 0) {
				names = new String[] { method.getName() };
			}
			for (String name : names) {

				if (StringUtils.isEmpty(name)) {
					name = method.getName();
				}

				PROTOTYPES.put(name, method);

				BeanDefinition beanDefinition_ = new BeanDefinition()//
						.setBeanClass(method.getReturnType())//
						.setName(method.getDeclaringClass().getName())//
						.setScope(scope);
				beanDefinitionLoader.register(name, beanDefinition_);
			}
		}
	}

	@Override
	protected void doCreateSingleton() throws Exception {

		Set<String> names = beanDefinitionRegistry.getBeanDefinitionsMap().keySet();

		for (String name : names) {

			BeanDefinition beanDefinition = beanDefinitionRegistry.getBeanDefinitionsMap().get(name);

			if (!beanDefinition.isSingleton()) {
				continue;
			}

			// interface
			Class<?>[] interfaces = beanDefinition.getBeanClass().getInterfaces();
			for (Class<?> clazz : interfaces) {
				if (beanDefinitionRegistry.containsInstance(clazz.getName())) {
					beanDefinitionRegistry.putSingleton(name, beanDefinitionRegistry.getSingleton(clazz.getName()));
					continue;
				}
			}

			if (beanDefinitionRegistry.containsInstance(name)) {
				continue;// initialized
			}
			if (PROTOTYPES.containsKey(name)) {
				Method method = PROTOTYPES.get(name);

				beanDefinitionRegistry.putSingleton(name, //
						method.invoke(//
								createDeclaringObject(name, beanDefinition, beanDefinition.getName())//
						)//
				);
				PROTOTYPES.remove(name);
				continue;
			}

			// initializing singleton bean
			Object bean = initializingBean(createBeanInstance(beanDefinition), name, beanDefinition);

			beanDefinitionRegistry.putSingleton(name, bean);
		}
	}

	/**
	 * 
	 * @param name
	 * @param beanDefinition
	 * @param declaringName
	 * @return
	 * @throws Exception
	 * @throws NoSuchBeanDefinitionException
	 */
	private Object createDeclaringObject(String name, BeanDefinition beanDefinition, String declaringName)
			throws Exception, NoSuchBeanDefinitionException {
		if (beanDefinitionRegistry.containsInstance(declaringName)) {

			return beanDefinitionRegistry.getSingleton(declaringName);
		} // not initialized

		Object declaringSingleton = super.initializingBean(//
				createBeanInstance(//
						beanDefinitionRegistry.getBeanDefinition(declaringName)//
				), //
				name, //
				beanDefinition //
		);

		beanDefinitionRegistry.putSingleton(name, declaringSingleton);

		return declaringSingleton;
	}

}
