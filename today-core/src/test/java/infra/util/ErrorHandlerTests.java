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
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/12/2 16:48
 */
@ExtendWith(OutputCaptureExtension.class)
class ErrorHandlerTests {

  @Test
  void forPropagating(CapturedOutput output) {
    ErrorHandler errorHandler = ErrorHandler.forPropagating();
    assertThatThrownBy(() -> errorHandler.handleError(new IOException("msg")))
            .isInstanceOf(UndeclaredThrowableException.class)
            .rootCause()
            .isInstanceOf(IOException.class)
            .hasMessage("msg");

    assertThat(errorHandler).extracting("message")
            .isEqualTo("Unexpected error occurred");

    assertThat(errorHandler).extracting("loggerName")
            .isEqualTo("infra.util.PropagatingErrorHandler");

    assertThat(StringUtils.countOccurrencesOf(output.toString(), "msg")).isOne();
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "infra.util.PropagatingErrorHandler")).isOne();
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "Unexpected error occurred")).isOne();

    //
    ErrorHandler forPropagating = ErrorHandler.forPropagating("msg", ErrorHandlerTests.class);

    assertThat(forPropagating).extracting("message").isEqualTo("msg");
    assertThat(forPropagating).extracting("loggerName").isEqualTo("infra.util.ErrorHandlerTests");

  }

  @Test
  void forLogging(CapturedOutput output) {
    ErrorHandler errorHandler = ErrorHandler.forLogging("error message", ErrorHandlerTests.class);
    errorHandler.handleError(new IOException("msg"));

    assertThat(errorHandler).extracting("message")
            .isEqualTo("error message");

    assertThat(errorHandler).extracting("loggerName")
            .isEqualTo("infra.util.ErrorHandlerTests");
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "msg")).isOne();
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "infra.util.ErrorHandlerTests")).isEqualTo(2);
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "error message")).isOne();

    errorHandler.handleError(new IOException("msg"));
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "msg")).isEqualTo(2);
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "infra.util.ErrorHandlerTests")).isEqualTo(4);
    assertThat(StringUtils.countOccurrencesOf(output.toString(), "error message")).isEqualTo(2);

  }

  @Test
  void forLogging_toString() {
    ErrorHandler errorHandler = ErrorHandler.forLogging();
    assertThat(errorHandler.toString()).endsWith("message = 'Unexpected error occurred', loggerName = 'infra.util.LoggingErrorHandler']");

  }

  @Test
  void nullMessageAndNullLoggerNameLogsDefaultValues() {
    PropagatingErrorHandler handler = new PropagatingErrorHandler(null, null);
    assertThat(handler).extracting("message").isEqualTo("Unexpected error occurred");
    assertThat(handler).extracting("loggerName").isEqualTo("infra.util.PropagatingErrorHandler");
  }

  @Test
  void customMessageAndLoggerNameAreUsed() {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("Custom message", "custom.logger");
    assertThat(handler).extracting("message").isEqualTo("Custom message");
    assertThat(handler).extracting("loggerName").isEqualTo("custom.logger");
  }

  @Test
  void handleErrorLogsAndPropagatesError(CapturedOutput output) {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("Test message", "test.logger");
    RuntimeException exception = new RuntimeException("Test error");

    assertThatThrownBy(() -> handler.handleError(exception))
            .isSameAs(exception);

    assertThat(output.toString())
            .contains("Test message")
            .contains("test.logger")
            .contains("Test error");
  }

  @Test
  void handleErrorWrapsCheckedExceptions(CapturedOutput output) {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("Test message", "test.logger");
    IOException exception = new IOException("IO error");

    assertThatThrownBy(() -> handler.handleError(exception))
            .isInstanceOf(UndeclaredThrowableException.class)
            .hasCause(exception);

    assertThat(output.toString())
            .contains("Test message")
            .contains("test.logger")
            .contains("IO error");
  }

  @Test
  void toStringIncludesMessageAndLoggerName() {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("Test message", "test.logger");
    assertThat(handler.toString())
            .contains("message = 'Test message'")
            .contains("loggerName = 'test.logger'");
  }

  @Test
  void handleErrorWithEmptyMessageAndLoggerName(CapturedOutput output) {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("", "");
    RuntimeException exception = new RuntimeException("Test error");

    assertThatThrownBy(() -> handler.handleError(exception))
            .isSameAs(exception);

    assertThat(output.toString())
            .contains("Unexpected error occurred")
            .contains("infra.util.PropagatingErrorHandler")
            .contains("Test error");
  }

  @Test
  void handleErrorWithBlankMessageAndLoggerName(CapturedOutput output) {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("  ", "  ");
    RuntimeException exception = new RuntimeException("Test error");

    assertThatThrownBy(() -> handler.handleError(exception))
            .isSameAs(exception);

    assertThat(output.toString())
            .contains("Unexpected error occurred")
            .contains("infra.util.PropagatingErrorHandler")
            .contains("Test error");
  }

  @Test
  void handleErrorPreservesStackTrace(CapturedOutput output) {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("Test message", "test.logger");

    RuntimeException original = new RuntimeException("Test");
    original.fillInStackTrace();
    StackTraceElement[] originalStack = original.getStackTrace();

    try {
      handler.handleError(original);
    }
    catch (RuntimeException thrown) {
      assertThat(thrown.getStackTrace()).isEqualTo(originalStack);
    }
  }

  @Test
  void handleErrorWithNestedExceptions(CapturedOutput output) {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("Test message", "test.logger");
    IOException inner = new IOException("Inner");
    RuntimeException outer = new RuntimeException("Outer", inner);

    assertThatThrownBy(() -> handler.handleError(outer))
            .isSameAs(outer)
            .hasCause(inner);

    assertThat(output.toString())
            .contains("Test message")
            .contains("Outer")
            .contains("Inner");
  }

  @Test
  void handleErrorWithNullException(CapturedOutput output) {
    PropagatingErrorHandler handler = new PropagatingErrorHandler("Test message", "test.logger");

    assertThatThrownBy(() -> handler.handleError(null))
            .isInstanceOf(UndeclaredThrowableException.class);

    assertThat(output.toString()).contains("Test message");
  }

}