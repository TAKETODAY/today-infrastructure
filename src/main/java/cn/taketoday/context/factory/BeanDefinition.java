/**
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
package cn.taketoday.context.factory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collection;

import cn.taketoday.context.AttributeAccessor;
import cn.taketoday.context.Constant;
import cn.taketoday.context.Scope;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.context.exception.NoSuchPropertyException;

/**
 * Bean definition
 *
 * @author TODAY <br>
 * 2018-06-23 11:23:45
 */
public interface BeanDefinition extends AnnotatedElement, AttributeAccessor {

  Method[] EMPTY_METHOD = Constant.EMPTY_METHOD_ARRAY;

  PropertyValue[] EMPTY_PROPERTY_VALUE = Constant.EMPTY_PROPERTY_VALUE;

  /**
   * Get a property
   *
   * @param name
   *         The name of property
   *
   * @return Property value object
   *
   * @throws NoSuchPropertyException
   *         If there is no property with given name
   */
  PropertyValue getPropertyValue(String name) throws NoSuchPropertyException;

  /**
   * Indicates that If the bean is a {@link Singleton}.
   *
   * @return If the bean is a {@link Singleton}.
   */
  boolean isSingleton();

  /**
   * Indicates that If the bean is a
   * {@link cn.taketoday.context.annotation.Prototype Prototype}.
   *
   * @return If the bean is a {@link cn.taketoday.context.annotation.Prototype
   * Prototype}.
   *
   * @since 2.17
   */
  boolean isPrototype();

  /**
   * Get bean class
   *
   * @return bean class
   */
  Class<?> getBeanClass();

  /**
   * Get init methods
   *
   * @return Get all the init methods, never be null
   */
  Method[] getInitMethods();

  /**
   * Get all the destroy methods name
   *
   * @return all the destroy methods name, never be null
   */
  String[] getDestroyMethods();

  /**
   * Get Bean {@link Scope}
   *
   * @return Bean {@link Scope}
   */
  String getScope();

  /**
   * Get bean name
   *
   * @return Bean name
   */
  String getName();

  /**
   * If bean is a {@link FactoryBean}
   *
   * @return If Bean is a {@link FactoryBean}
   */
  boolean isFactoryBean();

  /**
   * If a {@link Singleton} has initialized
   * <p>
   * If this bean is created from {@link FactoryBean} This explains
   * {@link FactoryBean} is initialized
   *
   * @return If Bean or {@link FactoryBean} is initialized
   */
  boolean isInitialized();

  /**
   * if it is from abstract class.
   *
   * @return if it is from abstract class
   *
   * @see #getChild()
   */
  boolean isAbstract();

  /**
   * Get all the {@link PropertyValue}s
   *
   * @return The bean's all {@link PropertyValue}
   */
  PropertyValue[] getPropertyValues();

  // ----------------- Configurable

  /**
   * Add PropertyValue to list.
   *
   * @param propertyValues
   *         {@link PropertyValue} object
   */
  void addPropertyValue(PropertyValue... propertyValues);

  /**
   * Add a collection of {@link PropertyValue}s
   *
   * @param propertyValues
   *         The {@link Collection} of {@link PropertyValue}s
   */
  void addPropertyValue(Collection<PropertyValue> propertyValues);

  /**
   * Apply bean If its initialized
   *
   * @param initialized
   *         The state of bean
   *
   * @return The {@link BeanDefinition}
   */
  BeanDefinition setInitialized(boolean initialized);

  /**
   * Apply bean' name
   *
   * @param name
   *         The bean's name
   *
   * @return The {@link BeanDefinition}
   */
  BeanDefinition setName(String name);

  /**
   * Apply bean' scope
   *
   * @param scope
   *         The scope of the bean
   *
   * @return The {@link BeanDefinition}
   *
   * @see Scope#PROTOTYPE
   * @see Scope#SINGLETON
   */
  BeanDefinition setScope(String scope);

  /**
   * Apply bean' initialize {@link Method}s
   *
   * @param initMethods
   *         The array of the bean's initialize {@link Method}s
   *
   * @return The {@link BeanDefinition}
   */
  BeanDefinition setInitMethods(Method... initMethods);

  /**
   * Apply bean' destroy {@link Method}s
   *
   * @param destroyMethods
   *         The array of the bean's destroy {@link Method}s
   *
   * @return The {@link BeanDefinition}
   */
  BeanDefinition setDestroyMethods(String... destroyMethods);

  /**
   * Apply bean' {@link PropertyValue}s
   *
   * @param propertyValues
   *         The array of the bean's {@link PropertyValue}s
   *
   * @return The {@link BeanDefinition}
   */
  BeanDefinition setPropertyValues(PropertyValue... propertyValues);

  /**
   * Indicates that If the bean is a {@link FactoryBean}.
   *
   * @param factoryBean
   *         If its a {@link FactoryBean}
   *
   * @return The {@link BeanDefinition}
   */
  BeanDefinition setFactoryBean(boolean factoryBean);

  /**
   * If An {@link Annotation} present on this bean
   *
   * @param annotation
   *         target {@link Annotation}
   *
   * @return If An {@link Annotation} present on this bean
   *
   * @since 2.1.7
   */
  @Override
  boolean isAnnotationPresent(Class<? extends Annotation> annotation);

  /**
   * Indicates that the abstract bean's child implementation
   *
   * @return Child implementation bean, returns {@code null} indicates that this
   * {@link BeanDefinition} is not abstract
   *
   * @since 2.1.7
   */
  BeanDefinition getChild();

  /**
   * Apply bean' initialize {@link Method}s
   *
   * @param initMethods
   *         The array of the bean's initialize {@link Method} names
   *
   * @return The {@link BeanDefinition}
   */
  BeanDefinition setInitMethods(String... initMethods);

  /**
   * new bean instance
   *
   * @param factory
   *         input bean factory
   *
   * @return new bean instance
   *
   * @since 3.0
   */
  Object newInstance(BeanFactory factory);

  /**
   * new bean instance
   *
   * @param factory
   *         input bean factory
   * @param args
   *         arguments to use when creating a corresponding instance
   *
   * @return new bean instance
   *
   * @since 3.0
   */
  Object newInstance(BeanFactory factory, Object... args);

  /**
   * Set whether this bean should be lazily initialized.
   * <p>If {@code false}, the bean will get instantiated on startup by bean
   * factories that perform eager initialization of singletons.
   * @since 3.0
   */
  void setLazyInit(boolean lazyInit);

  /**
   * Return whether this bean should be lazily initialized, i.e. not
   * eagerly instantiated on startup. Only applicable to a singleton bean.
   *
   * @since 3.0
   */
  boolean isLazyInit();

}
