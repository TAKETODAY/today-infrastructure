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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.encoder.EncoderBase;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.framework.logging.structured.CommonStructuredLogFormat;
import cn.taketoday.framework.logging.structured.StructuredLogFormatter;
import cn.taketoday.framework.logging.structured.StructuredLogFormatterFactory;
import cn.taketoday.framework.logging.structured.StructuredLogFormatterFactory.CommonFormatters;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.Instantiator.AvailableParameters;

/**
 * {@link Encoder Logback encoder} for structured logging.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see StructuredLogFormatter
 * @since 5.0
 */
public class StructuredLogEncoder extends EncoderBase<ILoggingEvent> {

  private final ThrowableProxyConverter throwableProxyConverter = new ThrowableProxyConverter();

  @Nullable
  private String format;

  private StructuredLogFormatter<ILoggingEvent> formatter;

  @Nullable
  private Charset charset = StandardCharsets.UTF_8;

  public void setFormat(@Nullable String format) {
    this.format = format;
  }

  public void setCharset(@Nullable Charset charset) {
    this.charset = charset;
  }

  @Override
  public void start() {
    Assert.state(this.format != null, "Format has not been set");
    this.formatter = createFormatter(this.format);
    super.start();
    this.throwableProxyConverter.start();
  }

  private StructuredLogFormatter<ILoggingEvent> createFormatter(String format) {
    Environment environment = (Environment) getContext().getObject(Environment.class.getName());
    Assert.state(environment != null, "Unable to find Infra Environment in logger context");
    return new StructuredLogFormatterFactory<>(ILoggingEvent.class, environment, this::addAvailableParameters,
            this::addCommonFormatters)
            .get(format);
  }

  private void addAvailableParameters(AvailableParameters availableParameters) {
    availableParameters.add(ThrowableProxyConverter.class, this.throwableProxyConverter);
  }

  private void addCommonFormatters(CommonFormatters<ILoggingEvent> commonFormatters) {
    commonFormatters.add(CommonStructuredLogFormat.ELASTIC_COMMON_SCHEMA, instantiator ->
            new ElasticCommonSchemaStructuredLogFormatter(
                    instantiator.getArg(ConfigurableEnvironment.class),
                    instantiator.getArg(ThrowableProxyConverter.class)));
    commonFormatters.add(CommonStructuredLogFormat.LOGSTASH,
            instantiator -> new LogstashStructuredLogFormatter(instantiator.getArg(ThrowableProxyConverter.class)));
  }

  @Override
  public void stop() {
    this.throwableProxyConverter.stop();
    super.stop();
  }

  @Nullable
  @Override
  public byte[] headerBytes() {
    return null;
  }

  @Override
  public byte[] encode(ILoggingEvent event) {
    return this.formatter.formatAsBytes(event, (this.charset != null) ? this.charset : StandardCharsets.UTF_8);
  }

  @Nullable
  @Override
  public byte[] footerBytes() {
    return null;
  }

}
