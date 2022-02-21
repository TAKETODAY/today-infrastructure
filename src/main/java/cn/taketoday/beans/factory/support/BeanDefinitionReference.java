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
package cn.taketoday.beans.factory.support;

import cn.taketoday.beans.BeanWrapper;
import cn.taketoday.beans.factory.AutowireCapableBeanFactory;
import cn.taketoday.lang.Nullable;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/11/30 15:40
 */
public class BeanDefinitionReference implements PropertyValueRetriever {

  @Nullable
  private final BeanDefinitionBuilder builder;

  @Nullable
  private final BeanDefinition definition;

  public BeanDefinitionReference(@Nullable BeanDefinitionBuilder builder, @Nullable BeanDefinition definition) {
    this.builder = builder;
    this.definition = definition;
  }

  @Override
  public Object retrieve(String propertyPath, BeanWrapper binder, AutowireCapableBeanFactory beanFactory) {
    BeanDefinition definition = getDefinition();
    if (beanFactory instanceof AbstractAutowireCapableBeanFactory factory) {
      Object bean = factory.createBean(getBeanName(definition), definition, null);
      if (bean != null) {
        bean = factory.handleFactoryBean(definition.getBeanName(), definition.getBeanName(), definition, bean);
      }
      return bean;
    }
    Class<?> beanClass = definition.getBeanClass();
    return beanFactory.createBean(beanClass);
  }

  private BeanDefinition getDefinition() {
    if (builder != null) {
      return builder.build();
    }
    return definition;
  }

  private String getBeanName(BeanDefinition definition) {
    String name = definition.getBeanName();
    if (name == null) {
      definition.setBeanName(definition.getBeanClassName());
      return definition.getBeanClassName();
    }
    return name;
  }

  public static BeanDefinitionReference from(Class<?> beanClass) {
    return from(new BeanDefinition(beanClass));
  }

  public static BeanDefinitionReference from(String name, Class<?> beanClass) {
    return from(new BeanDefinition(name, beanClass));
  }

  public static BeanDefinitionReference from(BeanDefinition definition) {
    return new BeanDefinitionReference(null, definition);
  }

  public static BeanDefinitionReference from(BeanDefinitionBuilder builder) {
    return new BeanDefinitionReference(builder, null);
  }

}
