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

package cn.taketoday.test.context.junit.jupiter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import java.util.function.Function;

import cn.taketoday.beans.factory.config.BeanExpressionContext;
import cn.taketoday.beans.factory.config.BeanExpressionResolver;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.core.annotation.AnnotatedElementUtils;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.test.annotation.DirtiesContext;
import cn.taketoday.test.annotation.DirtiesContext.HierarchyMode;
import cn.taketoday.test.context.TestContextAnnotationUtils;
import cn.taketoday.util.StringUtils;

/**
 * Abstract base class for implementations of {@link ExecutionCondition} that
 * evaluate expressions configured via annotations to determine if a container
 * or test is enabled.
 *
 * <p>Expressions can be any of the following.
 *
 * <ul>
 * <li>Spring Expression Language (SpEL) expression &mdash; for example:
 * <pre style="code">#{systemProperties['os.name'].toLowerCase().contains('mac')}</pre>
 * <li>Placeholder for a property available in the Spring
 * {@link cn.taketoday.core.env.Environment Environment} &mdash; for example:
 * <pre style="code">${smoke.tests.enabled}</pre>
 * <li>Text literal &mdash; for example:
 * <pre style="code">true</pre>
 * </ul>
 *
 * @author Sam Brannen
 * @author Tadaya Tsuyukubo
 * @see EnabledIf
 * @see DisabledIf
 * @since 4.0
 */
abstract class AbstractExpressionEvaluatingCondition implements ExecutionCondition {

  private static final Logger logger = LoggerFactory.getLogger(AbstractExpressionEvaluatingCondition.class);

  /**
   * Evaluate the expression configured via the supplied annotation type on
   * the {@link AnnotatedElement} for the supplied {@link ExtensionContext}.
   *
   * @param annotationType the type of annotation to process
   * @param expressionExtractor a function that extracts the expression from
   * the annotation
   * @param reasonExtractor a function that extracts the reason from the
   * annotation
   * @param loadContextExtractor a function that extracts the {@code loadContext}
   * flag from the annotation
   * @param enabledOnTrue indicates whether the returned {@code ConditionEvaluationResult}
   * should be {@link ConditionEvaluationResult#enabled enabled} if the expression
   * evaluates to {@code true}
   * @param context the {@code ExtensionContext}
   * @return {@link ConditionEvaluationResult#enabled enabled} if the container
   * or test should be enabled; otherwise {@link ConditionEvaluationResult#disabled disabled}
   */
  protected <A extends Annotation> ConditionEvaluationResult evaluateAnnotation(Class<A> annotationType,
          Function<A, String> expressionExtractor, Function<A, String> reasonExtractor,
          Function<A, Boolean> loadContextExtractor, boolean enabledOnTrue, ExtensionContext context) {

    Assert.state(context.getElement().isPresent(), "No AnnotatedElement");
    AnnotatedElement element = context.getElement().get();
    Optional<A> annotation = findMergedAnnotation(element, annotationType);

    if (annotation.isEmpty()) {
      String reason = String.format("%s is enabled since @%s is not present", element,
              annotationType.getSimpleName());
      if (logger.isDebugEnabled()) {
        logger.debug(reason);
      }
      return ConditionEvaluationResult.enabled(reason);
    }

    String expression = annotation.map(expressionExtractor)
            .map(String::trim)
            .filter(StringUtils::isNotEmpty)
            .orElseThrow(() -> new IllegalStateException(String.format(
                    "The expression in @%s on [%s] must not be blank", annotationType.getSimpleName(), element)));

    boolean loadContext = loadContextExtractor.apply(annotation.get());
    boolean evaluatedToTrue = evaluateExpression(expression, loadContext, annotationType, context);
    ConditionEvaluationResult result;

    if (evaluatedToTrue) {
      String adjective = (enabledOnTrue ? "enabled" : "disabled");
      String reason = annotation.map(reasonExtractor).filter(StringUtils::hasText).orElseGet(
              () -> String.format("%s is %s because @%s(\"%s\") evaluated to true", element, adjective,
                      annotationType.getSimpleName(), expression));
      if (logger.isInfoEnabled()) {
        logger.info(reason);
      }
      result = (enabledOnTrue ? ConditionEvaluationResult.enabled(reason)
                              : ConditionEvaluationResult.disabled(reason));
    }
    else {
      String adjective = (enabledOnTrue ? "disabled" : "enabled");
      String reason = String.format("%s is %s because @%s(\"%s\") did not evaluate to true",
              element, adjective, annotationType.getSimpleName(), expression);
      if (logger.isDebugEnabled()) {
        logger.debug(reason);
      }
      result = (enabledOnTrue ? ConditionEvaluationResult.disabled(reason) :
                ConditionEvaluationResult.enabled(reason));
    }

    // If we eagerly loaded the ApplicationContext to evaluate SpEL expressions
    // and the test class ends up being disabled, we have to check if the
    // user asked for the ApplicationContext to be closed via @DirtiesContext,
    // since the DirtiesContextTestExecutionListener will never be invoked for
    // a disabled test class.
    // See https://github.com/spring-projects/spring-framework/issues/26694
    if (loadContext && result.isDisabled() && element instanceof Class<?> testClass) {
      DirtiesContext dirtiesContext = TestContextAnnotationUtils.findMergedAnnotation(testClass, DirtiesContext.class);
      if (dirtiesContext != null) {
        HierarchyMode hierarchyMode = dirtiesContext.hierarchyMode();
        ApplicationExtension.getTestContextManager(context).getTestContext().markApplicationContextDirty(hierarchyMode);
      }
    }

    return result;
  }

