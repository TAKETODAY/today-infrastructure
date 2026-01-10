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

package infra.util;

import org.junit.jupiter.api.Test;

import infra.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/16 22:29
 */
class LogFormatUtilsTests {

  @Test
  void formatValueWithNullReturnsEmptyString() {
    assertThat(LogFormatUtils.formatValue(null, true)).isEqualTo("");
    assertThat(LogFormatUtils.formatValue(null, false)).isEqualTo("");
  }

  @Test
  void formatValueWithStringUnderLimit() {
    String value = "Hello World";
    assertThat(LogFormatUtils.formatValue(value, true)).isEqualTo("\"Hello World\"");
    assertThat(LogFormatUtils.formatValue(value, false)).isEqualTo("\"Hello World\"");
  }

  @Test
  void formatValueWithNonCharSequenceUnderLimit() {
    Integer value = 42;
    assertThat(LogFormatUtils.formatValue(value, true)).isEqualTo("42");
    assertThat(LogFormatUtils.formatValue(value, false)).isEqualTo("42");
  }

  @Test
  void formatValueWithNonCharSequenceOverLimit() {
    String value = "This is a very long string representation that exceeds the limit of 100 characters and should be truncated accordingly";
    Object object = new Object() {
      @Override
      public String toString() {
        return value;
      }
    };

    String result = LogFormatUtils.formatValue(object, 50, false);
    assertThat(result.length()).isEqualTo(50 + " (truncated)...".length());
    assertThat(result).startsWith("This is a very long string representation");
  }

  @Test
  void formatValueReplacesNewlinesAndControlCharacters() {
    String value = "Line1\nLine2\rLine3\u0001";
    String result = LogFormatUtils.formatValue(value, 100, true);
    assertThat(result).isEqualTo("\"Line1<EOL>Line2<EOL>Line3?\"");
  }

  @Test
  void formatValueHandlesExceptionInToString() {
    Object value = new Object() {
      @Override
      public String toString() {
        throw new RuntimeException("toString failed");
      }
    };

    String result = LogFormatUtils.formatValue(value, true);
    assertThat(result).contains("RuntimeException");
  }

  @Test
  void formatValueWithMaxLengthNegativeOne() {
    String value = "Some text";
    String result = LogFormatUtils.formatValue(value, -1, false);
    assertThat(result).isEqualTo("\"Some text\"");
  }

  @Test
  void traceDebugLogsAtDebugLevelWhenTraceDisabled() {
    Logger logger = mock(Logger.class);
    when(logger.isDebugEnabled()).thenReturn(true);
    when(logger.isTraceEnabled()).thenReturn(false);

    LogFormatUtils.traceDebug(logger, isTrace -> isTrace ? "trace message" : "debug message");

    verify(logger).debug("debug message");
    verify(logger, never()).trace(anyString());
  }

  @Test
  void traceDebugLogsAtTraceLevelWhenTraceEnabled() {
    Logger logger = mock(Logger.class);
    when(logger.isDebugEnabled()).thenReturn(true);
    when(logger.isTraceEnabled()).thenReturn(true);

    LogFormatUtils.traceDebug(logger, isTrace -> isTrace ? "trace message" : "debug message");

    verify(logger).trace("trace message");
    verify(logger, never()).debug(anyString());
  }

  @Test
  void traceDebugDoesNothingWhenDebugEnabledReturnsFalse() {
    Logger logger = mock(Logger.class);
    when(logger.isDebugEnabled()).thenReturn(false);

    LogFormatUtils.traceDebug(logger, isTrace -> { throw new AssertionError("Should not be called"); });

    verify(logger, never()).isTraceEnabled();
    verify(logger, never()).trace(anyString());
    verify(logger, never()).debug(anyString());
  }

}