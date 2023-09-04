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

package cn.taketoday.retry.policy;

import java.io.Serial;
import java.util.Map;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.factory.BeanFactoryAware;
import cn.taketoday.context.expression.BeanFactoryResolver;
import cn.taketoday.expression.Expression;
import cn.taketoday.expression.ParserContext;
import cn.taketoday.expression.spel.standard.SpelExpressionParser;
import cn.taketoday.expression.spel.support.StandardEvaluationContext;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.retry.RetryContext;

/**
 * Subclass of {@link SimpleRetryPolicy} that delegates to super.canRetry() and, if true,
 * further evaluates an expression against the last thrown exception.
 *
 * @author Gary Russell
 * @author Aldo Sinanaj
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ExpressionRetryPolicy extends SimpleRetryPolicy implements BeanFactoryAware {

  @Serial
  private static final long serialVersionUID = 1L;

  private static final Logger logger = LoggerFactory.getLogger(ExpressionRetryPolicy.class);

  private static final ParserContext PARSER_CONTEXT = ParserContext.TEMPLATE_EXPRESSION;

  private final Expression expression;

  private final StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

  /**
   * Construct an instance with the provided {@link Expression}.
   *
   * @param expression the expression
   */
  public ExpressionRetryPolicy(Expression expression) {
    Assert.notNull(expression, "'expression' cannot be null");
    this.expression = expression;
  }

  /**
   * Construct an instance with the provided expression.
   *
   * @param expressionString the expression.
   */
  public ExpressionRetryPolicy(String expressionString) {
    Assert.notNull(expressionString, "'expressionString' cannot be null");
    this.expression = getExpression(expressionString);
  }

  /**
   * Construct an instance with the provided {@link Expression}.
   *
   * @param maxAttempts the max attempts
   * @param retryableExceptions the exceptions
   * @param traverseCauses true to examine causes
   * @param expression the expression
   */
  public ExpressionRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
          boolean traverseCauses, Expression expression) {
    super(maxAttempts, retryableExceptions, traverseCauses);
    Assert.notNull(expression, "'expression' cannot be null");
    this.expression = expression;
  }

  /**
   * Construct an instance with the provided expression.
   *
   * @param maxAttempts the max attempts
   * @param retryableExceptions the exceptions
   * @param traverseCauses true to examine causes
   * @param expressionString the expression.
   * @param defaultValue the default action
   */
  public ExpressionRetryPolicy(int maxAttempts, Map<Class<? extends Throwable>, Boolean> retryableExceptions,
          boolean traverseCauses, String expressionString, boolean defaultValue) {
    super(maxAttempts, retryableExceptions, traverseCauses, defaultValue);
    Assert.notNull(expressionString, "'expressionString' cannot be null");
    this.expression = getExpression(expressionString);
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
  }

  public ExpressionRetryPolicy withBeanFactory(BeanFactory beanFactory) {
    setBeanFactory(beanFactory);
    return this;
  }

  @Override
  public boolean canRetry(RetryContext context) {
    Throwable lastThrowable = context.getLastThrowable();
    if (lastThrowable == null) {
      return super.canRetry(context);
    }
    else {
      return super.canRetry(context)
              && Boolean.TRUE.equals(expression.getValue(this.evaluationContext, lastThrowable, Boolean.class));
    }
  }

  /**
   * Get expression based on the expression string. At the moment supports both literal
   * and template expressions. Template expressions are deprecated.
   *
   * @param expression the expression string
   * @return literal expression or template expression
   */
  private static Expression getExpression(String expression) {
    if (isTemplate(expression)) {
      logger.warn("#{...} syntax is not required for this run-time expression "
              + "and is deprecated in favor of a simple expression string");
      return SpelExpressionParser.INSTANCE.parseExpression(expression, PARSER_CONTEXT);
    }
    return SpelExpressionParser.INSTANCE.parseExpression(expression);
  }

  /**
   * Check if the expression is a template
   *
   * @param expression the expression string
   * @return true if the expression string is a template
   */
  public static boolean isTemplate(String expression) {
    return expression.contains(PARSER_CONTEXT.getExpressionPrefix())
            && expression.contains(PARSER_CONTEXT.getExpressionSuffix());
  }

}