  private <A extends Annotation> boolean evaluateExpression(String expression, boolean loadContext,
          Class<A> annotationType, ExtensionContext context) {

    Assert.state(context.getElement().isPresent(), "No AnnotatedElement");
    AnnotatedElement element = context.getElement().get();
    GenericApplicationContext gac = null;
    ApplicationContext applicationContext;

    if (loadContext) {
      applicationContext = ApplicationExtension.getApplicationContext(context);
    }
    else {
      gac = new GenericApplicationContext();
      gac.refresh();
      applicationContext = gac;
    }

    if (!(applicationContext instanceof ConfigurableApplicationContext)) {
      if (logger.isWarnEnabled()) {
        String contextType = applicationContext.getClass().getName();
        logger.warn(String.format("@%s(\"%s\") could not be evaluated on [%s] since the test " +
                        "ApplicationContext [%s] is not a ConfigurableApplicationContext",
                annotationType.getSimpleName(), expression, element, contextType));
      }
      return false;
    }

    ConfigurableBeanFactory configurableBeanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    BeanExpressionResolver expressionResolver = configurableBeanFactory.getBeanExpressionResolver();
    Assert.state(expressionResolver != null, "No BeanExpressionResolver");
    BeanExpressionContext beanExpressionContext = new BeanExpressionContext(configurableBeanFactory, null);

    Object result = expressionResolver.evaluate(
            configurableBeanFactory.resolveEmbeddedValue(expression), beanExpressionContext);

    if (gac != null) {
      gac.close();
    }

    if (result instanceof Boolean) {
      return (Boolean) result;
    }
    else if (result instanceof String) {
      String str = ((String) result).trim().toLowerCase();
      if ("true".equals(str)) {
        return true;
      }
      Assert.state("false".equals(str),
              () -> String.format("@%s(\"%s\") on %s must evaluate to \"true\" or \"false\", not \"%s\"",
                      annotationType.getSimpleName(), expression, element, result));
      return false;
    }
    else {
      String message = String.format("@%s(\"%s\") on %s must evaluate to a String or a Boolean, not %s",
              annotationType.getSimpleName(), expression, element,
              (result != null ? result.getClass().getName() : "null"));
      throw new IllegalStateException(message);
    }
  }

  private static <A extends Annotation> Optional<A> findMergedAnnotation(
          AnnotatedElement element, Class<A> annotationType) {

    return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, annotationType));
  }

}
