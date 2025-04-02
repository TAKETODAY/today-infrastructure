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

package infra.context.expression;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.expression.AccessException;
import infra.expression.BeanResolver;
import infra.expression.EvaluationContext;
import infra.lang.Assert;
import infra.lang.Nullable;

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
