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

package cn.taketoday.expression.spel.ast;

import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.BeanResolver;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;

/**
 * Represents a reference to a bean, for example {@code @orderService} or
 * {@code @'order.service'}.
 *
 * <p>For a {@link cn.taketoday.beans.factory.FactoryBean FactoryBean}, the
 * syntax {@code &orderServiceFactory} can be used to access the factory itself.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class BeanReference extends SpelNodeImpl {

  private static final String FACTORY_BEAN_PREFIX = "&";

  private final String beanName;

  public BeanReference(int startPos, int endPos, String beanName) {
    super(startPos, endPos);
    this.beanName = beanName;
  }

  @Override
  public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
    BeanResolver beanResolver = state.getEvaluationContext().getBeanResolver();
    if (beanResolver == null) {
      throw new SpelEvaluationException(
              getStartPosition(), SpelMessage.NO_BEAN_RESOLVER_REGISTERED, this.beanName);
    }

    try {
      return new TypedValue(beanResolver.resolve(state.getEvaluationContext(), this.beanName));
    }
    catch (AccessException ex) {
      throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_BEAN_RESOLUTION,
              this.beanName, ex.getMessage());
    }
  }

  @Override
  public String toStringAST() {
    StringBuilder sb = new StringBuilder();
    if (!this.beanName.startsWith(FACTORY_BEAN_PREFIX)) {
      sb.append('@');
    }
    if (!this.beanName.contains(".")) {
      sb.append(this.beanName);
    }
    else {
      sb.append('\'').append(this.beanName).append('\'');
    }
    return sb.toString();
  }

}
