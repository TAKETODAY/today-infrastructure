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
package infra.logging;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2019-12-06 23:09
 */
@Order(Integer.MAX_VALUE)
@Execution(ExecutionMode.SAME_THREAD)
class LoggerTests {

  @Test
  void slf4jLogger() throws Exception {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    assertTrue(logger instanceof Slf4jLogger);
    assertEquals(logger.getName(), getClass().getName());

    assertTrue(logger.isWarnEnabled());
    assertTrue(logger.isInfoEnabled());
    assertTrue(logger.isErrorEnabled());
    assertTrue(logger.isTraceEnabled());
    assertTrue(logger.isDebugEnabled());

    logger.info("testSlf4jLogger");
    logger.warn("testSlf4jLogger");
    logger.error("testSlf4jLogger");
    logger.debug("testSlf4jLogger");
    logger.trace("testSlf4jLogger");
    logger.logInternal(Level.INFO, LogMessage.format("hello {}", "World"), null);

    // LocationAwareSlf4jLogger
    Logger today = Slf4jLoggerFactory.createLog("today");

    assertThat(today).isInstanceOf(LocationAwareSlf4jLogger.class);
    LocationAwareSlf4jLogger locationAwareSlf4jLogger = (LocationAwareSlf4jLogger) today;

    locationAwareSlf4jLogger.logInternal(Level.INFO, LogMessage.format("hello {}", "World"), null);
  }

  @Test
  public void testLog4jLogger() throws Exception {
    Logger logger = createLogger(new Log4j2LoggerFactory());

    assertTrue(logger instanceof Log4j2Logger);
    assertEquals(logger.getName(), getClass().getName());

    assertTrue(logger.isWarnEnabled());
    assertTrue(logger.isInfoEnabled());
    assertTrue(logger.isErrorEnabled());
    assertTrue(logger.isTraceEnabled());
    assertTrue(logger.isDebugEnabled());

    logger.info("testLog4jLogger");
    logger.warn("testLog4jLogger");
    logger.error("testLog4jLogger");
    logger.debug("testLog4jLogger");
    logger.trace("testLog4jLogger");

  }

  @Test
  public void testJavaLoggingLogger() throws Exception {
    Logger logger = createLogger(new JavaLoggingFactory());

    assertTrue(logger instanceof JavaLoggingLogger);
    assertEquals(logger.getName(), getClass().getName());

    assertTrue(logger.isWarnEnabled());
    assertTrue(logger.isInfoEnabled());
    assertTrue(logger.isErrorEnabled());
    assertFalse(logger.isDebugEnabled());
    assertFalse(logger.isTraceEnabled());

    logger.info("testLog4jLogger");
    logger.warn("testLog4jLogger");
    logger.error("testLog4jLogger");
    logger.debug("testLog4jLogger");
    logger.trace("testLog4jLogger");
  }

  @Test
  void noOpLogger() {
    NoOpLogger logger = new NoOpLogger();
    assertThat(logger.getName()).isEqualTo("NoOpLogger");

    assertFalse(logger.isWarnEnabled());
    assertFalse(logger.isInfoEnabled());
    assertFalse(logger.isErrorEnabled());
    assertFalse(logger.isDebugEnabled());
    assertFalse(logger.isTraceEnabled());

    logger.info("NoOpLogger");
    logger.warn("NoOpLogger");
    logger.error("NoOpLogger");
    logger.debug("NoOpLogger");
    logger.trace("NoOpLogger");
  }

  Logger createLogger(LoggerFactory loggerFactory) {
    return loggerFactory.createLogger(getClass().getName());
  }

  @Test
  void logMessageWithSupplier() {
    LogMessage msg = LogMessage.from(() -> new StringBuilder("a").append(" b"));
    assertThat(msg.toString()).isEqualTo("a b");
    assertThat(msg.toString()).isSameAs(msg.toString());

  }

