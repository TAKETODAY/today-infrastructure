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
package cn.taketoday.beans.factory;

import java.util.Set;

import cn.taketoday.beans.DisposableBean;

/**
 * Extension of the {@link BeanFactory} interface to be implemented
 * by bean factories that are capable of autowiring, provided that
 * they want to expose this functionality for existing bean instances.
 *
 * @author TODAY 2020/9/13 10:54
 */
public interface AutowireCapableBeanFactory extends BeanFactory {

  //-------------------------------------------------------------------------
  // Typical methods for creating and populating external bean instances
  //-------------------------------------------------------------------------

  /**
   * Fully create a new bean instance of the given class.
   * <p>
   * Performs full initialization of the bean, including all applicable
   * {@link BeanPostProcessor BeanPostProcessors}.
   * <p>
   * Note: This is intended for creating a fresh instance, populating annotated
   * fields and methods as well as applying all standard bean initialization
   * callbacks.
   *
   * @param beanClass the class of the bean to create
   * @return the new bean instance
   * @throws BeansException if instantiation or wiring failed
   */
  default <T> T createBean(Class<T> beanClass) throws BeansException {
    return createBean(beanClass, false);
  }

  /**
   * Fully create a new bean instance of the given class
   * <p>
   * Performs full initialization of the bean, including all applicable
   * {@link BeanPostProcessor BeanPostProcessors}.
   *
   * @param beanClass the class of the bean to create
   * @param cacheBeanDef cache bean definition
   * @return the new bean instance
   * @throws BeansException if instantiation or wiring failed
   */
  <T> T createBean(Class<T> beanClass, boolean cacheBeanDef) throws BeansException;

  /**
   * Populate the given bean instance through applying after-instantiation callbacks
   *
   * <p>Note: This is essentially intended for (re-)populating annotated fields and
   * methods, either for new instances or for deserialized instances.
   *
   * @param existingBean the existing bean instance
   * @throws BeansException if wiring failed
   */
  void autowireBean(Object existingBean) throws BeansException;

  /**
   * Instantiate a new bean instance of the given class . All constants defined
   * in this interface are supported here.
   * Can also be invoked with {@code AUTOWIRE_NO} in order to just apply
   * before-instantiation callbacks (e.g. for annotation-driven injection).
   * <p>Does <i>not</i> apply standard {@link BeanPostProcessor BeanPostProcessors}
   * callbacks or perform any further initialization of the bean. This interface
   * offers distinct, fine-grained operations for those purposes, for example
   * {@link #initializeBean}. However, {@link InstantiationAwareBeanPostProcessor}
   * callbacks are applied, if applicable to the construction of the instance.
   *
   * @param beanClass the class of the bean to instantiate
   * @return the new bean instance
   * @throws BeansException if instantiation or wiring failed
   * @see #initializeBean
   * @see #applyBeanPostProcessorsBeforeInitialization
   * @see #applyBeanPostProcessorsAfterInitialization
   */
  Object autowire(Class<?> beanClass) throws BeansException;

  /**
   * Configure the given raw bean: autowiring bean properties, applying
   * bean property values, applying factory callbacks such as {@code setBeanName}
   * and {@code setBeanFactory}, and also applying all bean post processors
   * (including ones which might wrap the given raw bean).
   * <p>This is effectively a superset of what {@link #initializeBean} provides,
   * fully applying the configuration specified by the corresponding bean definition.
   * <b>Note: This method requires a bean definition for the given name!</b>
   *
   * @param existingBean the existing bean instance
   * @param beanName the name of the bean, to be passed to it if necessary
   * (a bean definition of that name has to be available)
   * @return the bean instance to use, either the original or a wrapped one
   * @throws NoSuchBeanDefinitionException if there is no bean definition with the given name
   * @throws BeansException if the initialization failed
   * @see #initializeBean
   */
  Object configureBean(Object existingBean, String beanName) throws BeansException;

