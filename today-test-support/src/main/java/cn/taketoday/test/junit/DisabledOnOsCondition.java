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

package cn.taketoday.test.junit;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.AnnotationUtils;

import java.util.Optional;

/**
 * Evaluates {@link DisabledOnOs}.
 *
 * @author Moritz Halbritter
 * @since 4.0
 */
class DisabledOnOsCondition implements ExecutionCondition {

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<DisabledOnOs> annotation = AnnotationUtils.findAnnotation(context.getElement(), DisabledOnOs.class);
    if (annotation.isEmpty()) {
      return ConditionEvaluationResult.enabled("No @DisabledOnOs found");
    }
    return evaluate(annotation.get());
  }

  private ConditionEvaluationResult evaluate(DisabledOnOs annotation) {
    String architecture = System.getProperty("os.arch");
    String os = System.getProperty("os.name");
    if (annotation.os().isCurrentOs() && annotation.architecture().equals(architecture)) {
      String reason = annotation.disabledReason().isEmpty()
                      ? String.format("Disabled on OS = %s, architecture = %s", os, architecture)
                      : annotation.disabledReason();
      return ConditionEvaluationResult.disabled(reason);
    }
    return ConditionEvaluationResult
            .enabled(String.format("Enabled on OS = %s, architecture = %s", os, architecture));
  }

}
