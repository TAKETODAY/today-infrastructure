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

package infra.aop.config;

import org.jspecify.annotations.Nullable;

import infra.beans.factory.config.BeanDefinition;
import infra.beans.factory.parsing.AbstractComponentDefinition;
import infra.beans.factory.parsing.ComponentDefinition;
import infra.lang.Assert;

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
