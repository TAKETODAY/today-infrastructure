/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.beans.factory;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.PropertyValues;
import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanPostProcessor;

/**
 * Extension of the {@link BeanPostProcessor} interface, allowing for post-processing
 * of dependency injection before the factory applies property values to the bean.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/19 21:35</a>
 * @since 4.0
 */
public interface DependenciesBeanPostProcessor extends BeanPostProcessor {

  /**
   * Post-process the given property values before the factory applies them
   * to the given bean.
   * <p>The default implementation returns the given {@code pvs} as-is.
   *
   * @param pvs the property values that the factory is about to apply (never {@code null})
   * @param bean the bean instance created, but whose properties have not yet been set
   * @param beanName the name of the bean
   * @return the actual property values to apply to the given bean (can be the passed-in
   * PropertyValues instance), or {@code null} to skip property population
   * @throws BeansException in case of errors
   * @see BeanDefinition#isEnableDependencyInjection()
   */
  default @Nullable PropertyValues processDependencies(PropertyValues pvs, Object bean, String beanName) {
    return pvs;
  }

}
