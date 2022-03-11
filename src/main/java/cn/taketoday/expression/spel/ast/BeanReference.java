/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.expression.spel.ast;

import cn.taketoday.expression.AccessException;
import cn.taketoday.expression.BeanResolver;
import cn.taketoday.expression.EvaluationException;
import cn.taketoday.expression.TypedValue;
import cn.taketoday.expression.spel.ExpressionState;
import cn.taketoday.expression.spel.SpelEvaluationException;
import cn.taketoday.expression.spel.SpelMessage;

/**
 * Represents a bean reference to a type, for example {@code @foo} or {@code @'foo.bar'}.
 * For a FactoryBean the syntax {@code &foo} can be used to access the factory itself.
 *
 * @author Andy Clement
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
