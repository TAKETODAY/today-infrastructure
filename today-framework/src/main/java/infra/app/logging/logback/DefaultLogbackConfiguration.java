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

import java.nio.charset.Charset;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import infra.app.logging.LogFile;
import infra.core.ansi.AnsiColor;
import infra.core.ansi.AnsiElement;
import infra.core.ansi.AnsiStyle;
import infra.lang.Nullable;
import infra.util.StringUtils;

/**
 * Default logback configuration used by Infra. Uses {@link LogbackConfigurator} to
 * improve startup time. See also the {@code base.xml}, {@code defaults.xml},
 * {@code console-appender.xml} and {@code file-appender.xml} files provided for classic
 * {@code logback.xml} use.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DefaultLogbackConfiguration {

  private static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

  private static final String NAME_AND_GROUP = "%esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}";

  private static final String DATETIME = "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}}";

  private static final String DEFAULT_CONSOLE_LOG_PATTERN = faint(DATETIME) + " "
          + colorByLevel("${LOG_LEVEL_PATTERN:-%5p}") + " " + magenta("${PID:-}") + " "
          + faint("--- " + NAME_AND_GROUP + "[%15.15t] ${LOG_CORRELATION_PATTERN:-}") + cyan("%-40.40logger{39}")
          + " " + faint(":") + " %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}";

  static final String CONSOLE_LOG_PATTERN = "${CONSOLE_LOG_PATTERN:-" + DEFAULT_CONSOLE_LOG_PATTERN;

  private static final String DEFAULT_FILE_LOG_PATTERN = DATETIME + " ${LOG_LEVEL_PATTERN:-%5p} ${PID:-} --- "
          + NAME_AND_GROUP + "[%t] ${LOG_CORRELATION_PATTERN:-}"
          + "%-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}";

  static final String FILE_LOG_PATTERN = "${FILE_LOG_PATTERN:-" + DEFAULT_FILE_LOG_PATTERN;

  @Nullable
  private final LogFile logFile;

  DefaultLogbackConfiguration(@Nullable LogFile logFile) {
    this.logFile = logFile;
  }

  void apply(LogbackConfigurator config) {
    config.getConfigurationLock().lock();
    try {
      defaults(config);
      Appender<ILoggingEvent> consoleAppender = consoleAppender(config);
      if (this.logFile != null) {
        Appender<ILoggingEvent> fileAppender = fileAppender(config, this.logFile.toString());
        config.root(Level.INFO, consoleAppender, fileAppender);
      }
      else {
        config.root(Level.INFO, consoleAppender);
      }
    }
    finally {
      config.getConfigurationLock().unlock();
    }
  }

  private void defaults(LogbackConfigurator config) {
    config.conversionRule("clr", ColorConverter.class, ColorConverter::new);
    config.conversionRule("correlationId", CorrelationIdConverter.class, CorrelationIdConverter::new);
    config.conversionRule("esb", EnclosedInSquareBracketsConverter.class, EnclosedInSquareBracketsConverter::new);
    config.conversionRule("wex", WhitespaceThrowableProxyConverter.class, WhitespaceThrowableProxyConverter::new);
    config.conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class,
            ExtendedWhitespaceThrowableProxyConverter::new);

    putProperty(config, "CONSOLE_LOG_PATTERN", CONSOLE_LOG_PATTERN);
    putProperty(config, "CONSOLE_LOG_CHARSET", "${CONSOLE_LOG_CHARSET:-" + DEFAULT_CHARSET + "}");
    putProperty(config, "CONSOLE_LOG_THRESHOLD", "${CONSOLE_LOG_THRESHOLD:-TRACE}");
    putProperty(config, "CONSOLE_LOG_STRUCTURED_FORMAT", "${CONSOLE_LOG_STRUCTURED_FORMAT:-}");
    putProperty(config, "FILE_LOG_PATTERN", FILE_LOG_PATTERN);
    putProperty(config, "FILE_LOG_CHARSET", "${FILE_LOG_CHARSET:-" + DEFAULT_CHARSET + "}");
    putProperty(config, "FILE_LOG_THRESHOLD", "${FILE_LOG_THRESHOLD:-TRACE}");
    putProperty(config, "FILE_LOG_STRUCTURED_FORMAT", "${FILE_LOG_STRUCTURED_FORMAT:-}");
    config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
    config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
  }

  void putProperty(LogbackConfigurator config, String name, String val) {
    config.getContext().putProperty(name, resolve(config, val));
  }

  private Appender<ILoggingEvent> consoleAppender(LogbackConfigurator config) {
    ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    createAppender(config, appender, "CONSOLE");
    config.appender("CONSOLE", appender);
    return appender;
  }

  private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config, String logFile) {
    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    createAppender(config, appender, "FILE");
    appender.setFile(logFile);
    setRollingPolicy(appender, config);
    config.appender("FILE", appender);
    return appender;
  }

  private void createAppender(LogbackConfigurator config, OutputStreamAppender<ILoggingEvent> appender, String type) {
    appender.addFilter(createThresholdFilter(config, type));
    Encoder<ILoggingEvent> encoder = createEncoder(config, type);
    appender.setEncoder(encoder);
    config.start(encoder);
  }

  private ThresholdFilter createThresholdFilter(LogbackConfigurator config, String type) {
    ThresholdFilter filter = new ThresholdFilter();
    filter.setLevel(resolve(config, "${" + type + "_LOG_THRESHOLD}"));
    filter.start();
    return filter;
  }

  private Encoder<ILoggingEvent> createEncoder(LogbackConfigurator config, String type) {
    Charset charset = resolveCharset(config, "${" + type + "_LOG_CHARSET}");
    String structuredLogFormat = resolve(config, "${" + type + "_LOG_STRUCTURED_FORMAT}");
    if (StringUtils.isNotEmpty(structuredLogFormat)) {
      StructuredLogEncoder encoder = createStructuredLogEncoder(config, structuredLogFormat);
      encoder.setCharset(charset);
      return encoder;
    }
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setCharset(charset);
    encoder.setPattern(resolve(config, "${" + type + "_LOG_PATTERN}"));
    return encoder;
  }

  private StructuredLogEncoder createStructuredLogEncoder(LogbackConfigurator config, String format) {
    StructuredLogEncoder encoder = new StructuredLogEncoder();
    encoder.setFormat(format);
    return encoder;
  }

  private void setRollingPolicy(RollingFileAppender<ILoggingEvent> appender, LogbackConfigurator config) {
    SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
    rollingPolicy.setContext(config.getContext());
    rollingPolicy.setFileNamePattern(
            resolve(config, "${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}"));
    rollingPolicy
            .setCleanHistoryOnStart(resolveBoolean(config, "${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}"));
    rollingPolicy.setMaxFileSize(resolveFileSize(config, "${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}"));
    rollingPolicy.setTotalSizeCap(resolveFileSize(config, "${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}"));
    rollingPolicy.setMaxHistory(resolveInt(config, "${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}"));
    appender.setRollingPolicy(rollingPolicy);
    rollingPolicy.setParent(appender);
    config.start(rollingPolicy);
  }

  private boolean resolveBoolean(LogbackConfigurator config, String val) {
    return Boolean.parseBoolean(resolve(config, val));
  }

  private int resolveInt(LogbackConfigurator config, String val) {
    return Integer.parseInt(resolve(config, val));
  }

  private FileSize resolveFileSize(LogbackConfigurator config, String val) {
    return FileSize.valueOf(resolve(config, val));
  }

  private Charset resolveCharset(LogbackConfigurator config, String val) {
    return Charset.forName(resolve(config, val));
  }

  private String resolve(LogbackConfigurator config, String val) {
    try {
      return OptionHelper.substVars(val, config.getContext());
    }
    catch (ScanException ex) {
      throw new RuntimeException(ex);
    }
  }

  private static String faint(String value) {
    return color(value, AnsiStyle.FAINT);
  }

  private static String cyan(String value) {
    return color(value, AnsiColor.CYAN);
  }

  private static String magenta(String value) {
    return color(value, AnsiColor.MAGENTA);
  }

  private static String colorByLevel(String value) {
    return "%clr(" + value + "){}";
  }

  private static String color(String value, AnsiElement ansiElement) {
    return "%clr(" + value + "){" + ColorConverter.getName(ansiElement) + "}";
  }

}
