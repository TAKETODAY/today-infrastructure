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

import infra.context.annotation.Condition;
import infra.context.annotation.ConditionContext;
import infra.core.type.AnnotatedTypeMetadata;
import infra.core.type.AnnotationMetadata;
import infra.core.type.ClassMetadata;
import infra.core.type.MethodMetadata;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.ClassUtils;
import infra.util.StringUtils;

/**
 * Base of all {@link Condition} implementations used with Framework. Provides sensible
 * logging to help the user diagnose what classes are loaded.
 *
 * @author Phillip Webb
 * @author Greg Turnquist
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/16 15:46
 */
public abstract class InfraCondition implements Condition {
  private static final Logger log = LoggerFactory.getLogger(InfraCondition.class);

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    String classOrMethodName = getClassOrMethodName(metadata);
    try {
      ConditionOutcome outcome = getMatchOutcome(context, metadata);
      logOutcome(classOrMethodName, outcome);
      recordEvaluation(context, classOrMethodName, outcome);
      return outcome.isMatch();
    }
    catch (NoClassDefFoundError ex) {
      throw new IllegalStateException("""
              Could not evaluate condition on %s due to %s not found. Make sure your own configuration does not rely on that class. \
              This can also happen if you are @ComponentScanning a today-framework package (e.g. if you put a @ComponentScan in the \
              default package by mistake)""".formatted(classOrMethodName, ex.getMessage()), ex);
    }
    catch (RuntimeException ex) {
      throw new IllegalStateException("Error processing condition on " + getName(metadata), ex);
    }
  }

  private String getName(AnnotatedTypeMetadata metadata) {
    if (metadata instanceof AnnotationMetadata) {
      return ((AnnotationMetadata) metadata).getClassName();
    }
    if (metadata instanceof MethodMetadata methodMetadata) {
      return methodMetadata.getDeclaringClassName() + "." + methodMetadata.getMethodName();
    }
    return metadata.toString();
  }

  private static String getClassOrMethodName(AnnotatedTypeMetadata metadata) {
    if (metadata instanceof ClassMetadata classMetadata) {
      return classMetadata.getClassName();
    }
    MethodMetadata methodMetadata = (MethodMetadata) metadata;
    return methodMetadata.getDeclaringClassName() + "#" + methodMetadata.getMethodName();
  }

  protected final void logOutcome(String classOrMethodName, ConditionOutcome outcome) {
    if (log.isTraceEnabled()) {
      log.trace(getLogMessage(classOrMethodName, outcome));
    }
  }

  private StringBuilder getLogMessage(String classOrMethodName, ConditionOutcome outcome) {
    StringBuilder message = new StringBuilder();
    message.append("Condition ");
    message.append(ClassUtils.getShortName(getClass()));
    message.append(" on ");
    message.append(classOrMethodName);
    message.append(outcome.isMatch() ? " matched" : " did not match");
    if (StringUtils.isNotEmpty(outcome.getMessage())) {
      message.append(" due to ");
      message.append(outcome.getMessage());
    }
    return message;
  }

  private void recordEvaluation(ConditionContext context, String classOrMethodName, ConditionOutcome outcome) {
    ConditionEvaluationReport report = context.getEvaluationReport();
    if (report != null) {
      report.recordConditionEvaluation(classOrMethodName, this, outcome);
    }
  }

  /**
   * Determine the outcome of the match along with suitable log output.
   *
   * @param context the condition context
   * @param metadata the annotation metadata
   * @return the condition outcome
   */
  public abstract ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata);

  /**
   * Return true if any of the specified conditions match.
   *
   * @param context the context
   * @param metadata the annotation meta-data
   * @param conditions conditions to test
   * @return {@code true} if any condition matches.
   */
  protected final boolean anyMatches(ConditionContext context, AnnotatedTypeMetadata metadata, Condition... conditions) {
    for (Condition condition : conditions) {
      if (matches(context, metadata, condition)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Return true if any of the specified condition matches.
   *
   * @param context the context
   * @param metadata the annotation meta-data
   * @param condition condition to test
   * @return {@code true} if the condition matches.
   */
  protected final boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata, Condition condition) {
    if (condition instanceof InfraCondition) {
      return ((InfraCondition) condition).getMatchOutcome(context, metadata).isMatch();
    }
    return condition.matches(context, metadata);
  }

}
