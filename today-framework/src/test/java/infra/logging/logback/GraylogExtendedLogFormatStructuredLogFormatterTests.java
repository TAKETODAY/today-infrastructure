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

package infra.logging.logback;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collections;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraylogExtendedLogFormatStructuredLogFormatter}.
 *
 * @author Samuel Lissner
 * @author Moritz Halbritter
 */
@ExtendWith(OutputCaptureExtension.class)
class GraylogExtendedLogFormatStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

  private GraylogExtendedLogFormatStructuredLogFormatter formatter;

  @Override
  @BeforeEach
  void setUp() {
    super.setUp();
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("logging.structured.gelf.service.name", "name");
    environment.setProperty("logging.structured.gelf.service.version", "1.0.0");
    environment.setProperty("app.pid", "1");
    this.formatter = new GraylogExtendedLogFormatStructuredLogFormatter(environment, getThrowableProxyConverter());
  }

  @Test
  void shouldFormat() {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Map.of("mdc-1", "mdc-v-1"));
    event.setKeyValuePairs(keyValuePairs("kv-1", "kv-v-1"));
    String json = this.formatter.format(event);
    assertThat(json).endsWith("\n");
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(
            map("version", "1.1", "host", "name", "timestamp", 1719910193.0, "level", 6, "_level_name", "INFO",
                    "_process_pid", 1, "_process_thread_name", "main", "_service_version", "1.0.0", "_log_logger",
                    "org.example.Test", "short_message", "message", "_mdc-1", "mdc-v-1", "_kv-1", "kv-v-1"));
  }

  @Test
  void shouldFormatMillisecondsInTimestamp() {
    LoggingEvent event = createEvent();
    event.setTimeStamp(1719910193123L);
    event.setMDCPropertyMap(Collections.emptyMap());
    String json = this.formatter.format(event);
    assertThat(json).contains("\"timestamp\":1719910193.123");
    assertThat(json).endsWith("\n");
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("version", "1.1", "host", "name", "timestamp",
            1719910193.123, "level", 6, "_level_name", "INFO", "_process_pid", 1, "_process_thread_name", "main",
            "_service_version", "1.0.0", "_log_logger", "org.example.Test", "short_message", "message"));
  }

  @Test
  void shouldNotAllowInvalidFieldNames(CapturedOutput output) {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Map.of("/", "value"));
    String json = this.formatter.format(event);
    assertThat(json).endsWith("\n");
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("version", "1.1", "host", "name", "timestamp",
            1719910193.0, "level", 6, "_level_name", "INFO", "_process_pid", 1, "_process_thread_name", "main",
            "_service_version", "1.0.0", "_log_logger", "org.example.Test", "short_message", "message"));
    assertThat(output).contains("'/' is not a valid field name according to GELF standard");
  }

  @Test
  void shouldNotAllowIllegalFieldNames(CapturedOutput output) {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Map.of("id", "1"));
    String json = this.formatter.format(event);
    assertThat(json).endsWith("\n");
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("version", "1.1", "host", "name", "timestamp",
            1719910193.0, "level", 6, "_level_name", "INFO", "_process_pid", 1, "_process_thread_name", "main",
            "_service_version", "1.0.0", "_log_logger", "org.example.Test", "short_message", "message"));
    assertThat(output).contains("'id' is an illegal field name according to GELF standard");
  }

  @Test
  void shouldNotAddDoubleUnderscoreToCustomFields() {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Map.of("_custom", "value"));
    String json = this.formatter.format(event);
    assertThat(json).endsWith("\n");
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(
            map("version", "1.1", "host", "name", "timestamp", 1719910193.0, "level", 6, "_level_name", "INFO",
                    "_process_pid", 1, "_process_thread_name", "main", "_service_version", "1.0.0", "_log_logger",
                    "org.example.Test", "short_message", "message", "_custom", "value"));
  }

  @Test
  void shouldFormatException() {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Collections.emptyMap());
    event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
    String json = this.formatter.format(event);
    Map<String, Object> deserialized = deserialize(json);
    String fullMessage = (String) deserialized.get("full_message");
    String stackTrace = (String) deserialized.get("_error_stack_trace");
    assertThat(fullMessage).startsWith(
            "message\n\njava.lang.RuntimeException: Boom%n\tat infra.logging.logback.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException"
                    .formatted());
    assertThat(deserialized)
            .containsAllEntriesOf(map("_error_type", "java.lang.RuntimeException", "_error_message", "Boom"));
    assertThat(stackTrace).startsWith(
            "java.lang.RuntimeException: Boom%n\tat infra.logging.logback.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException"
                    .formatted());
    assertThat(json).contains(
            "java.lang.RuntimeException: Boom%n\\tat infra.logging.logback.GraylogExtendedLogFormatStructuredLogFormatterTests.shouldFormatException"
                    .formatted()
                    .replace("\n", "\\n")
                    .replace("\r", "\\r"));
  }

}
