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
import org.slf4j.Marker;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LogstashStructuredLogFormatter}.
 *
 * @author Moritz Halbritter
 */
class LogstashStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

  private LogstashStructuredLogFormatter formatter;

  @Override
  @BeforeEach
  void setUp() {
    super.setUp();
    this.formatter = new LogstashStructuredLogFormatter(getThrowableProxyConverter());
  }

  @Test
  void shouldFormat() {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Map.of("mdc-1", "mdc-v-1"));
    event.setKeyValuePairs(keyValuePairs("kv-1", "kv-v-1"));
    Marker marker1 = getMarker("marker-1");
    marker1.add(getMarker("marker-2"));
    event.addMarker(marker1);
    String json = this.formatter.format(event);
    assertThat(json).endsWith("\n");
    Map<String, Object> deserialized = deserialize(json);
    String timestamp = DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .format(OffsetDateTime.ofInstant(EVENT_TIME, ZoneId.systemDefault()));
    assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", timestamp, "@version", "1",
            "message", "message", "logger_name", "org.example.Test", "thread_name", "main", "level", "INFO",
            "level_value", 20000, "mdc-1", "mdc-v-1", "kv-1", "kv-v-1", "tags", List.of("marker-1", "marker-2")));
  }

  @Test
  void shouldFormatException() {
    LoggingEvent event = createEvent();
    event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
    event.setMDCPropertyMap(Collections.emptyMap());
    String json = this.formatter.format(event);
    Map<String, Object> deserialized = deserialize(json);
    String stackTrace = (String) deserialized.get("stack_trace");
    assertThat(stackTrace).startsWith(
            "java.lang.RuntimeException: Boom%n\tat infra.logging.logback.LogstashStructuredLogFormatterTests.shouldFormatException"
                    .formatted());
    assertThat(json).contains(
            "java.lang.RuntimeException: Boom%n\\tat infra.logging.logback.LogstashStructuredLogFormatterTests.shouldFormatException"
                    .formatted()
                    .replace("\n", "\\n")
                    .replace("\r", "\\r"));
  }

}
