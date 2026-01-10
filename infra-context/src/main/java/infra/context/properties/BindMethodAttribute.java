/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.properties;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.support.BeanDefinitionRegistry;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.properties.bind.BindMethod;
import infra.core.AttributeAccessor;

/**
 * Allows a {@link BindMethod} value to be stored and retrieved from an
 * {@link AttributeAccessor}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class BindMethodAttribute {

  static final String NAME = BindMethod.class.getName();

  @Nullable
  static BindMethod get(ApplicationContext applicationContext, String beanName) {
    return (applicationContext instanceof ConfigurableApplicationContext configurableApplicationContext)
            ? get(configurableApplicationContext.getBeanFactory(), beanName) : null;
  }

  @Nullable
  static BindMethod get(ConfigurableBeanFactory beanFactory, String beanName) {
    return (!beanFactory.containsBeanDefinition(beanName)) ? null : get(beanFactory.getBeanDefinition(beanName));
  }

  @Nullable
  static BindMethod get(BeanDefinitionRegistry beanDefinitionRegistry, String beanName) {
    return (!beanDefinitionRegistry.containsBeanDefinition(beanName))
            ? null
            : get(beanDefinitionRegistry.getBeanDefinition(beanName));
  }

  @Nullable
  static BindMethod get(AttributeAccessor attributes) {
    return (BindMethod) attributes.getAttribute(NAME);
  }

  static void set(AttributeAccessor attributes, BindMethod bindMethod) {
    attributes.setAttribute(NAME, bindMethod);
  }

}
