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

package infra.app.logging.logback;

import org.slf4j.event.KeyValuePair;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.util.LevelToSyslogSeverity;
import infra.app.json.JsonWriter.Members;
import infra.app.json.WritableJson;
import infra.app.logging.structured.CommonStructuredLogFormat;
import infra.app.logging.structured.GraylogExtendedLogFormatService;
import infra.app.logging.structured.JsonWriterStructuredLogFormatter;
import infra.app.logging.structured.StructuredLogFormatter;
import infra.core.env.ConfigurableEnvironment;
import infra.lang.Assert;
import infra.logging.LogMessage;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.CollectionUtils;
import infra.util.StringUtils;

/**
 * Logback {@link StructuredLogFormatter} for
 * {@link CommonStructuredLogFormat#GRAYLOG_EXTENDED_LOG_FORMAT}. Supports GELF version
 * 1.1.
 *
 * @author Samuel Lissner
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
class GraylogExtendedLogFormatStructuredLogFormatter extends JsonWriterStructuredLogFormatter<ILoggingEvent> {

  private static final Logger logger = LoggerFactory.getLogger(GraylogExtendedLogFormatStructuredLogFormatter.class);

  /**
   * Allowed characters in field names are any word character (letter, number,
   * underscore), dashes and dots.
   */
  private static final Pattern FIELD_NAME_VALID_PATTERN = Pattern.compile("^[\\w.\\-]*$");

  /**
   * Libraries SHOULD not allow to send id as additional field ("_id"). Graylog server
   * nodes omit this field automatically.
   */
  private static final Set<String> ADDITIONAL_FIELD_ILLEGAL_KEYS = Set.of("id", "_id");

  GraylogExtendedLogFormatStructuredLogFormatter(ConfigurableEnvironment environment, ThrowableProxyConverter throwableProxyConverter) {
    super((members) -> jsonMembers(environment, throwableProxyConverter, members));
  }

  private static void jsonMembers(ConfigurableEnvironment environment, ThrowableProxyConverter throwableProxyConverter,
          Members<ILoggingEvent> members) {
    members.add("version", "1.1");
    members.add("short_message", ILoggingEvent::getFormattedMessage)
            .as(GraylogExtendedLogFormatStructuredLogFormatter::getMessageText);
    members.add("timestamp", ILoggingEvent::getTimeStamp)
            .as(GraylogExtendedLogFormatStructuredLogFormatter::formatTimeStamp);
    members.add("level", LevelToSyslogSeverity::convert);
    members.add("_level_name", ILoggingEvent::getLevel);
    members.add("_process_pid", environment.getProperty("app.pid", Long.class))
            .when(Objects::nonNull);
    members.add("_process_thread_name", ILoggingEvent::getThreadName);
    GraylogExtendedLogFormatService.get(environment).jsonMembers(members);
    members.add("_log_logger", ILoggingEvent::getLoggerName);
    members.from(ILoggingEvent::getMDCPropertyMap)
            .when(CollectionUtils::isNotEmpty)
            .usingPairs((mdc, pairs) -> mdc.forEach((key, value) -> createAdditionalField(key, value, pairs)));
    members.from(ILoggingEvent::getKeyValuePairs)
            .when(CollectionUtils::isNotEmpty)
            .usingPairs(GraylogExtendedLogFormatStructuredLogFormatter::createAdditionalField);
    members.add()
            .whenNotNull(ILoggingEvent::getThrowableProxy)
            .usingMembers((throwableMembers) -> throwableMembers(throwableMembers, throwableProxyConverter));
  }

  private static String getMessageText(String formattedMessage) {
    // Always return text as a blank message will lead to a error as of Graylog v6
    return (!StringUtils.hasText(formattedMessage)) ? "(blank)" : formattedMessage;
  }

  /**
   * GELF requires "seconds since UNIX epoch with optional <b>decimal places for
   * milliseconds</b>". To comply with this requirement, we format a POSIX timestamp
   * with millisecond precision as e.g. "1725459730385" -> "1725459730.385"
   *
   * @param timeStamp the timestamp of the log message
   * @return the timestamp formatted as string with millisecond precision
   */
  private static WritableJson formatTimeStamp(long timeStamp) {
    return (out) -> out.append(new BigDecimal(timeStamp).movePointLeft(3).toPlainString());
  }

  private static void throwableMembers(Members<ILoggingEvent> members, ThrowableProxyConverter throwableProxyConverter) {
    members.add("full_message", (event) -> formatFullMessageWithThrowable(throwableProxyConverter, event));
    members.add("_error_type", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getClassName);
    members.add("_error_stack_trace", throwableProxyConverter::convert);
    members.add("_error_message", ILoggingEvent::getThrowableProxy).as(IThrowableProxy::getMessage);
  }

  private static String formatFullMessageWithThrowable(ThrowableProxyConverter throwableProxyConverter, ILoggingEvent event) {
    return event.getFormattedMessage() + "\n\n" + throwableProxyConverter.convert(event);
  }

  private static void createAdditionalField(List<KeyValuePair> keyValuePairs, BiConsumer<Object, Object> pairs) {
    keyValuePairs.forEach((keyValuePair) -> createAdditionalField(keyValuePair.key, keyValuePair.value, pairs));
  }

  private static void createAdditionalField(String name, Object value, BiConsumer<Object, Object> pairs) {
    Assert.notNull(name, "fieldName is required");
    if (!FIELD_NAME_VALID_PATTERN.matcher(name).matches()) {
      logger.warn(LogMessage.format("'{}' is not a valid field name according to GELF standard", name));
      return;
    }
    if (ADDITIONAL_FIELD_ILLEGAL_KEYS.contains(name)) {
      logger.warn(LogMessage.format("'{}' is an illegal field name according to GELF standard", name));
      return;
    }
    pairs.accept(asAdditionalFieldName(name), value);
  }

  private static Object asAdditionalFieldName(String name) {
    return (!name.startsWith("_")) ? "_" + name : name;
  }

}
