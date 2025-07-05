/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.test.junit;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Locale;

/**
 * An implementation of {@link ExecutionCondition} that conditionally enables or disables
 * the execution of a test method or class based on the specified locale attributes.
 *
 * @author Dmytro Nosan
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 */
class EnabledOnLocaleCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    return AnnotationSupport.findAnnotation(context.getElement(), EnabledOnLocale.class)
            .map(this::evaluate)
            .orElseGet(() -> ConditionEvaluationResult.enabled("No @EnabledOnLocale annotation found"));
  }

  private ConditionEvaluationResult evaluate(EnabledOnLocale annotation) {
    Locale locale = Locale.getDefault();
    String language = locale.getLanguage();
    if (!annotation.language().equals(language)) {
      return ConditionEvaluationResult.disabled("Disabled on language: " + language);
    }
    return ConditionEvaluationResult.enabled("Enabled on language: " + language);
  }

}
