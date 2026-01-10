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

package infra.beans.factory.config;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.support.AbstractBeanDefinition;

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
   * @see AbstractBeanDefinition#getBeanClass()
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

}
