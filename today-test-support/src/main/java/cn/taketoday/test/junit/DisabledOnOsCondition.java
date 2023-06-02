/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.test.junit;

import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;

import cn.taketoday.core.annotation.MergedAnnotation;
import cn.taketoday.core.annotation.MergedAnnotations;

/**
 * Evaluates {@link DisabledOnOs}.
 *
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DisabledOnOsCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    if (context.getElement().isEmpty()) {
      return ConditionEvaluationResult.enabled("No element for @DisabledOnOs found");
    }
    MergedAnnotation<DisabledOnOs> annotation = MergedAnnotations
            .from(context.getElement().get(), MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
            .get(DisabledOnOs.class);
    if (!annotation.isPresent()) {
      return ConditionEvaluationResult.enabled("No @DisabledOnOs found");
    }
    return evaluate(annotation.synthesize());
  }

  private ConditionEvaluationResult evaluate(DisabledOnOs annotation) {
    String architecture = System.getProperty("os.arch");
    String os = System.getProperty("os.name");
    boolean onDisabledOs = Arrays.stream(annotation.os()).anyMatch(OS::isCurrentOs);
    boolean onDisabledArchitecture = Arrays.asList(annotation.architecture()).contains(architecture);
    if (onDisabledOs && onDisabledArchitecture) {
      String reason = annotation.disabledReason().isEmpty()
                      ? String.format("Disabled on OS = %s, architecture = %s", os, architecture)
                      : annotation.disabledReason();
      return ConditionEvaluationResult.disabled(reason);
    }
    return ConditionEvaluationResult
            .enabled(String.format("Enabled on OS = %s, architecture = %s", os, architecture));
  }

}
