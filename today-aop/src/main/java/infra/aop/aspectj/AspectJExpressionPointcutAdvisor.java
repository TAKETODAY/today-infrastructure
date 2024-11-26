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

package infra.aop.aspectj;

import infra.aop.Pointcut;
import infra.aop.support.AbstractGenericPointcutAdvisor;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.lang.Nullable;

/**
 * Framework AOP Advisor that can be used for any AspectJ pointcut expression.
 *
 * @author Rob Harrop
 * @since 4.0
 */
@SuppressWarnings("serial")
public class AspectJExpressionPointcutAdvisor extends AbstractGenericPointcutAdvisor implements BeanFactoryAware {

  private final AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();

  public void setExpression(@Nullable String expression) {
    this.pointcut.setExpression(expression);
  }

  @Nullable
  public String getExpression() {
    return this.pointcut.getExpression();
  }

  public void setLocation(@Nullable String location) {
    this.pointcut.setLocation(location);
  }

  @Nullable
  public String getLocation() {
    return this.pointcut.getLocation();
  }

  public void setParameterNames(String... names) {
    this.pointcut.setParameterNames(names);
  }

  public void setParameterTypes(Class<?>... types) {
    this.pointcut.setParameterTypes(types);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.pointcut.setBeanFactory(beanFactory);
  }

  @Override
  public Pointcut getPointcut() {
    return this.pointcut;
  }

}
