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

package infra.core.testfixture;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Arrays;
import java.util.Optional;

import infra.lang.Assert;

import static org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled;
import static org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

/**
 * {@link ExecutionCondition} for  {@link TestGroup} support.
 *
 * @author Sam Brannen
 * @see EnabledForTestGroups @EnabledForTestGroups
 */
class TestGroupsCondition implements ExecutionCondition {

  private static final ConditionEvaluationResult ENABLED_BY_DEFAULT = enabled("@EnabledForTestGroups is not present");

  @Override
  public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
    Optional<EnabledForTestGroups> optional = findAnnotation(context.getElement(), EnabledForTestGroups.class);
    if (optional.isEmpty()) {
      return ENABLED_BY_DEFAULT;
    }
    TestGroup[] testGroups = optional.get().value();
    Assert.state(testGroups.length > 0, "You must declare at least one TestGroup in @EnabledForTestGroups");
    return (Arrays.stream(testGroups).anyMatch(TestGroup::isActive)) ?
           enabled("Enabled for TestGroups: " + Arrays.toString(testGroups)) :
           disabled("Disabled for TestGroups: " + Arrays.toString(testGroups));
  }

}
