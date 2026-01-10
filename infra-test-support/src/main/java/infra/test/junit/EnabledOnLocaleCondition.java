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
