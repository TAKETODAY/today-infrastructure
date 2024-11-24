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

import org.slf4j.event.KeyValuePair;

import java.util.Objects;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import infra.core.env.ConfigurableEnvironment;
import infra.app.json.JsonWriter;
import infra.app.json.JsonWriter.PairExtractor;
import infra.logging.structured.CommonStructuredLogFormat;
import infra.logging.structured.ElasticCommonSchemaService;
import infra.logging.structured.JsonWriterStructuredLogFormatter;
import infra.logging.structured.StructuredLogFormatter;

/**
 * Logback {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#ELASTIC_COMMON_SCHEMA}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class ElasticCommonSchemaStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

  private static final PairExtractor<KeyValuePair> keyValuePairExtractor = PairExtractor.of((pair) -> pair.key,
          (pair) -> pair.value);

  ElasticCommonSchemaStructuredLogFormatter(ConfigurableEnvironment environment,
          ThrowableProxyConverter throwableProxyConverter) {
    super((members) -> jsonMembers(environment, throwableProxyConverter, members));
  }

  private static void jsonMembers(ConfigurableEnvironment environment, ThrowableProxyConverter throwableProxyConverter,
          JsonWriter.Members<ILoggingEvent> members) {
    members.add("@timestamp", ILoggingEvent::getInstant);
    members.add("log.level", ILoggingEvent::getLevel);
    members.add("process.pid", environment.getProperty("app.pid", Long.class)).when(Objects::nonNull);
    members.add("process.thread.name", ILoggingEvent::getThreadName);
    ElasticCommonSchemaService.get(environment).jsonMembers(members);
    members.add("log.logger", ILoggingEvent::getLoggerName);
    members.add("message", ILoggingEvent::getFormattedMessage);
    members.addMapEntries(ILoggingEvent::getMDCPropertyMap);
    members.from(ILoggingEvent::getKeyValuePairs)
            .whenNotEmpty()
            .usingExtractedPairs(Iterable::forEach, keyValuePairExtractor);
    members.add().whenNotNull(ILoggingEvent::getThrowableProxy).usingMembers((throwableMembers) -> {
      throwableMembers.add("error.type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
      throwableMembers.add("error.message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
      throwableMembers.add("error.stack_trace", throwableProxyConverter::convert);
    });
    members.add("ecs.version", "8.11");
  }

}
