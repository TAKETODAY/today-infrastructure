/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.aop.config;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.config.BeanReference;
import infra.beans.factory.parsing.ComponentDefinition;
import infra.beans.factory.parsing.CompositeComponentDefinition;

/**
 * {@link ComponentDefinition}
 * that holds an aspect definition, including its nested pointcuts.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @see #getNestedComponents()
 * @see PointcutComponentDefinition
 * @since 4.0
 */
public class AspectComponentDefinition extends CompositeComponentDefinition {

  private final BeanDefinition[] beanDefinitions;

  private final BeanReference[] beanReferences;

  public AspectComponentDefinition(String aspectName, BeanDefinition @Nullable [] beanDefinitions,
          BeanReference @Nullable [] beanReferences, @Nullable Object source) {

    super(aspectName, source);
    this.beanDefinitions = (beanDefinitions != null ? beanDefinitions : new BeanDefinition[0]);
    this.beanReferences = (beanReferences != null ? beanReferences : new BeanReference[0]);
  }

  @Override
  public BeanDefinition[] getBeanDefinitions() {
    return this.beanDefinitions;
  }

  @Override
  public BeanReference[] getBeanReferences() {
    return this.beanReferences;
  }

}
