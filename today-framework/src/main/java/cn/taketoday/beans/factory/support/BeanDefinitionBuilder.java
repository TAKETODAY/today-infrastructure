/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.beans.factory.support;

import java.util.function.Supplier;

import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.config.AutowiredPropertyMarker;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.BeanDefinitionCustomizer;
import cn.taketoday.beans.factory.config.RuntimeBeanReference;
import cn.taketoday.beans.factory.xml.NamespaceHandler;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * Programmatic means of constructing {@link BeanDefinition BeanDefinitions}
 * using the builder pattern. Intended primarily for use when implementing
 * {@link NamespaceHandler NamespaceHandlers}.
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author TODAY 2021/10/2 22:45
 * @since 4.0
 */
public class BeanDefinitionBuilder {

  /**
   * The {@code BeanDefinition} instance we are creating.
   */
  private final AbstractBeanDefinition beanDefinition;

  /**
   * Our current position with respect to constructor args.
   */
  private int constructorArgIndex;

  /**
   * Enforce the use of factory methods.
   */
  private BeanDefinitionBuilder(AbstractBeanDefinition beanDefinition) {
    this.beanDefinition = beanDefinition;
  }

  /**
   * Return the current BeanDefinition object in its raw (unvalidated) form.
   *
   * @see #getBeanDefinition()
   */
  public AbstractBeanDefinition getRawBeanDefinition() {
    return this.beanDefinition;
  }

  /**
   * Validate and return the created BeanDefinition object.
   */
  public AbstractBeanDefinition getBeanDefinition() {
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
   *
   * @param factoryMethod the name of the factory method
   * @param factoryBean the name of the bean to call the specified factory method on
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
   *
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

  public BeanDefinitionBuilder propertyValues(PropertyValues propertyValues) {
    beanDefinition.getPropertyValues().add(propertyValues);
    return this;
  }

  /**
   * Add a reference to the specified bean name under the property specified.
   *
   * @param name the name of the property to add the reference to
   * @param beanName the name of the bean being referenced
   */
  public BeanDefinitionBuilder addPropertyReference(String name, String beanName) {
    this.beanDefinition.getPropertyValues().add(name, new RuntimeBeanReference(beanName));
    return this;
  }

  /**
   * Add an autowired marker for the specified property on the specified bean.
   *
   * @param name the name of the property to mark as autowired
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
   *
   * @see BeanDefinition#SCOPE_SINGLETON
   * @see BeanDefinition#SCOPE_PROTOTYPE
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
   */
  public BeanDefinitionBuilder setPrimary(boolean primary) {
    this.beanDefinition.setPrimary(primary);
    return this;
  }

  /**
   * Set the role of this definition.
   */
  public BeanDefinitionBuilder setRole(int role) {
    this.beanDefinition.setRole(role);
    return this;
  }

  /**
   * Set whether this bean is 'synthetic', that is, not defined by
   * the application itself.
   */
  public BeanDefinitionBuilder setSynthetic(boolean synthetic) {
    this.beanDefinition.setSynthetic(synthetic);
    return this;
  }

  /**
   * Apply the given customizers to the underlying bean definition.
   */
  public BeanDefinitionBuilder applyCustomizers(BeanDefinitionCustomizer... customizers) {
    for (BeanDefinitionCustomizer customizer : customizers) {
      customizer.customize(this.beanDefinition);
    }
    return this;
  }

  //---------------------------------------------------------------------
  // static utils
  //---------------------------------------------------------------------

  public static String defaultBeanName(String className) {
    return StringUtils.uncapitalize(ClassUtils.getSimpleName(className));
  }

  public static String defaultBeanName(Class<?> clazz) {
    String simpleName = clazz.getSimpleName();
    return StringUtils.uncapitalize(simpleName);
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
   */
  public static BeanDefinitionBuilder genericBeanDefinition() {
    return new BeanDefinitionBuilder(new GenericBeanDefinition());
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
   *
   * @param beanClassName the class name for the bean that the definition is being created for
   */
  public static BeanDefinitionBuilder genericBeanDefinition(String beanClassName) {
    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
    builder.beanDefinition.setBeanClassName(beanClassName);
    return builder;
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
   *
   * @param beanClass the {@code Class} of the bean that the definition is being created for
   */
  public static BeanDefinitionBuilder genericBeanDefinition(Class<?> beanClass) {
    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
    builder.beanDefinition.setBeanClass(beanClass);
    return builder;
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link GenericBeanDefinition}.
   *
   * @param beanClass the {@code Class} of the bean that the definition is being created for
   * @param instanceSupplier a callback for creating an instance of the bean
   */
  public static <T> BeanDefinitionBuilder genericBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new GenericBeanDefinition());
    builder.beanDefinition.setBeanClass(beanClass);
    builder.beanDefinition.setInstanceSupplier(instanceSupplier);
    return builder;
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
   *
   * @param beanClassName the class name for the bean that the definition is being created for
   */
  public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName) {
    return rootBeanDefinition(beanClassName, null);
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
   *
   * @param beanClassName the class name for the bean that the definition is being created for
   * @param factoryMethodName the name of the method to use to construct the bean instance
   */
  public static BeanDefinitionBuilder rootBeanDefinition(String beanClassName, @Nullable String factoryMethodName) {
    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
    builder.beanDefinition.setBeanClassName(beanClassName);
    builder.beanDefinition.setFactoryMethodName(factoryMethodName);
    return builder;
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
   *
   * @param beanClass the {@code Class} of the bean that the definition is being created for
   */
  public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass) {
    return rootBeanDefinition(beanClass, (String) null);
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
   *
   * @param beanClass the {@code Class} of the bean that the definition is being created for
   * @param factoryMethodName the name of the method to use to construct the bean instance
   */
  public static BeanDefinitionBuilder rootBeanDefinition(Class<?> beanClass, @Nullable String factoryMethodName) {
    BeanDefinitionBuilder builder = new BeanDefinitionBuilder(new RootBeanDefinition());
    builder.beanDefinition.setBeanClass(beanClass);
    builder.beanDefinition.setFactoryMethodName(factoryMethodName);
    return builder;
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
   *
   * @param beanType the {@link ResolvableType type} of the bean that the definition is being created for
   * @param instanceSupplier a callback for creating an instance of the bean
   */
  public static <T> BeanDefinitionBuilder rootBeanDefinition(ResolvableType beanType, Supplier<T> instanceSupplier) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(beanType);
    beanDefinition.setInstanceSupplier(instanceSupplier);
    return new BeanDefinitionBuilder(beanDefinition);
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link RootBeanDefinition}.
   *
   * @param beanClass the {@code Class} of the bean that the definition is being created for
   * @param instanceSupplier a callback for creating an instance of the bean
   * @see #rootBeanDefinition(ResolvableType, Supplier)
   */
  public static <T> BeanDefinitionBuilder rootBeanDefinition(Class<T> beanClass, Supplier<T> instanceSupplier) {
    return rootBeanDefinition(ResolvableType.fromClass(beanClass), instanceSupplier);
  }

  /**
   * Create a new {@code BeanDefinitionBuilder} used to construct a {@link ChildBeanDefinition}.
   *
   * @param parentName the name of the parent bean
   */
  public static BeanDefinitionBuilder childBeanDefinition(String parentName) {
    return new BeanDefinitionBuilder(new ChildBeanDefinition(parentName));
  }

}
