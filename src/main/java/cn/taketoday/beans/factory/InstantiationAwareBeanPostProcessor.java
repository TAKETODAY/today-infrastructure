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

import cn.taketoday.lang.Nullable;

/**
 * Sub-interface of {@link BeanPostProcessor} that adds a before-instantiation callback,
 * and a callback after instantiation but before explicit properties are set or
 * autowiring occurs.
 *
 * <p>Typically used to suppress default instantiation for specific target beans,
 * for example to create proxies with special TargetSources (pooling targets,
 * lazily initializing targets, etc), or to implement additional injection strategies
 * such as field injection.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. It is recommended to implement the plain
 * {@link BeanPostProcessor} interface as far as possible.
 *
 * @author TODAY 2021/2/1 14:42
 * @since 3.0
 */
public interface InstantiationAwareBeanPostProcessor extends BeanPostProcessor {

  /**
   * Apply this BeanPostProcessor <i>before the target bean gets instantiated</i>.
   * The returned bean object may be a proxy to use instead of the target bean,
   * effectively suppressing default instantiation of the target bean.
   * <p>If a non-null object is returned by this method, the bean creation process
   * will be short-circuited. The only further processing applied is the
   * {@link InitializationBeanPostProcessor#postProcessAfterInitialization} callback
   * from the configured {@link BeanPostProcessor BeanPostProcessors}.
   * <p>This callback will be applied to bean definitions with their bean class,
   * as well as to factory-method definitions in which case the returned bean type
   * will be passed in here.
   *
   * @param beanClass the class of the bean to be instantiated
   * @param beanName the name of the bean
   * @return the bean object to expose instead of a default instance of the target bean,
   * or {@code null} to proceed with default instantiation
   * @throws BeansException in case of errors
   * @see BeanDefinition#getBeanClass()
   */
  @Nullable
  default Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
    return null;
  }

  /**
   * Perform operations after the bean has been instantiated, via a constructor or factory method,
   * but before Bean property population (from explicit properties or autowiring) occurs.
   * <p>This is the ideal callback for performing custom field injection on the given bean
   * instance, right before autowiring kicks in.
   * <p>The default implementation returns {@code true}.
   *
   * @param bean the bean instance created, with properties not having been set yet
   * @param beanName the name of the bean
   * @return {@code true} if properties should be set on the bean; {@code false}
   * if property population should be skipped. Normal implementations should return {@code true}.
   * Returning {@code false} will also prevent any subsequent InstantiationAwareBeanPostProcessor
   * instances being invoked on this bean instance.
   * @throws BeansException in case of errors
   * @see #postProcessBeforeInstantiation
   * @since 4.0
   */
  default boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException {
    return true;
  }

  /**
   * Obtain a reference for early access to the specified bean,
   * typically for the purpose of resolving a circular reference.
   * <p>This callback gives post-processors a chance to expose a wrapper
   * early - that is, before the target bean instance is fully initialized.
   * The exposed object should be equivalent to the what
   * {@link InitializationBeanPostProcessor#postProcessBeforeInitialization} /
   * {@link InitializationBeanPostProcessor#postProcessAfterInitialization}
   * would expose otherwise. Note that the object returned by this method will
   * be used as bean reference unless the post-processor returns a different
   * wrapper from said post-process callbacks. In other words: Those post-process
   * callbacks may either eventually expose the same reference or alternatively
   * return the raw bean instance from those subsequent callbacks (if the wrapper
   * for the affected bean has been built for a call to this method already,
   * it will be exposes as final bean reference by default).
   * <p>The default implementation returns the given {@code bean} as-is.
   *
   * @param bean the raw bean instance
   * @param beanName the name of the bean
   * @return the object to expose as bean reference
   * (typically with the passed-in bean instance as default)
   * @throws BeansException in case of errors
   */
  default Object getEarlyBeanReference(Object bean, String beanName) throws BeansException {
    return bean;
  }

}
