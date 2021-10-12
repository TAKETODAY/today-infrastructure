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

package cn.taketoday.context.loader;

import cn.taketoday.beans.factory.BeanDefinitionRegistry;
import cn.taketoday.context.annotation.BeanDefinitionBuilder;

/**
 * @author TODAY 2021/10/10 22:10
 * @since 4.0
 */
public class BeanDefinitionCreationContext {
  final BeanDefinitionRegistry registry;
  private final BeanDefinitionBuilder definitionBuilder = new BeanDefinitionBuilder();

  public BeanDefinitionCreationContext(BeanDefinitionRegistry registry) {
    this.registry = registry;
  }

  public BeanDefinitionRegistry getRegistry() {
    return registry;
  }

  public BeanDefinitionBuilder getDefinitionBuilder() {
    return definitionBuilder;
  }

  public String createBeanName(Class<?> c) {
    return BeanDefinitionBuilder.defaultBeanName(c);
  }

  public String createBeanName(String className) {
    return BeanDefinitionBuilder.defaultBeanName(className);
  }

}
