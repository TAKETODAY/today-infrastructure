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
}