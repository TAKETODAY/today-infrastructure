/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.aop.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import cn.taketoday.logging.Logger;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.ALLOWED_PLACEHOLDERS;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENTS;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_ARGUMENT_TYPES;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_EXCEPTION;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_INVOCATION_TIME;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_METHOD_NAME;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_RETURN_VALUE;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_NAME;
import static cn.taketoday.aop.interceptor.CustomizableTraceInterceptor.PLACEHOLDER_TARGET_CLASS_SHORT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class CustomizableTraceInterceptorTests {

  private final CustomizableTraceInterceptor interceptor = new CustomizableTraceInterceptor();

  @Test
  void setEmptyEnterMessage() {
    // Must not be able to set empty enter message
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(""));
  }

  @Test
  void setEnterMessageWithReturnValuePlaceholder() {
    // Must not be able to set enter message with return value placeholder
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(PLACEHOLDER_RETURN_VALUE));
  }

  @Test
  void setEnterMessageWithExceptionPlaceholder() {
    // Must not be able to set enter message with exception placeholder
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(PLACEHOLDER_EXCEPTION));
  }

  @Test
  void setEnterMessageWithInvocationTimePlaceholder() {
    // Must not be able to set enter message with invocation time placeholder
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setEnterMessage(PLACEHOLDER_INVOCATION_TIME));
  }

  @Test
  void setEmptyExitMessage() {
    // Must not be able to set empty exit message
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExitMessage(""));
  }

  @Test
  void setExitMessageWithExceptionPlaceholder() {
    // Must not be able to set exit message with exception placeholder
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExitMessage(PLACEHOLDER_EXCEPTION));
  }

  @Test
  void setEmptyExceptionMessage() {
    // Must not be able to set empty exception message
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExceptionMessage(""));
  }

  @Test
  void setExceptionMethodWithReturnValuePlaceholder() {
    // Must not be able to set exception message with return value placeholder
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.setExceptionMessage(PLACEHOLDER_RETURN_VALUE));
  }

  @Test
  void sunnyDayPathLogsCorrectly() throws Throwable {
    MethodInvocation methodInvocation = mock();
    given(methodInvocation.getMethod()).willReturn(String.class.getMethod("toString"));
    given(methodInvocation.getThis()).willReturn(this);

    Logger log = mock(Logger.class);
    given(log.isTraceEnabled()).willReturn(true);

    CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
    interceptor.invoke(methodInvocation);

    verify(log, times(2)).trace(anyString());
  }

  @Test
  void exceptionPathLogsCorrectly() throws Throwable {
    MethodInvocation methodInvocation = mock();

    IllegalArgumentException exception = new IllegalArgumentException();
    given(methodInvocation.getMethod()).willReturn(String.class.getMethod("toString"));
    given(methodInvocation.getThis()).willReturn(this);
    given(methodInvocation.proceed()).willThrow(exception);

    Logger log = mock(Logger.class);
    given(log.isTraceEnabled()).willReturn(true);

    CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
    assertThatIllegalArgumentException().isThrownBy(() -> interceptor.invoke(methodInvocation));

    verify(log).trace(anyString());
    verify(log).trace(anyString(), eq(exception));
  }

  @Test
  void sunnyDayPathLogsCorrectlyWithPrettyMuchAllPlaceholdersMatching() throws Throwable {
    MethodInvocation methodInvocation = mock();

    given(methodInvocation.getMethod()).willReturn(String.class.getMethod("toString", new Class[0]));
    given(methodInvocation.getThis()).willReturn(this);
    given(methodInvocation.getArguments()).willReturn(new Object[] { "$ One \\$", 2L });
    given(methodInvocation.proceed()).willReturn("Hello!");

    Logger log = mock(Logger.class);
    given(log.isTraceEnabled()).willReturn(true);

    CustomizableTraceInterceptor interceptor = new StubCustomizableTraceInterceptor(log);
    interceptor.setEnterMessage(new StringBuilder()
            .append("Entering the '").append(PLACEHOLDER_METHOD_NAME)
            .append("' method of the [").append(PLACEHOLDER_TARGET_CLASS_NAME)
            .append("] class with the following args (").append(PLACEHOLDER_ARGUMENTS)
            .append(") and arg types (").append(PLACEHOLDER_ARGUMENT_TYPES)
            .append(").").toString());
    interceptor.setExitMessage(new StringBuilder()
            .append("Exiting the '").append(PLACEHOLDER_METHOD_NAME)
            .append("' method of the [").append(PLACEHOLDER_TARGET_CLASS_SHORT_NAME)
            .append("] class with the following args (").append(PLACEHOLDER_ARGUMENTS)
            .append(") and arg types (").append(PLACEHOLDER_ARGUMENT_TYPES)
            .append("), returning '").append(PLACEHOLDER_RETURN_VALUE)
            .append("' and taking '").append(PLACEHOLDER_INVOCATION_TIME)
            .append("' this long.").toString());
    interceptor.invoke(methodInvocation);

    verify(log, times(2)).trace(anyString());
  }

  /**
   * This test effectively verifies that the internal ALLOWED_PLACEHOLDERS set
   * is properly configured in {@link CustomizableTraceInterceptor}.
   */
  @Test
  void supportedPlaceholderValues() {
    assertThat(ALLOWED_PLACEHOLDERS).containsAll(getPlaceholderConstantValues());
  }

  private List<String> getPlaceholderConstantValues() {
    return Arrays.stream(CustomizableTraceInterceptor.class.getFields())
            .filter(ReflectionUtils::isPublicStaticFinal)
            .filter(field -> field.getName().startsWith("PLACEHOLDER_"))
            .map(this::getFieldValue)
            .map(String.class::cast)
            .toList();
  }

  private Object getFieldValue(Field field) {
    try {
      return field.get(null);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @SuppressWarnings("serial")
  private static class StubCustomizableTraceInterceptor extends CustomizableTraceInterceptor {

    private final Logger log;

    StubCustomizableTraceInterceptor(Logger log) {
      super.setUseDynamicLogger(false);
      this.log = log;
    }

    @Override
    protected Logger getLoggerForInvocation(MethodInvocation invocation) {
      return this.log;
    }
  }

}