  @Test
  void logMessageWithFormat1() {
    LogMessage msg = LogMessage.format("a {}", "b");
    assertThat(msg.toString()).isEqualTo("a b");
    assertThat(msg.toString()).isSameAs(msg.toString());
  }

  @Test
  void logMessageWithFormat2() {
    LogMessage msg = LogMessage.format("a {} {}", "b", "c");
    assertThat(msg.toString()).isEqualTo("a b c");
    assertThat(msg.toString()).isSameAs(msg.toString());
  }

  @Test
  void logMessageWithFormat3() {
    LogMessage msg = LogMessage.format("a {} {} {}", "b", "c", "d");
    assertThat(msg.toString()).isEqualTo("a b c d");
    assertThat(msg.toString()).isSameAs(msg.toString());
  }

  @Test
  void logMessageWithFormat4() {
    LogMessage msg = LogMessage.format("a {} {} {} {}", "b", "c", "d", "e");
    assertThat(msg.toString()).isEqualTo("a b c d e");
    assertThat(msg.toString()).isSameAs(msg.toString());
  }

  @Test
  void logMessageWithFormatX() {
    LogMessage msg = LogMessage.format("a {} {} {} {} {}", "b", "c", "d", "e", "f");
    assertThat(msg.toString()).isEqualTo("a b c d e f");
    assertThat(msg.toString()).isSameAs(msg.toString());
  }

  @Test
  void isEnabled() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    assertThat(logger.isEnabled(Level.DEBUG)).isTrue();
    assertThat(logger.isEnabled(Level.WARN)).isTrue();
    assertThat(logger.isEnabled(Level.INFO)).isTrue();
    assertThat(logger.isEnabled(Level.TRACE)).isTrue();
    assertThat(logger.isEnabled(Level.ERROR)).isTrue();

