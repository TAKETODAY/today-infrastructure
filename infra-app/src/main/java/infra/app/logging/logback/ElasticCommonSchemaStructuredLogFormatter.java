/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.logging.logback;

import org.slf4j.event.KeyValuePair;

import java.util.Objects;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import infra.app.json.JsonWriter;
import infra.app.json.JsonWriter.PairExtractor;
import infra.app.logging.structured.CommonStructuredLogFormat;
import infra.app.logging.structured.ElasticCommonSchemaService;
import infra.app.logging.structured.JsonWriterStructuredLogFormatter;
import infra.app.logging.structured.StructuredLogFormatter;
import infra.core.env.ConfigurableEnvironment;

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
