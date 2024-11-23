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

package cn.taketoday.test.context.aot;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import cn.taketoday.aot.AotDetector;
import cn.taketoday.core.annotation.AnnotatedElementUtils;

/**
 * {@link ExecutionCondition} implementation for {@link DisabledInAotMode}.
 *
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DisabledInAotModeCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    boolean aotEnabled = AotDetector.useGeneratedArtifacts();
    if (aotEnabled) {
      AnnotatedElement element = context.getElement().orElseThrow(() -> new IllegalStateException("No AnnotatedElement"));
      String customReason = findMergedAnnotation(element, DisabledInAotMode.class)
              .map(DisabledInAotMode::value).orElse(null);
      return ConditionEvaluationResult.disabled("Disabled in Infra AOT mode", customReason);
    }
    return ConditionEvaluationResult.enabled("Infra AOT mode is not enabled");
  }

  private static <A extends Annotation> Optional<A> findMergedAnnotation(
          AnnotatedElement element, Class<A> annotationType) {

    return Optional.ofNullable(AnnotatedElementUtils.findMergedAnnotation(element, annotationType));
  }

}
