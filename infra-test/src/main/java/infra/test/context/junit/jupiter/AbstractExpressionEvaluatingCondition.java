/*
 * Copyright 2002-present the original author or authors.
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

package infra.test.context.junit.jupiter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

import infra.beans.factory.config.BeanExpressionContext;
import infra.beans.factory.config.BeanExpressionResolver;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.context.ApplicationContext;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.core.annotation.AnnotatedElementUtils;
import infra.core.env.Environment;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.test.annotation.DirtiesContext;
import infra.test.annotation.DirtiesContext.HierarchyMode;
import infra.test.context.TestContextAnnotationUtils;
import infra.util.StringUtils;

/**
 * Abstract base class for implementations of {@link ExecutionCondition} that
 * evaluate expressions configured via annotations to determine if a container
 * or test is enabled.
 *
 * <p>Expressions can be any of the following.
 *
 * <ul>
 * <li>Infra Expression Language (SpEL) expression &mdash; for example:
 * <pre style="code">#{systemProperties['os.name'].toLowerCase().contains('mac')}</pre>
 * <li>Placeholder for a property available in the Infra
 * {@link Environment Environment} &mdash; for example:
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
        InfraExtension.getTestContextManager(context).getTestContext().markApplicationContextDirty(hierarchyMode);
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
      applicationContext = InfraExtension.getApplicationContext(context);
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
      String str = ((String) result).trim().toLowerCase(Locale.ROOT);
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
