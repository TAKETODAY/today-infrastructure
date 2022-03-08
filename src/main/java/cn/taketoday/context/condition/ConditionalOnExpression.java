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
package cn.taketoday.context.condition;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.beans.factory.config.BeanExpressionResolver;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.expression.StandardBeanExpressionResolver;
import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.type.AnnotatedTypeMetadata;

/**
 * annotation for a conditional element that depends on the value of a Java
 * Unified Expression Language
 *
 * @author TODAY <br>
 * 2019-06-18 15:11
 */
@Retention(RetentionPolicy.RUNTIME)
@Conditional(OnExpressionCondition.class)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConditionalOnExpression {

  /**
   * The Java Unified Expression Language expression to evaluate. Expression
   * should return {@code true} if the condition passes or {@code false} if it
   * fails.
   *
   * @return the El expression
   */
  String value() default "true";
}

final class OnExpressionCondition extends ContextCondition {

  @Override
  public ConditionOutcome getMatchOutcome(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
    MergedAnnotation<ConditionalOnExpression> annotation = metadata.getAnnotation(ConditionalOnExpression.class);
    String expression = annotation.getStringValue();
    expression = wrapIfNecessary(expression);

    ConditionMessage.Builder messageBuilder = ConditionMessage.forCondition(
            ConditionalOnExpression.class, "(" + expression + ")");

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

}
