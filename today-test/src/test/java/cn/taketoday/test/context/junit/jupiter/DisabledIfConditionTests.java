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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

import java.lang.reflect.Method;
import java.util.Optional;

import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.context.TestContextManager;
import cn.taketoday.test.context.junit.SpringJUnitJupiterTestSuite;
import cn.taketoday.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DisabledIfCondition} that verify actual condition evaluation
 * results and exception handling; whereas, {@link DisabledIfTests} only tests
 * the <em>happy paths</em>.
 *
 * <p>To run these tests in an IDE that does not have built-in support for the JUnit
 * Platform, simply run {@link SpringJUnitJupiterTestSuite} as a JUnit 4 test.
 *
 * @author Sam Brannen
 * @see DisabledIfTests
 * @since 5.0
 */
class DisabledIfConditionTests {

  private final DisabledIfCondition condition = new DisabledIfCondition();

  @Test
  void missingDisabledIf() {
    ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("missingDisabledIf"));
    assertThat(result.isDisabled()).isFalse();
    assertThat(result.getReason().get()).endsWith("missingDisabledIf() is enabled since @DisabledIf is not present");
  }

  @Test
  void disabledByEmptyExpression() {
    assertExpressionIsBlank("emptyExpression");
    assertExpressionIsBlank("blankExpression");
  }

  @Test
  void invalidExpressionEvaluationType() {
    String methodName = "nonBooleanOrStringExpression";
    Method method = ReflectionUtils.findMethod(getClass(), methodName);

    assertThatIllegalStateException()
            .isThrownBy(() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)))
            .withMessageContaining(
                    "@DisabledIf(\"#{6 * 7}\") on " + method + " must evaluate to a String or a Boolean, not java.lang.Integer");
  }

  @Test
  void unsupportedStringEvaluationValue() {
    String methodName = "stringExpressionThatIsNeitherTrueNorFalse";
    Method method = ReflectionUtils.findMethod(getClass(), methodName);

    assertThatIllegalStateException()
            .isThrownBy(() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)))
            .withMessageContaining(
                    "@DisabledIf(\"#{'enigma'}\") on " + method + " must evaluate to \"true\" or \"false\", not \"enigma\"");
  }

  @Test
  void disabledWithCustomReason() {
    ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("customReason"));
    assertThat(result.isDisabled()).isTrue();
    assertThat(result.getReason()).contains("Because... 42!");
  }

  @Test
  void disabledWithDefaultReason() {
    ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("defaultReason"));
    assertThat(result.isDisabled()).isTrue();
    assertThat(result.getReason().get())
            .endsWith("defaultReason() is disabled because @DisabledIf(\"#{1 + 1 eq 2}\") evaluated to true");
  }

  @Test
  void notDisabledWithDefaultReason() {
    ConditionEvaluationResult result = condition.evaluateExecutionCondition(buildExtensionContext("neverDisabledWithDefaultReason"));
    assertThat(result.isDisabled()).isFalse();
    assertThat(result.getReason().get())
            .endsWith("neverDisabledWithDefaultReason() is enabled because @DisabledIf(\"false\") did not evaluate to true");
  }

  // -------------------------------------------------------------------------

  private ExtensionContext buildExtensionContext(String methodName) {
    Class<?> testClass = SpringTestCase.class;
    Method method = ReflectionUtils.findMethod(getClass(), methodName);
    Store store = mock(Store.class);
    given(store.getOrComputeIfAbsent(any(), any(), any())).willReturn(new TestContextManager(testClass));

    ExtensionContext extensionContext = mock(ExtensionContext.class);
    given(extensionContext.getTestClass()).willReturn(Optional.of(testClass));
    given(extensionContext.getElement()).willReturn(Optional.of(method));
    given(extensionContext.getStore(any())).willReturn(store);
    return extensionContext;
  }

  private void assertExpressionIsBlank(String methodName) {
    assertThatIllegalStateException()
            .isThrownBy(() -> condition.evaluateExecutionCondition(buildExtensionContext(methodName)))
            .withMessageContaining("must not be blank");
  }

  // -------------------------------------------------------------------------

  @DisabledIf("")
  private void emptyExpression() {
  }

  @DisabledIf("\t")
  private void blankExpression() {
  }

  @DisabledIf("#{6 * 7}")
  private void nonBooleanOrStringExpression() {
  }

  @DisabledIf("#{'enigma'}")
  private void stringExpressionThatIsNeitherTrueNorFalse() {
  }

  @DisabledIf(expression = "#{6 * 7 == 42}", reason = "Because... 42!")
  private void customReason() {
  }

  @DisabledIf("#{1 + 1 eq 2}")
  private void defaultReason() {
  }

  @DisabledIf("false")
  private void neverDisabledWithDefaultReason() {
  }

  private static class SpringTestCase {

    @Configuration
    static class Config {
    }
  }

}
