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

package cn.taketoday.test.context.junit.jupiter;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * {@code DisabledIfCondition} is an {@link org.junit.jupiter.api.extension.ExecutionCondition}
 * that supports the {@link DisabledIf @DisabledIf} annotation when using the <em>Spring
 * TestContext Framework</em> in conjunction with JUnit 5's <em>Jupiter</em> programming model.
 *
 * <p>Any attempt to use the {@code DisabledIfCondition} without the presence of
 * {@link DisabledIf @DisabledIf} will result in an <em>enabled</em>
 * {@link ConditionEvaluationResult}.
 *
 * @author Sam Brannen
 * @see DisabledIf
 * @see EnabledIf
 * @see InfraExtension
 * @since 4.0
 */
public class DisabledIfCondition extends AbstractExpressionEvaluatingCondition {

  /**
   * Containers and tests are disabled if {@code @DisabledIf} is present on the
   * corresponding test class or test method and the configured expression evaluates
   * to {@code true}.
   */
  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    return evaluateAnnotation(DisabledIf.class, DisabledIf::expression, DisabledIf::reason,
            DisabledIf::loadContext, false, context);
  }

}
