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

package infra.logging;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/7 21:25
 */
class CompositeLoggerTests {

  @Test
  void shouldCreateCompositeLoggerWithMultipleLoggers() {
    Logger logger1 = mock(Logger.class);
    Logger logger2 = mock(Logger.class);
    List<Logger> loggers = List.of(logger1, logger2);
    String name = "testLogger";

    CompositeLogger compositeLogger = new CompositeLogger(loggers, name);

    assertThat(compositeLogger).isNotNull();
    assertThat(compositeLogger.getName()).isEqualTo(name);
  }

  @Test
  void shouldReturnCorrectName() {
    Logger logger = mock(Logger.class);
    List<Logger> loggers = List.of(logger);
    String name = "compositeLogger";

    CompositeLogger compositeLogger = new CompositeLogger(loggers, name);

    assertThat(compositeLogger.getName()).isEqualTo(name);
  }

  @Test
  void shouldCheckIfErrorIsEnabled() {
    Logger enabledLogger = mock(Logger.class);
    when(enabledLogger.isEnabled(Level.ERROR)).thenReturn(true);

    Logger disabledLogger = mock(Logger.class);
    when(disabledLogger.isEnabled(Level.ERROR)).thenReturn(false);

    List<Logger> loggers = List.of(disabledLogger, enabledLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    assertThat(compositeLogger.isErrorEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfWarnIsEnabled() {
    Logger enabledLogger = mock(Logger.class);
    when(enabledLogger.isEnabled(Level.WARN)).thenReturn(true);

    Logger disabledLogger = mock(Logger.class);
    when(disabledLogger.isEnabled(Level.WARN)).thenReturn(false);

    List<Logger> loggers = List.of(disabledLogger, enabledLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    assertThat(compositeLogger.isWarnEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfInfoIsEnabled() {
    Logger enabledLogger = mock(Logger.class);
    when(enabledLogger.isEnabled(Level.INFO)).thenReturn(true);

    Logger disabledLogger = mock(Logger.class);
    when(disabledLogger.isEnabled(Level.INFO)).thenReturn(false);

    List<Logger> loggers = List.of(disabledLogger, enabledLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    assertThat(compositeLogger.isInfoEnabled()).isTrue();
  }

  @Test
  void shouldCheckIfTraceIsEnabled() {
    Logger enabledLogger = mock(Logger.class);
    when(enabledLogger.isEnabled(Level.TRACE)).thenReturn(true);

    Logger disabledLogger = mock(Logger.class);
    when(disabledLogger.isEnabled(Level.TRACE)).thenReturn(false);

    List<Logger> loggers = List.of(disabledLogger, enabledLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    assertThat(compositeLogger.isTraceEnabled()).isTrue();
  }

  @Test
  void shouldReturnFalseWhenNoLoggerEnabled() {
    Logger disabledLogger = mock(Logger.class);
    when(disabledLogger.isEnabled(any(Level.class))).thenReturn(false);

    List<Logger> loggers = List.of(disabledLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    assertThat(compositeLogger.isErrorEnabled()).isFalse();
    assertThat(compositeLogger.isWarnEnabled()).isFalse();
    assertThat(compositeLogger.isInfoEnabled()).isFalse();
    assertThat(compositeLogger.isTraceEnabled()).isFalse();
  }

  @Test
  void shouldLogToCorrectLoggerBasedOnLevel() {
    Logger errorLogger = mock(Logger.class);
    Logger infoLogger = mock(Logger.class);
    Logger debugLogger = mock(Logger.class);

    when(errorLogger.isEnabled(Level.ERROR)).thenReturn(true);
    when(infoLogger.isEnabled(Level.INFO)).thenReturn(true);
    when(debugLogger.isEnabled(Level.DEBUG)).thenReturn(true);

    List<Logger> loggers = List.of(debugLogger, infoLogger, errorLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    compositeLogger.error("Error message");
    compositeLogger.info("Info message");
    compositeLogger.debug("Debug message");

    verify(errorLogger).logInternal(Level.ERROR, "Error message", null, null);
    verify(infoLogger).logInternal(Level.INFO, "Info message", null, null);
    verify(debugLogger).logInternal(Level.DEBUG, "Debug message", null, null);
  }

  @Test
  void shouldReturnNoOpLoggerWhenNoLoggerSupportsLevel() {
    Logger disabledLogger = mock(Logger.class);
    when(disabledLogger.isEnabled(any(Level.class))).thenReturn(false);

    List<Logger> loggers = List.of(disabledLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    assertThat(compositeLogger).extracting("errorLogger").isSameAs(CompositeLogger.NO_OP_LOG);
    assertThat(compositeLogger).extracting("warnLogger").isSameAs(CompositeLogger.NO_OP_LOG);
    assertThat(compositeLogger).extracting("infoLogger").isSameAs(CompositeLogger.NO_OP_LOG);
    assertThat(compositeLogger).extracting("debugLogger").isSameAs(CompositeLogger.NO_OP_LOG);
    assertThat(compositeLogger).extracting("traceLogger").isSameAs(CompositeLogger.NO_OP_LOG);
  }

  @Test
  void shouldSelectFirstEnabledLoggerForLevel() {
    Logger firstEnabled = mock(Logger.class);
    Logger secondEnabled = mock(Logger.class);
    when(firstEnabled.isEnabled(Level.INFO)).thenReturn(true);
    when(secondEnabled.isEnabled(Level.INFO)).thenReturn(true);

    List<Logger> loggers = List.of(secondEnabled, firstEnabled);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    // Should select the first enabled logger in the list
    assertThat(compositeLogger).extracting("infoLogger").isSameAs(secondEnabled);
  }

  @Test
  void shouldHandleMixedEnabledLoggers() {
    Logger errorLogger = mock(Logger.class);
    Logger infoLogger = mock(Logger.class);
    Logger traceLogger = mock(Logger.class);

    when(errorLogger.isEnabled(Level.ERROR)).thenReturn(true);
    when(errorLogger.isEnabled(Level.WARN)).thenReturn(true);
    when(errorLogger.isEnabled(Level.INFO)).thenReturn(false);
    when(errorLogger.isEnabled(Level.DEBUG)).thenReturn(false);
    when(errorLogger.isEnabled(Level.TRACE)).thenReturn(false);

    when(infoLogger.isEnabled(Level.ERROR)).thenReturn(false);
    when(infoLogger.isEnabled(Level.WARN)).thenReturn(false);
    when(infoLogger.isEnabled(Level.INFO)).thenReturn(true);
    when(infoLogger.isEnabled(Level.DEBUG)).thenReturn(true);
    when(infoLogger.isEnabled(Level.TRACE)).thenReturn(false);

    when(traceLogger.isEnabled(Level.ERROR)).thenReturn(false);
    when(traceLogger.isEnabled(Level.WARN)).thenReturn(false);
    when(traceLogger.isEnabled(Level.INFO)).thenReturn(false);
    when(traceLogger.isEnabled(Level.DEBUG)).thenReturn(false);
    when(traceLogger.isEnabled(Level.TRACE)).thenReturn(true);

    List<Logger> loggers = List.of(traceLogger, infoLogger, errorLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    assertThat(compositeLogger).extracting("errorLogger").isSameAs(errorLogger);
    assertThat(compositeLogger).extracting("warnLogger").isSameAs(errorLogger);
    assertThat(compositeLogger).extracting("infoLogger").isSameAs(infoLogger);
    assertThat(compositeLogger).extracting("debugLogger").isSameAs(infoLogger);
    assertThat(compositeLogger).extracting("traceLogger").isSameAs(traceLogger);
  }

  @Test
  void shouldLogMessageWithThrowable() {
    Logger errorLogger = mock(Logger.class);
    when(errorLogger.isEnabled(Level.ERROR)).thenReturn(true);

    List<Logger> loggers = List.of(errorLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    Throwable throwable = new RuntimeException("Test exception");
    compositeLogger.error("Error with exception", throwable);

    verify(errorLogger).logInternal(Level.ERROR, "Error with exception", throwable, null);
  }

  @Test
  void shouldLogMessageWithArguments() {
    Logger infoLogger = mock(Logger.class);
    when(infoLogger.isEnabled(Level.INFO)).thenReturn(true);

    List<Logger> loggers = List.of(infoLogger);
    CompositeLogger compositeLogger = new CompositeLogger(loggers, "test");

    Object[] args = { "arg1", 42 };
    compositeLogger.info("Info with args: {} and {}", args);

    verify(infoLogger).logInternal(Level.INFO, "Info with args: {} and {}", null, args);
  }

}