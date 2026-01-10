/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.core.testfixture;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/9 23:19
 */
@Documented
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisabledIfInContinuousIntegrationCondition.class)
public @interface DisabledIfInContinuousIntegration {

  String disabledReason() default "";
}

class DisabledIfInContinuousIntegrationCondition implements ExecutionCondition {

  public static final String KEY = "CI";
  public static final String VALUE = "true";

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    return context.getElement()
            .map(element -> element.getAnnotation(DisabledIfInContinuousIntegration.class))
            .map(annotation -> {
              String property = System.getProperty(KEY);
              if (property == null) {
                property = System.getenv(KEY);
              }

              if (VALUE.equals(property)) {
                return disabled("Running in a CI Environment", annotation.disabledReason());
              }
              return null;
            })
            .orElseGet(() -> enabled("Not In CI Environment"));
  }

}

