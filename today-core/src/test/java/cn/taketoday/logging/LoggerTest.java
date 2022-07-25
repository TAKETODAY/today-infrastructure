/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.logging;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author TODAY <br>
 * 2019-12-06 23:09
 */
@Order(Integer.MAX_VALUE)
public class LoggerTest {

  @Test
  public void testSlf4jLogger() throws Exception {
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

    assertThat(msg.charAt(1)).isEqualTo(' ');
    assertThat(msg.length()).isEqualTo(3);
    assertThat(msg.subSequence(0, 1)).isEqualTo("a");

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
}
