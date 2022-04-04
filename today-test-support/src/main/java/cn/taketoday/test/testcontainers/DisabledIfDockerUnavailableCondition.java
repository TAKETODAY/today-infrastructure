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

package cn.taketoday.test.testcontainers;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.DockerClientFactory;

/**
 * An {@link ExecutionCondition} that disables execution if Docker is unavailable.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 4.0
 */
class DisabledIfDockerUnavailableCondition implements ExecutionCondition {

  private static final String SILENCE_PROPERTY = "visibleassertions.silence";

  private static final ConditionEvaluationResult ENABLED = ConditionEvaluationResult.enabled("Docker available");

  private static final ConditionEvaluationResult DISABLED = ConditionEvaluationResult.disabled("Docker unavailable");

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    String originalSilenceValue = System.getProperty(SILENCE_PROPERTY);
    try {
      DockerClientFactory.instance().client();
      return ENABLED;
    }
    catch (Throwable ex) {
      return DISABLED;
    }
    finally {
      if (originalSilenceValue != null) {
        System.setProperty(SILENCE_PROPERTY, originalSilenceValue);
      }
      else {
        System.clearProperty(SILENCE_PROPERTY);
      }
    }
  }

}