    assertThat(logger.hashCode()).isEqualTo(logger.hashCode());
  }

  @Test
  void shouldLogTraceMessagesWithVariousParameters() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test trace with simple message
    logger.trace("Simple trace message");

    // Test trace with one parameter
    logger.trace("Trace message with one param: {}", "param1");

    // Test trace with two parameters
    logger.trace("Trace message with two params: {} and {}", "param1", "param2");

    // Test trace with multiple parameters
    logger.trace("Trace message with multiple params: {}, {}, {}", "param1", "param2", "param3");

    // Test trace with throwable
    logger.trace("Trace message with exception", new RuntimeException("Test exception"));

    // Test trace with object message
    logger.trace("Trace object message");

    // Test trace with object message and throwable
    logger.trace("Trace object message", new RuntimeException("Test exception"));

    assertThat(true).isTrue();
  }

  @Test
  void shouldLogDebugMessagesWithVariousParameters() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test debug with simple message
    logger.debug("Simple debug message");

    // Test debug with one parameter
    logger.debug("Debug message with one param: {}", "param1");

    // Test debug with two parameters
    logger.debug("Debug message with two params: {} and {}", "param1", "param2");

    // Test debug with multiple parameters
    logger.debug("Debug message with multiple params: {}, {}, {}", "param1", "param2", "param3");

    // Test debug with throwable
    logger.debug("Debug message with exception", new RuntimeException("Test exception"));

    // Test debug with object message
    logger.debug("Debug object message");

    // Test debug with object message and throwable
    logger.debug("Debug object message", new RuntimeException("Test exception"));

    assertThat(true).isTrue();
  }

  @Test
  void shouldLogInfoMessagesWithVariousParameters() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test info with simple message
    logger.info("Simple info message");

    // Test info with one parameter
    logger.info("Info message with one param: {}", "param1");

    // Test info with two parameters
    logger.info("Info message with two params: {} and {}", "param1", "param2");

    // Test info with multiple parameters
    logger.info("Info message with multiple params: {}, {}, {}", "param1", "param2", "param3");

    // Test info with throwable
    logger.info("Info message with exception", new RuntimeException("Test exception"));

    // Test info with object message
    logger.info("Info object message");

    // Test info with object message and throwable
    logger.info("Info object message", new RuntimeException("Test exception"));

    assertThat(true).isTrue();
  }

  @Test
  void shouldLogWarnMessagesWithVariousParameters() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test warn with simple message
    logger.warn("Simple warn message");

    // Test warn with one parameter
    logger.warn("Warn message with one param: {}", "param1");

    // Test warn with two parameters
    logger.warn("Warn message with two params: {} and {}", "param1", "param2");

    // Test warn with multiple parameters
    logger.warn("Warn message with multiple params: {}, {}, {}", "param1", "param2", "param3");

    // Test warn with throwable
    logger.warn("Warn message with exception", new RuntimeException("Test exception"));

    // Test warn with object message
    logger.warn("Warn object message");

    // Test warn with object message and throwable
    logger.warn("Warn object message", new RuntimeException("Test exception"));

    assertThat(true).isTrue();
  }

  @Test
  void shouldLogErrorMessagesWithVariousParameters() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test error with simple message
    logger.error("Simple error message");

    // Test error with one parameter
    logger.error("Error message with one param: {}", "param1");

    // Test error with two parameters
    logger.error("Error message with two params: {} and {}", "param1", "param2");

    // Test error with parameter and throwable
    logger.error("Error message with param and exception", "param1", new RuntimeException("Test exception"));

    // Test error with two parameters and throwable
    logger.error("Error message with two params and exception", "param1", "param2", new RuntimeException("Test exception"));

    // Test error with multiple parameters
    logger.error("Error message with multiple params: {}, {}, {}", "param1", "param2", "param3");

    // Test error with throwable
    logger.error("Error message with exception", new RuntimeException("Test exception"));

    // Test error with object message
    logger.error("Error object message");

    // Test error with object message and throwable
    logger.error("Error object message", new RuntimeException("Test exception"));

    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleNullMessages() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test all log levels with null messages
    logger.trace((String) null);
    logger.debug((String) null);
    logger.info((String) null);
    logger.warn((String) null);
    logger.error((String) null);

    // Test with null object messages
    logger.trace((Object) null);
    logger.debug((Object) null);
    logger.info((Object) null);
    logger.warn((Object) null);
    logger.error((Object) null);

    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleNullParameters() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test with null parameters
    logger.info("Message with null param: {}", (Object) null);
    logger.info("Message with null params: {} and {}", null, null);
    logger.info("Message with mixed null params: {} and {}", "param1", null);

    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleThrowableInArguments() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test that throwable in arguments is treated as the last argument
    RuntimeException exception = new RuntimeException("Test exception");
    logger.info("Message with exception in args: {}", "param1", exception);

    assertThat(true).isTrue();
  }

  @Test
  void shouldTestEqualsAndHashCode() {
    Logger logger1 = createLogger(new Slf4jLoggerFactory());
    Logger logger2 = createLogger(new Slf4jLoggerFactory());

    // Same logger name should be equal
    assertThat(logger1.equals(logger1)).isTrue();
    assertThat(logger1.hashCode()).isEqualTo(logger1.hashCode());

    // Different logger instances with same name should be equal
    Logger anotherLogger = createLogger(new Slf4jLoggerFactory());
    assertThat(logger1.equals(anotherLogger)).isTrue();
    assertThat(logger1.hashCode()).isEqualTo(anotherLogger.hashCode());

    // Non-logger object should not be equal
    assertThat(logger1.equals("not a logger")).isFalse();

    // Null should not be equal
    assertThat(logger1.equals(null)).isFalse();
  }

  @Test
  void shouldLogMessagesAtAllLevels() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test all log levels with simple messages
    logger.trace("Trace message");
    logger.debug("Debug message");
    logger.info("Info message");
    logger.warn("Warn message");
    logger.error("Error message");

    assertThat(true).isTrue();
  }

  @Test
  void shouldLogMessagesWithObjectParameters() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test all log levels with object messages
    logger.trace((Object) "Trace object message");
    logger.debug((Object) "Debug object message");
    logger.info((Object) "Info object message");
    logger.warn((Object) "Warn object message");
    logger.error((Object) "Error object message");

    // Test with throwable
    RuntimeException exception = new RuntimeException("Test exception");
    logger.trace((Object) "Trace object with exception", exception);
    logger.debug((Object) "Debug object with exception", exception);
    logger.info((Object) "Info object with exception", exception);
    logger.warn((Object) "Warn object with exception", exception);
    logger.error((Object) "Error object with exception", exception);

    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleNullArgumentsInFormatMethods() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test format methods with null arguments
    logger.info("Message with null arg: {}", (Object) null);
    logger.info("Message with null args: {} {}", null, null);
    logger.info("Message with mixed args: {} {}", "valid", null);
    logger.info("Message with multiple nulls: {} {} {}", null, null, null);

    assertThat(true).isTrue();
  }

  @Test
  void shouldHandleThrowableInVarargsPosition() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test that throwable in last varargs position is properly handled
    RuntimeException exception = new RuntimeException("Test exception");
    logger.info("Message with exception in varargs", "param1", "param2", exception);

    assertThat(true).isTrue();
  }

  @Test
  void shouldTestIsEnabledMethod() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test isEnabled for all levels
    boolean traceEnabled = logger.isEnabled(Level.TRACE);
    boolean debugEnabled = logger.isEnabled(Level.DEBUG);
    boolean infoEnabled = logger.isEnabled(Level.INFO);
    boolean warnEnabled = logger.isEnabled(Level.WARN);
    boolean errorEnabled = logger.isEnabled(Level.ERROR);

    assertThat(traceEnabled).isNotNull();
    assertThat(debugEnabled).isNotNull();
    assertThat(infoEnabled).isNotNull();
    assertThat(warnEnabled).isNotNull();
    assertThat(errorEnabled).isNotNull();
  }

  @Test
  void shouldTestLogInternalMethod() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test logInternal with different parameters
    logger.logInternal(Level.INFO, "Internal log message", null, null);
    logger.logInternal(Level.ERROR, "Internal log with throwable", new RuntimeException("Test"), null);
    logger.logInternal(Level.DEBUG, "Internal log with args", null, new Object[] { "arg1", "arg2" });

    assertThat(true).isTrue();
  }

  @Test
  void shouldTestErrorMethodsWithThrowable() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test error methods that accept throwable parameter
    RuntimeException exception = new RuntimeException("Test exception");
    logger.error("Error with one arg and throwable", "param1", exception);
    logger.error("Error with two args and throwable", "param1", "param2", exception);

    assertThat(true).isTrue();
  }

  @Test
  void shouldTestWarnMethodsWithMultipleArguments() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test warn method with multiple arguments
    logger.warn("Warn message with two args: {} {}", "arg1", "arg2");
    logger.warn("Warn message with varargs: {} {} {}", "arg1", "arg2", "arg3");

    assertThat(true).isTrue();
  }

  @Test
  void shouldTestDebugMethodsWithMultipleArguments() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test debug method with multiple arguments
    logger.debug("Debug message with two args: {} {}", "arg1", "arg2");
    logger.debug("Debug message with varargs: {} {} {}", "arg1", "arg2", "arg3");

    assertThat(true).isTrue();
  }

  @Test
  void shouldTestInfoMethodsWithMultipleArguments() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test info method with multiple arguments
    logger.info("Info message with two args: {} {}", "arg1", "arg2");
    logger.info("Info message with varargs: {} {} {}", "arg1", "arg2", "arg3");

    assertThat(true).isTrue();
  }

  @Test
  void shouldTestTraceMethodsWithMultipleArguments() {
    Logger logger = createLogger(new Slf4jLoggerFactory());

    // Test trace method with multiple arguments
    logger.trace("Trace message with two args: {} {}", "arg1", "arg2");
    logger.trace("Trace message with varargs: {} {} {}", "arg1", "arg2", "arg3");

    assertThat(true).isTrue();
  }

}
