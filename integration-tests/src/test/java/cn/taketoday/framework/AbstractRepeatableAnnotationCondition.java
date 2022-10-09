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

package cn.taketoday.framework;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import static java.lang.String.format;
import static org.junit.platform.commons.util.AnnotationUtils.findRepeatableAnnotations;

abstract class AbstractRepeatableAnnotationCondition<A extends Annotation> implements ExecutionCondition {

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final Class<A> annotationType;

  AbstractRepeatableAnnotationCondition(Class<A> annotationType) {
    this.annotationType = annotationType;
  }

  @Override
  public final ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<AnnotatedElement> optionalElement = context.getElement();
    if (optionalElement.isPresent()) {
      AnnotatedElement annotatedElement = optionalElement.get();
      // @formatter:off
			return findRepeatableAnnotations(annotatedElement, this.annotationType).stream()
					.map(annotation -> {
						ConditionEvaluationResult result = evaluate(annotation);
						logResult(annotation, annotatedElement, result);
						return result;
					})
					.filter(ConditionEvaluationResult::isDisabled)
					.findFirst()
					.orElse(getNoDisabledConditionsEncounteredResult());
			// @formatter:on
    }
    return getNoDisabledConditionsEncounteredResult();
  }

  protected abstract ConditionEvaluationResult evaluate(A annotation);

  protected abstract ConditionEvaluationResult getNoDisabledConditionsEncounteredResult();

  private void logResult(A annotation, AnnotatedElement annotatedElement, ConditionEvaluationResult result) {
    logger.trace(() -> format("Evaluation of %s on [%s] resulted in: %s", annotation, annotatedElement, result));
  }

}
