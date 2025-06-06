/*
 * Copyright 2017 - 2024 the original author or authors.
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

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.parsing.AbstractComponentDefinition;
import infra.beans.factory.parsing.ComponentDefinition;
import infra.lang.Assert;
import infra.lang.Nullable;

/**
 * {@link ComponentDefinition}
 * implementation that holds a pointcut definition.
 *
 * @author Rob Harrop
 * @since 4.0
 */
public class PointcutComponentDefinition extends AbstractComponentDefinition {

  private final String pointcutBeanName;

  private final BeanDefinition pointcutDefinition;

  private final String description;

  public PointcutComponentDefinition(String pointcutBeanName, BeanDefinition pointcutDefinition, String expression) {
    Assert.notNull(pointcutBeanName, "Bean name is required");
    Assert.notNull(pointcutDefinition, "Pointcut definition is required");
    Assert.notNull(expression, "Expression is required");
    this.pointcutBeanName = pointcutBeanName;
    this.pointcutDefinition = pointcutDefinition;
    this.description = "Pointcut <name='" + pointcutBeanName + "', expression=[" + expression + "]>";
  }

  @Override
  public String getName() {
    return this.pointcutBeanName;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public BeanDefinition[] getBeanDefinitions() {
    return new BeanDefinition[] { this.pointcutDefinition };
  }

  @Override
  @Nullable
  public Object getSource() {
    return this.pointcutDefinition.getSource();
  }

}
