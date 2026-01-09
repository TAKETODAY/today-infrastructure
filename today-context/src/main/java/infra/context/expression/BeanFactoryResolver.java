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

package infra.context.expression;

import org.jspecify.annotations.Nullable;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.expression.AccessException;
import infra.expression.BeanResolver;
import infra.expression.EvaluationContext;
import infra.lang.Assert;

/**
 * EL bean resolver that operates against a {@link BeanFactory}.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 2019-02-23 10:36
 */
public class BeanFactoryResolver implements BeanResolver {

  private final BeanFactory beanFactory;

  /**
   * Create a new {@link BeanFactoryResolver} for the given factory.
   *
   * @param beanFactory the {@link BeanFactory} to resolve bean names against
   */
  public BeanFactoryResolver(BeanFactory beanFactory) {
    Assert.notNull(beanFactory, "BeanFactory is required");
    this.beanFactory = beanFactory;
  }

  @Nullable
  @Override
  public Object resolve(EvaluationContext context, String beanName) throws AccessException {
    try {
      return this.beanFactory.getBean(beanName);
    }
    catch (BeansException ex) {
      throw new AccessException("Could not resolve bean reference against BeanFactory", ex);
    }
  }

}
