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

package infra.test.context.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import infra.context.ApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.app.logging.LogLevel;
import infra.app.logging.LoggingSystem;
import infra.test.annotation.DirtiesContext;
import infra.test.context.TestContext;
import infra.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

class ExpressionConditionAdditionalTests {

  @Test
  void missingElementAndBlankExpressionsAreRejected() {
    ExtensionContext context = mock(ExtensionContext.class);
    given(context.getElement()).willReturn(Optional.empty());
    assertThatIllegalStateException().isThrownBy(() -> new EnabledIfCondition().evaluateExecutionCondition(context))
            .withMessage("No AnnotatedElement");
    assertThatIllegalStateException().isThrownBy(() -> new EnabledIfCondition().evaluateExecutionCondition(context(Blank.class)))
            .withMessageContaining("must not be blank");
  }

  @Test
  void stringBooleanAndInvalidResultsAreDistinguished() {
    var condition = new EnabledIfCondition();
    assertThat(condition.evaluateExecutionCondition(context(TrueString.class)).isDisabled()).isFalse();
    assertThat(condition.evaluateExecutionCondition(context(FalseString.class)).isDisabled()).isTrue();
    assertThat(condition.evaluateExecutionCondition(context(WithReason.class)).getReason()).contains("explicit reason");
    assertThatIllegalStateException().isThrownBy(() -> condition.evaluateExecutionCondition(context(NumberValue.class)))
            .withMessageContaining("must evaluate to a String or a Boolean");
    assertThatIllegalStateException().isThrownBy(() -> condition.evaluateExecutionCondition(context(NullValue.class)))
            .withMessageContaining("must evaluate to a String or a Boolean");
    assertThatIllegalStateException().isThrownBy(() -> condition.evaluateExecutionCondition(context(InvalidString.class)))
            .withMessageContaining("must evaluate to \"true\" or \"false\"");
  }

  @Test
  void nonConfigurableLoadedContextDisablesConditionAndHonorsDirtiesContext() {
    ExtensionContext extension = context(Dirty.class);
    TestContext testContext = mock(TestContext.class);
    TestContextManager manager = mock(TestContextManager.class);
    given(manager.getTestContext()).willReturn(testContext);
    try (var infra = mockStatic(InfraExtension.class)) {
      infra.when(() -> InfraExtension.getApplicationContext(extension)).thenReturn(mock(ApplicationContext.class));
      infra.when(() -> InfraExtension.getTestContextManager(extension)).thenReturn(manager);
      assertThat(new EnabledIfCondition().evaluateExecutionCondition(extension).isDisabled()).isTrue();
      verify(testContext).markApplicationContextDirty(DirtiesContext.HierarchyMode.EXHAUSTIVE);
    }
  }

  @ParameterizedTest
  @EnumSource(value = LogLevel.class, names = { "DEBUG", "OFF" })
  void diagnosticsDoNotChangeEnabledDisabledOrUnsupportedContextResults(LogLevel level) throws Exception {
    LoggingSystem logging = LoggingSystem.get(getClass().getClassLoader());
    String name = AbstractExpressionEvaluatingCondition.class.getName();
    var configuration = logging.getLoggerConfiguration(name);
    LogLevel previous = (configuration != null ? configuration.getConfiguredLevel() : null);
    logging.setLogLevel(name, level);
    try {
      var condition = new EnabledIfCondition();
      assertThat(condition.evaluateExecutionCondition(context(getClass())).isDisabled()).isFalse();
      assertThat(condition.evaluateExecutionCondition(context(TrueString.class)).isDisabled()).isFalse();
      assertThat(condition.evaluateExecutionCondition(context(FalseString.class)).isDisabled()).isTrue();
      nonConfigurableLoadedContextDisablesConditionAndHonorsDirtiesContext();
      ExtensionContext extension = context(Clean.class);
      try (var application = new GenericApplicationContext(); var infra = mockStatic(InfraExtension.class)) {
        application.refresh();
        infra.when(() -> InfraExtension.getApplicationContext(extension)).thenReturn(application);
        assertThat(condition.evaluateExecutionCondition(extension).isDisabled()).isTrue();
      }
    }
    finally {
      logging.setLogLevel(name, previous);
    }
  }

  @Test
  void loadedContextWithoutExpressionResolverIsRejected() {
    ExtensionContext extension = context(Clean.class);
    try (var application = new GenericApplicationContext(); var infra = mockStatic(InfraExtension.class)) {
      application.refresh();
      application.getBeanFactory().setBeanExpressionResolver(null);
      infra.when(() -> InfraExtension.getApplicationContext(extension)).thenReturn(application);
      assertThatIllegalStateException().isThrownBy(() -> new EnabledIfCondition().evaluateExecutionCondition(extension))
              .withMessage("No BeanExpressionResolver");
    }
  }

  private ExtensionContext context(AnnotatedElement element) {
    ExtensionContext context = mock(ExtensionContext.class);
    given(context.getElement()).willReturn(Optional.of(element));
    return context;
  }

  @EnabledIf(" ")
  static class Blank { }

  @EnabledIf(" TrUe ")
  static class TrueString { }

  @EnabledIf(" FaLsE ")
  static class FalseString { }

  @EnabledIf(expression = "#{true}", reason = "explicit reason")
  static class WithReason { }

  @EnabledIf("#{42}")
  static class NumberValue { }

  @EnabledIf("#{null}")
  static class NullValue { }

  @EnabledIf("not boolean")
  static class InvalidString { }

  @EnabledIf(expression = "true", loadContext = true)
  @DirtiesContext
  static class Dirty { }

  @EnabledIf(expression = "false", loadContext = true)
  static class Clean { }

}