  /**
   * Initialize the given raw bean, applying factory callbacks such as
   * {@code setBeanName} and {@code setBeanFactory}, also applying all bean post
   * processors (including ones which might wrap the given raw bean).
   * <p>
   * Note that no bean definition of the given name has to exist in the bean
   * factory. The passed-in bean name will simply be used for callbacks but not
   * checked against the registered bean definitions.
   *
   * @param existingBean the existing bean instance
   * @param beanName the name of the bean
   * @return the bean instance to use, either the original or a wrapped one
   * @throws BeanInitializingException if the initialization failed
   */
  Object initializeBean(Object existingBean, String beanName)
          throws BeanInitializingException;

  /**
   * Initialize the given raw bean, applying factory callbacks such as
   * {@code setBeanName} and {@code setBeanFactory}, also applying all bean post
   * processors (including ones which might wrap the given raw bean).
   * <p>
   *
   * @param existingBean the existing bean instance
   * @return the bean instance to use, either the original or a wrapped one
   * @throws BeanInitializingException if the initialization failed
   * @see #initializeBean(Object, String)
   */
  Object initializeBean(Object existingBean) throws BeanInitializingException;

  /**
   * Fully initialize the given raw bean, applying factory callbacks such as
   * {@code setBeanName} and {@code setBeanFactory}, also applying all bean post
   * processors (including ones which might wrap the given raw bean).
   * <p>
   * Note that no bean definition of the given name has to exist in the bean
   * factory. The passed-in bean name will simply be used for callbacks but not
   * checked against the registered bean definitions.
   *
   * @param existingBean the existing bean instance
   * @param def the bean def of the bean
   * @return the bean instance to use, either the original or a wrapped one
   * @throws BeanInitializingException if the initialization failed
   */
  Object initializeBean(final Object existingBean, final BeanDefinition def)
          throws BeanInitializingException;

  /**
   * Apply {@link BeanPostProcessor BeanPostProcessors} to the given existing bean
   * instance, invoking their {@code postProcessBeforeInitialization} methods. The
   * returned bean instance may be a wrapper around the original.
   *
   * @param existingBean the existing bean instance
   * @param beanName the name of the bean
   * @throws BeanInitializingException if any post-processing failed
   * @see BeanPostProcessor#postProcessBeforeInitialization
   */
  Object applyBeanPostProcessorsBeforeInitialization(Object existingBean, String beanName)
          throws BeanInitializingException;

  /**
   * Apply {@link BeanPostProcessor BeanPostProcessors} to the given existing bean
   * instance, invoking their {@code postProcessAfterInitialization} methods. The
   * returned bean instance may be a wrapper around the original.
   *
   * @param existingBean the existing bean instance
   * @param beanName the name of the bean
   * @throws BeanInitializingException if any post-processing failed
   * @see BeanPostProcessor#postProcessAfterInitialization
   */
  Object applyBeanPostProcessorsAfterInitialization(Object existingBean, String beanName)
          throws BeanInitializingException;

  /**
   * Destroy the given bean instance (typically coming from {@link #createBean}),
   * applying the {@link DisposableBean} contract as well as registered
   * {@link DestructionBeanPostProcessor DestructionBeanPostProcessor}.
   * <p>
   * Any exception that arises during destruction should be caught and logged
   * instead of propagated to the caller of this method.
   *
   * @param existingBean the bean instance to destroy
   */
  void destroyBean(Object existingBean);

  /**
   * Resolve the bean instance that uniquely matches the given object type, if any,
   * including its bean name.
   * <p>This is effectively a variant of {@link #getBean(Class)} which preserves the
   * bean name of the matching instance.
   *
   * @param requiredType type the bean must match; can be an interface or superclass
   * @return the bean name plus bean instance
   * @throws NoSuchBeanDefinitionException if no matching bean was found
   * @throws NoUniqueBeanException if more than one matching bean was found
   * @throws BeansException if the bean could not be created
   * @see #getBean(Class)
   * @since 4.0
   */
  <T> NamedBeanHolder<T> resolveNamedBean(Class<T> requiredType) throws BeansException;

  /**
   * @since 4.0
   */
  String getPrimaryCandidate(Set<String> candidateNames, Class<?> requiredType);

}
