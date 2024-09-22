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

package cn.taketoday.framework.logging.logback;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Context;
import ch.qos.logback.core.ContextBase;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.logging.structured.StructuredLogFormatter;
import cn.taketoday.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StructuredLogEncoder}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 */
class StructuredLogEncoderTests extends AbstractStructuredLoggingTests {

  private StructuredLogEncoder encoder;

  private Context loggerContext;

  private MockEnvironment environment;

  @Override
  @BeforeEach
  void setUp() {
    super.setUp();
    this.environment = new MockEnvironment();
    this.loggerContext = new ContextBase();
    this.loggerContext.putObject(Environment.class.getName(), this.environment);
    this.encoder = new StructuredLogEncoder();
    this.encoder.setContext(this.loggerContext);
  }

  @Override
  @AfterEach
  void tearDown() {
    super.tearDown();
    this.encoder.stop();
  }

  @Test
  void shouldSupportEcsCommonFormat() {
    this.encoder.setFormat("ecs");
    this.encoder.start();
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Collections.emptyMap());
    String json = encode(event);
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsKey("ecs.version");
  }

  @Test
  void shouldSupportLogstashCommonFormat() {
    this.encoder.setFormat("logstash");
    this.encoder.start();
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Collections.emptyMap());
    String json = encode(event);
    Map<String, Object> deserialized = deserialize(json);
    assertThat(deserialized).containsKey("@version");
  }

  @Test
  void shouldSupportCustomFormat() {
    this.encoder.setFormat(CustomLogbackStructuredLoggingFormatter.class.getName());
    this.encoder.start();
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Collections.emptyMap());
    String format = encode(event);
    assertThat(format).isEqualTo("custom-format");
  }

  @Test
  void shouldInjectCustomFormatConstructorParameters() {
    this.environment.setProperty("app.pid", "42");
    this.encoder.setFormat(CustomLogbackStructuredLoggingFormatterWithInjection.class.getName());
    this.encoder.start();
    LoggingEvent event = createEvent();
    event.setMDCPropertyMap(Collections.emptyMap());
    String format = encode(event);
    assertThat(format).isEqualTo("custom-format-with-injection pid=42 hasThrowableProxyConverter=true");
  }

  @Test
  void shouldCheckTypeArgument() {
    assertThatIllegalArgumentException().isThrownBy(() -> {
      this.encoder.setFormat(CustomLogbackStructuredLoggingFormatterWrongType.class.getName());
      this.encoder.start();
    }).withMessageContaining("must be ch.qos.logback.classic.spi.ILoggingEvent but was java.lang.String");
  }

  @Test
  void shouldCheckTypeArgumentWithRawType() {
    assertThatIllegalArgumentException().isThrownBy(() -> {
      this.encoder.setFormat(CustomLogbackStructuredLoggingFormatterRawType.class.getName());
      this.encoder.start();
    }).withMessageContaining("must be ch.qos.logback.classic.spi.ILoggingEvent but was null");
  }

  @Test
  void shouldFailIfNoCommonOrCustomFormatIsSet() {
    assertThatIllegalArgumentException().isThrownBy(() -> {
              this.encoder.setFormat("does-not-exist");
              this.encoder.start();
            })
            .withMessageContaining(
                    "Unknown format 'does-not-exist'. Values can be a valid fully-qualified class name or one of the common formats: [ecs, gelf, logstash]");
  }

  private String encode(LoggingEvent event) {
    return new String(this.encoder.encode(event), StandardCharsets.UTF_8);
  }

  static final class CustomLogbackStructuredLoggingFormatter implements StructuredLogFormatter<ILoggingEvent> {

    @Override
    public String format(ILoggingEvent event) {
      return "custom-format";
    }

  }

  static final class CustomLogbackStructuredLoggingFormatterWithInjection
          implements StructuredLogFormatter<ILoggingEvent> {

    private final Environment environment;

    private final ThrowableProxyConverter throwableProxyConverter;

    CustomLogbackStructuredLoggingFormatterWithInjection(Environment environment,
            ThrowableProxyConverter throwableProxyConverter) {
      this.environment = environment;
      this.throwableProxyConverter = throwableProxyConverter;
    }

    @Override
    public String format(ILoggingEvent event) {
      boolean hasThrowableProxyConverter = this.throwableProxyConverter != null;
      return "custom-format-with-injection pid=" + this.environment.getProperty("app.pid")
              + " hasThrowableProxyConverter=" + hasThrowableProxyConverter;
    }

  }

  static final class CustomLogbackStructuredLoggingFormatterWrongType implements StructuredLogFormatter<String> {

    @Override
    public String format(String event) {
      return event;
    }

  }

  @SuppressWarnings("rawtypes")
  static final class CustomLogbackStructuredLoggingFormatterRawType implements StructuredLogFormatter {

    @Override
    public String format(Object event) {
      return "";
    }

  }

}
