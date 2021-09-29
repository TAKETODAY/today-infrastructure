/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.beans.factory;

import cn.taketoday.core.Nullable;

/**
 * Programmatic means of constructing {@link BeanDefinition} using the builder pattern.
 * @since 4.0
 * @author TODAY 2021/9/29 10:38
 */
public class BeanDefinitionBuilder {

	/**
	 * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
	 * @param beanClass the {@code Class} of the bean that the definition is being created for
	 */
	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass) {
		return rootBeanDefinition(beanClass, (String) null);
	}

	public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass, @Nullable String factoryMethodName) {
		BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
		builder.beanDefinition.setBeanClass(beanClass);
		builder.beanDefinition.setFactoryMethodName(factoryMethodName);
		return builder;
	}

	/**
	 * The {@code BeanDefinition} instance we are creating.
	 */
	private final BeanDefinition beanDefinition;

	/**
	 * Our current position with respect to constructor args.
	 */
	private int constructorArgIndex;

	private BeanDefinitionBuilder(BeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
	}

	/**
	 * Return the current BeanDefinition object in its raw (unvalidated) form.
	 * @see #getBeanDefinition()
	 */
	public BeanDefinition getRawBeanDefinition() {
		return this.beanDefinition;
	}

	/**
	 * Validate and return the created BeanDefinition object.
	 */
	public BeanDefinition getBeanDefinition() {
		this.beanDefinition.validate();
		return this.beanDefinition;
	}

	/**
	 * Set the name of the parent definition of this bean definition.
	 */
	public BeanDefinitionBuilder setParentName(String parentName) {
		this.beanDefinition.setParentName(parentName);
		return this;
	}

	/**
	 * Set the name of a static factory method to use for this definition,
	 * to be called on this bean's class.
	 */
	public BeanDefinitionBuilder setFactoryMethod(String factoryMethod) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		return this;
	}

	/**
	 * Set the name of a non-static factory method to use for this definition,
	 * including the bean name of the factory instance to call the method on.
	 * @param factoryMethod the name of the factory method
	 * @param factoryBean the name of the bean to call the specified factory method on
	 * @since 4.3.6
	 */
	public BeanDefinitionBuilder setFactoryMethodOnBean(String factoryMethod, String factoryBean) {
		this.beanDefinition.setFactoryMethodName(factoryMethod);
		this.beanDefinition.setFactoryBeanName(factoryBean);
		return this;
	}

	/**
	 * Add an indexed constructor arg value. The current index is tracked internally
	 * and all additions are at the present point.
	 */
	public BeanDefinitionBuilder addConstructorArgValue(@Nullable Object value) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, value);
		return this;
	}

	/**
	 * Add a reference to a named bean as a constructor arg.
	 * @see #addConstructorArgValue(Object)
	 */
	public BeanDefinitionBuilder addConstructorArgReference(String beanName) {
		this.beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(
				this.constructorArgIndex++, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * Add the supplied property value under the given property name.
	 */
	public BeanDefinitionBuilder addPropertyValue(String name, @Nullable Object value) {
		this.beanDefinition.getPropertyValues().add(name, value);
		return this;
	}

	/**
	 * Add a reference to the specified bean name under the property specified.
	 * @param name the name of the property to add the reference to
	 * @param beanName the name of the bean being referenced
	 */
	public BeanDefinitionBuilder addPropertyReference(String name, String beanName) {
		this.beanDefinition.getPropertyValues().add(name, new RuntimeBeanReference(beanName));
		return this;
	}

	/**
	 * Add an autowired marker for the specified property on the specified bean.
	 * @param name the name of the property to mark as autowired
	 * @since 5.2
	 * @see AutowiredPropertyMarker
	 */
	public BeanDefinitionBuilder addAutowiredProperty(String name) {
		this.beanDefinition.getPropertyValues().add(name, AutowiredPropertyMarker.INSTANCE);
		return this;
	}

	/**
	 * Set the init method for this definition.
	 */
	public BeanDefinitionBuilder setInitMethodName(@Nullable String methodName) {
		this.beanDefinition.setInitMethodName(methodName);
		return this;
	}

	/**
	 * Set the destroy method for this definition.
	 */
	public BeanDefinitionBuilder setDestroyMethodName(@Nullable String methodName) {
		this.beanDefinition.setDestroyMethodName(methodName);
		return this;
	}


	/**
	 * Set the scope of this definition.
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_SINGLETON
	 * @see org.springframework.beans.factory.config.BeanDefinition#SCOPE_PROTOTYPE
	 */
	public BeanDefinitionBuilder setScope(@Nullable String scope) {
		this.beanDefinition.setScope(scope);
		return this;
	}

	/**
	 * Set whether or not this definition is abstract.
	 */
	public BeanDefinitionBuilder setAbstract(boolean flag) {
		this.beanDefinition.setAbstract(flag);
		return this;
	}

	/**
	 * Set whether beans for this definition should be lazily initialized or not.
	 */
	public BeanDefinitionBuilder setLazyInit(boolean lazy) {
		this.beanDefinition.setLazyInit(lazy);
		return this;
	}

	/**
	 * Set the autowire mode for this definition.
	 */
	public BeanDefinitionBuilder setAutowireMode(int autowireMode) {
		this.beanDefinition.setAutowireMode(autowireMode);
		return this;
	}

	/**
	 * Set the dependency check mode for this definition.
	 */
	public BeanDefinitionBuilder setDependencyCheck(int dependencyCheck) {
		this.beanDefinition.setDependencyCheck(dependencyCheck);
		return this;
	}

	/**
	 * Append the specified bean name to the list of beans that this definition
	 * depends on.
	 */
	public BeanDefinitionBuilder addDependsOn(String beanName) {
		if (this.beanDefinition.getDependsOn() == null) {
			this.beanDefinition.setDependsOn(beanName);
		}
		else {
			String[] added = ObjectUtils.addObjectToArray(this.beanDefinition.getDependsOn(), beanName);
			this.beanDefinition.setDependsOn(added);
		}
		return this;
	}

	/**
	 * Set whether this bean is a primary autowire candidate.
	 * @since 5.1.11
	 */
	public BeanDefinitionBuilder setPrimary(boolean primary) {
		this.beanDefinition.setPrimary(primary);
		return this;
	}



}
