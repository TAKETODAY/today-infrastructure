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
