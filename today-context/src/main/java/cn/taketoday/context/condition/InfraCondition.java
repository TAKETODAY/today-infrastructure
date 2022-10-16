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

import cn.taketoday.context.annotation.Condition;
import cn.taketoday.context.annotation.ConditionContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.core.type.AnnotationMetadata;
import cn.taketoday.core.type.ClassMetadata;
import cn.taketoday.core.type.MethodMetadata;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

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
      throw new IllegalStateException("Could not evaluate condition on " + classOrMethodName + " due to "
              + ex.getMessage() + " not found. Make sure your own configuration does not rely on "
              + "that class. This can also happen if you are "
              + "@ComponentScanning a today-framework package (e.g. if you "
              + "put a @ComponentScan in the default package by mistake)", ex);
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

  private void recordEvaluation(
          ConditionContext context, String classOrMethodName, ConditionOutcome outcome) {
    if (context.getBeanFactory() != null) {
      ConditionEvaluationReport.get(context.getBeanFactory())
              .recordConditionEvaluation(classOrMethodName, this, outcome);
    }
  }

  /**
   * Determine the outcome of the match along with suitable log output.
   *
   * @param context the condition context
   * @param metadata the annotation metadata
   * @return the condition outcome
   */
  public abstract ConditionOutcome getMatchOutcome(
          ConditionContext context, AnnotatedTypeMetadata metadata);

  /**
   * Return true if any of the specified conditions match.
   *
   * @param context the context
   * @param metadata the annotation meta-data
   * @param conditions conditions to test
   * @return {@code true} if any condition matches.
   */
  protected final boolean anyMatches(
          ConditionContext context, AnnotatedTypeMetadata metadata, Condition... conditions) {
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
  protected final boolean matches(
          ConditionContext context, AnnotatedTypeMetadata metadata, Condition condition) {
    if (condition instanceof InfraCondition) {
      return ((InfraCondition) condition).getMatchOutcome(context, metadata).isMatch();
    }
    return condition.matches(context, metadata);
  }

}
