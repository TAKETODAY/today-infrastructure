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

import infra.beans.factory.config.ConfigurableBeanFactory;

/**
 * Sub-interface implemented by bean factories that can be part
 * of a hierarchy.
 *
 * <p>The corresponding {@code setParentBeanFactory} method for bean
 * factories that allow setting the parent in a configurable
 * fashion can be found in the ConfigurableBeanFactory interface.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author TODAY 2021/9/28 21:21
 * @see ConfigurableBeanFactory#setParentBeanFactory
 * @since 4.0
 */
public interface HierarchicalBeanFactory extends BeanFactory {

  /**
   * Return the parent bean factory, or {@code null} if there is none.
   */
  @Nullable
  BeanFactory getParentBeanFactory();

  /**
   * Return whether the local bean factory contains a bean of the given name,
   * ignoring beans defined in ancestor contexts.
   * <p>This is an alternative to {@code containsBean}, ignoring a bean
   * of the given name from an ancestor bean factory.
   *
   * @param name the name of the bean to query
   * @return whether a bean with the given name is defined in the local factory
   * @see BeanFactory#containsBean
   */
  boolean containsLocalBean(String name);

}
