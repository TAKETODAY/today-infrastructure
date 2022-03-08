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

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.config.BeanPostProcessor;

/**
 * Factory hook that allows for custom modification of new bean instances
 * &mdash; for example, checking for marker interfaces or wrapping beans with
 * proxies.
 * <p>
 * post process when beans initialization
 * </p>
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/19 21:23</a>
 * @since 4.0
 */
public interface InitializationBeanPostProcessor extends BeanPostProcessor {

  /**
   * Apply this {@code BeanPostProcessor} to the given new bean instance
   * <i>before</i> any bean initialization callbacks (like InitializingBean's
   * {@code afterPropertiesSet} or a custom init-method). The bean will already be
   * populated with property values. The returned bean instance may be a wrapper
   * around the original.
   * <p>
   * The default implementation returns the given {@code bean} as-is.
   *
   * @param bean The new bean instance
   * @param beanName The definition of the bean
   * @return the bean instance to use, either the original or a wrapped one; if
   * {@code null}, no subsequent BeanPostProcessors will be invoked
   * @throws BeansException in case of errors
   * @see InitializingBean#afterPropertiesSet
   */
  default Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  /**
   * Apply this {@code BeanPostProcessor} to the given new bean instance
   * <i>after</i> any bean initialization callbacks (like InitializingBean's
   * {@code afterPropertiesSet} or a custom init-method). The bean will already be
   * populated with property values. The returned bean instance may be a wrapper
   * around the original.
   *
   * <p>
   * The default implementation returns the given {@code bean} as-is.
   *
   * @param bean the new bean instance, fully initialized
   * @param beanName the definition of the bean
   * @return the bean instance to use, either the original or a wrapped one; if
   * {@code null}, no subsequent BeanPostProcessors will be invoked
   * @throws BeansException in case of errors
   * @see InitializingBean#afterPropertiesSet
   * @see FactoryBean
   */
  default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

}
