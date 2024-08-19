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

import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import cn.taketoday.framework.json.JsonWriter;
import cn.taketoday.framework.json.JsonWriter.PairExtractor;
import cn.taketoday.framework.logging.structured.CommonStructuredLogFormat;
import cn.taketoday.framework.logging.structured.JsonWriterStructuredLogFormatter;
import cn.taketoday.framework.logging.structured.StructuredLogFormatter;

/**
 * Logback {@link StructuredLogFormatter} for {@link CommonStructuredLogFormat#LOGSTASH}.
 *
 * @author Moritz Halbritter
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class LogstashStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

  private static final PairExtractor<KeyValuePair> keyValuePairExtractor =
          PairExtractor.of(pair -> pair.key, pair -> pair.value);

  LogstashStructuredLogFormatter(ThrowableProxyConverter throwableProxyConverter) {
    super((members) -> jsonMembers(throwableProxyConverter, members));
  }

  private static void jsonMembers(ThrowableProxyConverter throwableProxyConverter,
          JsonWriter.Members<ILoggingEvent> members) {
    members.add("@timestamp", ILoggingEvent::getInstant).as(LogstashStructuredLogFormatter::asTimestamp);
    members.add("@version", "1");
    members.add("message", ILoggingEvent::getFormattedMessage);
    members.add("logger_name", ILoggingEvent::getLoggerName);
    members.add("thread_name", ILoggingEvent::getThreadName);
    members.add("level", ILoggingEvent::getLevel);
    members.add("level_value", ILoggingEvent::getLevel).as(Level::toInt);
    members.addMapEntries(ILoggingEvent::getMDCPropertyMap);
    members.from(ILoggingEvent::getKeyValuePairs)
            .whenNotEmpty()
            .usingExtractedPairs(Iterable::forEach, keyValuePairExtractor);
    members.add("tags", ILoggingEvent::getMarkerList)
            .whenNotNull()
            .as(LogstashStructuredLogFormatter::getMarkers)
            .whenNotEmpty();
    members.add("stack_trace", (event) -> event)
            .whenNotNull(ILoggingEvent::getThrowableProxy)
            .as(throwableProxyConverter::convert);
  }

  private static String asTimestamp(Instant instant) {
    OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneId.systemDefault());
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
  }

  private static Set<String> getMarkers(List<Marker> markers) {
    Set<String> result = new LinkedHashSet<>();
    addMarkers(result, markers.iterator());
    return result;
  }

  private static void addMarkers(Set<String> result, Iterator<Marker> iterator) {
    while (iterator.hasNext()) {
      Marker marker = iterator.next();
      result.add(marker.getName());
      if (marker.hasReferences()) {
        addMarkers(result, marker.iterator());
      }
    }
  }

}
