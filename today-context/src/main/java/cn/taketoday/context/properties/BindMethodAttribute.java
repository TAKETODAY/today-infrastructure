/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.context.properties;

import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.properties.bind.BindMethod;
import cn.taketoday.core.AttributeAccessor;
import cn.taketoday.lang.Nullable;

/**
 * Allows a {@link BindMethod} value to be stored and retrieved from an
 * {@link AttributeAccessor}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class BindMethodAttribute {

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
