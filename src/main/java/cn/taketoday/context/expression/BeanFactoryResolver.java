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
package cn.taketoday.context.expression;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.BeanResolver;
import cn.taketoday.expression.EvaluationContext;
import cn.taketoday.lang.Assert;

/**
 * EL bean resolver that operates against a Spring
 * {@link BeanFactory}.
 *
 * @author Juergen Hoeller
 * @author TODAY
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
    Assert.notNull(beanFactory, "BeanFactory must not be null");
    this.beanFactory = beanFactory;
  }

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
