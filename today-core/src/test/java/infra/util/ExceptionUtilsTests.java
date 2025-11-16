/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.util;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

import infra.util.function.ThrowingRunnable;
import infra.util.function.ThrowingSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/8/19 23:02
 */
class ExceptionUtilsTests {

  @Test
  void sneakyThrow() {
    Exception exception = new Exception();
    assertThatThrownBy(() -> {
      ExceptionUtils.sneakyThrow((ThrowingRunnable) () -> {
        throw exception;
      });

    }).isSameAs(exception);

    assertThat(ExceptionUtils.sneakyThrow(() -> {
      return "";
    })).isEmpty();

  }

  @Test
  void unwrapIfNecessary() {
    Exception exception = new Exception();
    assertThat(ExceptionUtils.unwrapIfNecessary(exception)).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new InvocationTargetException(exception))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new UndeclaredThrowableException(exception))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new InvocationTargetException(new InvocationTargetException(exception)))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new InvocationTargetException(new UndeclaredThrowableException(exception)))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new UndeclaredThrowableException(new InvocationTargetException(exception)))).isSameAs(exception);
    assertThat(ExceptionUtils.unwrapIfNecessary(new UndeclaredThrowableException(new UndeclaredThrowableException(exception)))).isSameAs(exception);

    //
    assertThat(ExceptionUtils.unwrapIfNecessary(new IllegalStateException(new UndeclaredThrowableException(exception)))).isInstanceOf(IllegalStateException.class);
    assertThat(ExceptionUtils.unwrapIfNecessary(new IllegalStateException(new InvocationTargetException(exception)))).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void getNestedMessageWithBothMessageAndCause() {
    Exception cause = new RuntimeException("root cause");
    String message = ExceptionUtils.getNestedMessage(cause, "base message");
    assertThat(message).isEqualTo("base message; nested exception is java.lang.RuntimeException: root cause");
  }

  @Test
  void getNestedMessageWithNullMessage() {
    Exception cause = new RuntimeException("root cause");
    String message = ExceptionUtils.getNestedMessage(cause, null);
    assertThat(message).isEqualTo("nested exception is java.lang.RuntimeException: root cause");
  }

  @Test
  void getNestedMessageWithNullCause() {
    String message = ExceptionUtils.getNestedMessage(null, "base message");
    assertThat(message).isEqualTo("base message");
  }

  @Test
  void getNestedMessageWithNullMessageAndCause() {
    String message = ExceptionUtils.getNestedMessage(null, null);
    assertThat(message).isNull();
  }

  @Test
  void getRootCauseWithMultipleNestedException() {
    Exception root = new RuntimeException("root");
    Exception middle = new RuntimeException("middle", root);
    Exception top = new RuntimeException("top", middle);

    assertThat(ExceptionUtils.getRootCause(top)).isSameAs(root);
  }

  @Test
  void getRootCauseWithSingleException() {
    Exception ex = new RuntimeException("single");
    assertThat(ExceptionUtils.getRootCause(ex)).isNull();
  }

  @Test
  void getRootCauseWithNullException() {
    assertThat(ExceptionUtils.getRootCause(null)).isNull();
  }

  @Test
  void getMostSpecificCauseWithType() {
    IllegalArgumentException target = new IllegalArgumentException("target");
    RuntimeException middle = new RuntimeException("middle", target);
    Exception top = new Exception("top", middle);

    assertThat(ExceptionUtils.getMostSpecificCause(top, IllegalArgumentException.class)).isSameAs(target);
  }

  @Test
  void getMostSpecificCauseReturnsNullWhenTypeNotFound() {
    RuntimeException ex = new RuntimeException("ex");
    assertThat(ExceptionUtils.getMostSpecificCause(ex, IllegalArgumentException.class)).isNull();
  }

  @Test
  void containsReturnsTrueWhenExceptionTypePresent() {
    IllegalArgumentException target = new IllegalArgumentException("target");
    RuntimeException wrapper = new RuntimeException(target);

    assertThat(ExceptionUtils.contains(wrapper, IllegalArgumentException.class)).isTrue();
  }

  @Test
  void containsReturnsFalseWhenExceptionTypeNotPresent() {
    RuntimeException ex = new RuntimeException("ex");
    assertThat(ExceptionUtils.contains(ex, IllegalArgumentException.class)).isFalse();
  }

  @Test
  void sneakyThrowWithNullThrowable() {
    assertThatThrownBy(() -> ExceptionUtils.sneakyThrow((Throwable) null))
            .isInstanceOf(NullPointerException.class)
            .hasMessage("t");
  }

  @Test
  void stackTraceToStringContainsExceptionDetails() {
    Exception ex = new RuntimeException("test message");
    String stackTrace = ExceptionUtils.stackTraceToString(ex);

    assertThat(stackTrace)
            .contains("java.lang.RuntimeException: test message")
            .contains("at ")
            .contains(getClass().getName());
  }

  @Test
  void unwrapIfNecessaryHandlesMultipleLevelsOfWrapping() {
    Exception original = new RuntimeException("original");
    Exception wrapped = new UndeclaredThrowableException(
            new InvocationTargetException(
                    new UndeclaredThrowableException(original)));

    assertThat(ExceptionUtils.unwrapIfNecessary(wrapped)).isSameAs(original);
  }

  @Test
  void sneakyThrowSupplierReturnsValueOnSuccess() {
    String result = ExceptionUtils.sneakyThrow(() -> "success");
    assertThat(result).isEqualTo("success");
  }

  @Test
  void sneakyThrowSupplierWithNullAction() {
    ThrowingSupplier<String> action = null;
    assertThatThrownBy(() -> ExceptionUtils.sneakyThrow(action))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void sneakyThrowRunnableWithNullAction() {
    ThrowingRunnable action = null;
    assertThatThrownBy(() -> ExceptionUtils.sneakyThrow(action))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void getMostSpecificCauseReturnsOriginalWhenNoRootCause() {
    Exception ex = new RuntimeException("original");
    assertThat(ExceptionUtils.getMostSpecificCause(ex)).isSameAs(ex);
  }

  @Test
  void getMostSpecificCauseWithNullOriginal() {
    assertThat(ExceptionUtils.getMostSpecificCause(null, RuntimeException.class)).isNull();
  }

  @Test
  void getMostSpecificCauseWithNullType() {
    Exception ex = new RuntimeException();
    assertThat((Object) ExceptionUtils.getMostSpecificCause(ex, null)).isNull();
  }

  @Test
  void containsReturnsFalseForNullType() {
    Exception ex = new RuntimeException();
    assertThat(ExceptionUtils.contains(ex, null)).isFalse();
  }

  @Test
  void sneakyThrowSupplierPropagatesException() {
    Exception expected = new RuntimeException();
    assertThatThrownBy(() -> ExceptionUtils.sneakyThrow(() -> { throw expected; }))
            .isSameAs(expected);
  }

  @Test
  void stackTraceToStringHandlesEmptyStackTrace() {
    Exception ex = new RuntimeException();
    ex.setStackTrace(new StackTraceElement[0]);
    String trace = ExceptionUtils.stackTraceToString(ex);

    assertThat(trace).contains("java.lang.RuntimeException");
  }

  @Test
  void getMostSpecificCauseWithSelfReferencingCause() {
    RuntimeException ex = new RuntimeException("test");

    assertThat(ExceptionUtils.getMostSpecificCause(ex)).isSameAs(ex);
  }

  @Test
  void unwrapIfNecessaryWithMultipleWrappingTypes() {
    Exception original = new IOException("test");
    Exception wrapped = new InvocationTargetException(
            new UndeclaredThrowableException(
                    new InvocationTargetException(original)));

    assertThat(ExceptionUtils.unwrapIfNecessary(wrapped)).isSameAs(original);
  }

  @Test
  void getMostSpecificCauseReturnsRootCauseWhenPresent() {
    Exception root = new RuntimeException("root");
    Exception middle = new RuntimeException("middle", root);
    Exception top = new RuntimeException("top", middle);

    assertThat(ExceptionUtils.getMostSpecificCause(top)).isSameAs(root);
  }

  @Test
  void getMostSpecificCauseWithNullException() {
    assertThat(ExceptionUtils.getMostSpecificCause(null)).isNull();
  }

  @Test
  void containsReturnsTrueForDirectExceptionType() {
    RuntimeException ex = new RuntimeException();
    assertThat(ExceptionUtils.contains(ex, RuntimeException.class)).isTrue();
  }

  @Test
  void containsReturnsTrueForNestedExceptionType() {
    Exception root = new IllegalArgumentException();
    Exception wrapper = new RuntimeException(root);
    assertThat(ExceptionUtils.contains(wrapper, IllegalArgumentException.class)).isTrue();
  }

  @Test
  void stackTraceToStringWithNullException() {
    assertThatThrownBy(() -> ExceptionUtils.stackTraceToString(null))
            .isInstanceOf(NullPointerException.class);
  }

  @Test
  void stackTraceToStringWithStandardException() {
    Exception ex = new Exception("test exception");
    String trace = ExceptionUtils.stackTraceToString(ex);

    assertThat(trace).contains("java.lang.Exception: test exception");
    assertThat(trace).contains("at " + ExceptionUtilsTests.class.getName());
  }

  @Test
  void sneakyThrowWithRuntimeException() {
    RuntimeException ex = new RuntimeException("sneaky");
    assertThatThrownBy(() -> { throw ExceptionUtils.sneakyThrow(ex); })
            .isSameAs(ex);
  }

  @Test
  void sneakyThrowWithCheckedException() {
    IOException ex = new IOException("sneaky checked");
    assertThatThrownBy(() -> { throw ExceptionUtils.sneakyThrow(ex); })
            .isSameAs(ex);
  }

  @Test
  void sneakyThrowRunnableExecutesSuccessfully() {
    assertThatCode(() -> ExceptionUtils.sneakyThrow(() -> { }))
            .doesNotThrowAnyException();
  }

  @Test
  void sneakyThrowRunnableThrowsException() {
    RuntimeException expected = new RuntimeException("expected");
    assertThatThrownBy(() -> ExceptionUtils.sneakyThrow((ThrowingRunnable) () -> { throw expected; }))
            .isSameAs(expected);
  }

  @Test
  void getNestedMessageWithMessageAndNullException() {
    String message = ExceptionUtils.getNestedMessage(null, "base message");
    assertThat(message).isEqualTo("base message");
  }

  @Test
  void unwrapIfNecessaryWithNonWrappingException() {
    Exception ex = new RuntimeException("normal");
    assertThat(ExceptionUtils.unwrapIfNecessary(ex)).isSameAs(ex);
  }

  @Test
  void getNestedMessageHandlesExceptionWithNullMessage() {
    Exception cause = new RuntimeException(); // No message
    String message = ExceptionUtils.getNestedMessage(cause, "base message");
    assertThat(message).startsWith("base message; nested exception is java.lang.RuntimeException");
  }

  @Test
  void stackTraceToStringHandlesExceptionWithNullMessage() {
    Exception ex = new RuntimeException((String) null);
    String trace = ExceptionUtils.stackTraceToString(ex);

    assertThat(trace).contains("java.lang.RuntimeException");
  }

  @Test
  void sneakyThrowSupplierWithCheckedException() {
    IOException expected = new IOException("checked");
    assertThatThrownBy(() -> ExceptionUtils.sneakyThrow(() -> { throw expected; }))
            .isSameAs(expected);
  }

}