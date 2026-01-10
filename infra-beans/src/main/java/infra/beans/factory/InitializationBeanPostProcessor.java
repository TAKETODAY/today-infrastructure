/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.config.BeanPostProcessor;

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
  @Nullable
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
  @Nullable
  default Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

}
