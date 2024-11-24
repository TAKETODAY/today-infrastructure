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

import java.util.Collections;
import java.util.Map;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import infra.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ElasticCommonSchemaStructuredLogFormatter}.
 *
 * @author Moritz Halbritter
 */
class ElasticCommonSchemaStructuredLogFormatterTests extends AbstractStructuredLoggingTests {

  private ElasticCommonSchemaStructuredLogFormatter formatter;

  @Override
  @BeforeEach
  void setUp() {
    super.setUp();
    MockEnvironment environment = new MockEnvironment();
    environment.setProperty("logging.structured.ecs.service.name", "name");
    environment.setProperty("logging.structured.ecs.service.version", "1.0.0");
    environment.setProperty("logging.structured.ecs.service.environment", "test");
    environment.setProperty("logging.structured.ecs.service.node-name", "node-1");
    environment.setProperty("app.pid", "1");
    this.formatter = new ElasticCommonSchemaStructuredLogFormatter(environment, getThrowableProxyConverter());
  }

  @Test
  void shouldFormat() {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Map.of("mdc-1", "mdc-v-1"));
    event.setKeyValuePairs(keyValuePairs("kv-1", "kv-v-1"));
    String json = this.formatter.format(event);
    assertThat(json).endsWith("\n");
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsExactlyInAnyOrderEntriesOf(map("@timestamp", "2024-07-02T08:49:53Z",
            "log.level", "INFO", "process.pid", 1, "process.thread.name", "main", "service.name", "name",
            "service.version", "1.0.0", "service.environment", "test", "service.node.name", "node-1", "log.logger",
            "org.example.Test", "message", "message", "mdc-1", "mdc-v-1", "kv-1", "kv-v-1", "ecs.version", "8.11"));
  }

  @Test
  void shouldFormatException() {
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Collections.emptyMap());
    event.setThrowableProxy(new ThrowableProxy(new RuntimeException("Boom")));
    String json = this.formatter.format(event);
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized)
            .containsAllEntriesOf(map("error.type", "java.lang.RuntimeException", "error.message", "Boom"));
    String stackTrace = (String) deserialized.get("error.stack_trace");
    assertThat(stackTrace).startsWith(
            "java.lang.RuntimeException: Boom%n\tat infra.logging.logback.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException"
                    .formatted());
    assertThat(json).contains(
            "java.lang.RuntimeException: Boom%n\\tat infra.logging.logback.ElasticCommonSchemaStructuredLogFormatterTests.shouldFormatException"
                    .formatted()
                    .replace("\n", "\\n")
                    .replace("\r", "\\r"));
  }

}
