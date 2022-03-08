/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.config;

import java.lang.reflect.Constructor;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.InitializationBeanPostProcessor;
import cn.taketoday.beans.factory.config.BeanPostProcessor;
import cn.taketoday.beans.factory.config.InstantiationAwareBeanPostProcessor;
import cn.taketoday.lang.Nullable;

/**
 * Extension of the {@link InstantiationAwareBeanPostProcessor} interface,
 * adding a callback for predicting the eventual type of a processed bean.
 *
 * <p><b>NOTE:</b> This interface is a special purpose interface, mainly for
 * internal use within the framework. In general, application-provided
 * post-processors should simply implement the plain {@link BeanPostProcessor}
 * interface or derive from the {@link InstantiationAwareBeanPostProcessor}
 * class. New methods might be added to this interface even in point releases.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/20 17:02
 */
public interface SmartInstantiationAwareBeanPostProcessor extends InstantiationAwareBeanPostProcessor {

  /**
   * Predict the type of the bean to be eventually returned from this
   * processor's {@link #postProcessBeforeInstantiation} callback.
   * <p>The default implementation returns {@code null}.
   *
   * @param beanClass the raw class of the bean
   * @param beanName the name of the bean
   * @return the type of the bean, or {@code null} if not predictable
   * @throws BeansException in case of errors
   */
  @Nullable
  default Class<?> predictBeanType(Class<?> beanClass, String beanName) throws BeansException {
    return null;
  }

  /**
   * Determine the candidate constructors to use for the given bean.
   * <p>The default implementation returns {@code null}.
   *
   * @param beanClass the raw class of the bean (never {@code null})
   * @param beanName the name of the bean
   * @return the candidate constructors, or {@code null} if none specified
   * @throws BeansException in case of errors
   */
  @Nullable
  default Constructor<?>[] determineCandidateConstructors(Class<?> beanClass, String beanName)
          throws BeansException {

    return null;
  }

  /**
   * Obtain a reference for early access to the specified bean,
   * typically for the purpose of resolving a circular reference.
   * <p>This callback gives post-processors a chance to expose a wrapper
   * early - that is, before the target bean instance is fully initialized.
   * The exposed object should be equivalent to the what
   * {@link InitializationBeanPostProcessor#postProcessBeforeInitialization}
   * / {@link InitializationBeanPostProcessor#postProcessAfterInitialization}
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
