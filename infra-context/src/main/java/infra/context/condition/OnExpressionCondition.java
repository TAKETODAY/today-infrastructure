/*
 * Copyright 2012-present the original author or authors.
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

package infra.context.condition;

import infra.beans.factory.config.BeanExpressionContext;
import infra.beans.factory.config.BeanExpressionResolver;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.annotation.ConditionContext;
import infra.context.expression.StandardBeanExpressionResolver;
import infra.core.Ordered;
import infra.core.type.AnnotatedTypeMetadata;

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
      resolver = new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader());
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
