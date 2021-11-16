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

package cn.taketoday.context.autowire;

import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.beans.dependency.DefaultDependencySetter;
import cn.taketoday.beans.dependency.DependencyResolvingStrategy;
import cn.taketoday.beans.dependency.DependencySetter;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.support.BeanMetadata;
import cn.taketoday.beans.support.BeanProperty;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.DefaultProps;
import cn.taketoday.context.annotation.Props;
import cn.taketoday.context.annotation.PropsReader;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.env.PropertyResolver;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

/**
 * @author TODAY 2021/11/15 22:55
 * @since 4.0
 */
public class PropsDependencyResolvingStrategy implements DependencyResolvingStrategy {
  private static final Logger log = LoggerFactory.getLogger(PropsDependencyResolvingStrategy.class);
  private final ApplicationContext context;

  @Nullable
  private PropsReader propsReader;

  public PropsDependencyResolvingStrategy(ApplicationContext context) {
    this.context = context;
  }

  @Nullable
  @Override
  public Set<DependencySetter> resolveDependencies(Object bean, String beanName) {
    Class<?> beanClass = bean.getClass();

    MergedAnnotation<Props> annotation = MergedAnnotations.from(beanClass).get(Props.class);
    if (!annotation.isPresent()) {
      return null;
    }
    BeanDefinition beanDefinition = context.getBeanDefinition(beanName);

    if (log.isDebugEnabled()) {
      log.debug("Loading Properties For: [{}]", beanClass.getName());
    }

    DefaultProps defaultProps = new DefaultProps(annotation);
    PropertyResolver propertyResolver = getResolver(defaultProps);

    Set<DependencySetter> dependencySetters = new LinkedHashSet<>();
    for (BeanProperty property : BeanMetadata.ofClass(beanClass)) {
      if (!property.isReadOnly()) {
        Object converted = read(property, defaultProps, propertyResolver);
        if (converted != null) {
          dependencySetters.add(new DefaultDependencySetter(converted, property));
        }
      }
    }
    dependencySetters.trimToSize();
    return dependencySetters;
  }

}
