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
              if (property != null) {
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

