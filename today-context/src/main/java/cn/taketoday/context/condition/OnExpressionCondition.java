/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.context.condition;

import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.beans.factory.config.BeanExpressionResolver;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.context.expression.StandardBeanExpressionResolver;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/4 16:11
 */
final class OnExpressionCondition extends InfraCondition implements Ordered {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String expression = metadata.getAnnotation(ConditionalOnExpression.class).getStringValue();
    expression = wrapIfNecessary(expression);
    ConditionMessage.Builder messageBuilder = ConditionMessage.forCondition(ConditionalOnExpression.class,
            "(" + expression + ")");
    expression = context.getEnvironment().resolvePlaceholders(expression);
    ConfigurableBeanFactory beanFactory = context.getBeanFactory();
    if (beanFactory != null) {
      boolean result = evaluateExpression(beanFactory, expression);
      return new ConditionOutcome(result, messageBuilder.resultedIn(result));
    }
    return ConditionOutcome.noMatch(messageBuilder.because("no BeanFactory available."));
  }

  private Boolean evaluateExpression(ConfigurableBeanFactory beanFactory, String expression) {
    BeanExpressionResolver resolver = beanFactory.getBeanExpressionResolver();
    if (resolver == null) {
      resolver = new StandardBeanExpressionResolver();
    }
    BeanExpressionContext expressionContext = new BeanExpressionContext(beanFactory, null);
    Object result = resolver.evaluate(expression, expressionContext);
    return (result != null && (boolean) result);
  }

  /**
   * Allow user to provide bare expression with no '#{}' wrapper.
   *
   * @param expression source expression
   * @return wrapped expression
   */
  private String wrapIfNecessary(String expression) {
    if (!expression.startsWith("#{")) {
      return "#{" + expression + "}";
    }
    return expression;
  }

  @Override
  public int getOrder() {
    return Ordered.LOWEST_PRECEDENCE - 20;
  }

}
