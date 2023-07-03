/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.framework.logging.logback;

import java.nio.charset.Charset;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;
import cn.taketoday.framework.logging.LogFile;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ExceptionUtils;

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

  @Nullable
  private final LogFile logFile;

  DefaultLogbackConfiguration(@Nullable LogFile logFile) {
    this.logFile = logFile;
  }

  void apply(LogbackConfigurator config) {
    synchronized(config.getConfigurationLock()) {
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
  }

  private void defaults(LogbackConfigurator config) {
    config.conversionRule("clr", ColorConverter.class);
    config.conversionRule("correlationId", CorrelationIdConverter.class);
    config.conversionRule("wex", WhitespaceThrowableProxyConverter.class);
    config.conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class);
    config.getContext()
            .putProperty("CONSOLE_LOG_PATTERN", resolve(config, "${CONSOLE_LOG_PATTERN:-"
                    + "%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}}){faint} %clr(${LOG_LEVEL_PATTERN:-%5p}) "
                    + "%clr(${PID:- }){magenta} %clr(---){faint} %clr(${LOGGED_APPLICATION_NAME:-}[%15.15t]){faint} "
                    + "%clr(${LOG_CORRELATION_PATTERN:-}){faint}%clr(%-40.40logger{39}){cyan} "
                    + "%clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"));
    String defaultCharset = Charset.defaultCharset().name();
    config.getContext()
            .putProperty("CONSOLE_LOG_CHARSET", resolve(config, "${CONSOLE_LOG_CHARSET:-" + defaultCharset + "}"));
    config.getContext().putProperty("CONSOLE_LOG_THRESHOLD", resolve(config, "${CONSOLE_LOG_THRESHOLD:-TRACE}"));
    config.getContext()
            .putProperty("FILE_LOG_PATTERN", resolve(config, "${FILE_LOG_PATTERN:-"
                    + "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}} ${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- ${LOGGED_APPLICATION_NAME:-}[%t] "
                    + "${LOG_CORRELATION_PATTERN:-}"
                    + "%-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}"));
    config.getContext()
            .putProperty("FILE_LOG_CHARSET", resolve(config, "${FILE_LOG_CHARSET:-" + defaultCharset + "}"));
    config.getContext().putProperty("FILE_LOG_THRESHOLD", resolve(config, "${FILE_LOG_THRESHOLD:-TRACE}"));
    config.logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
    config.logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
    config.logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
    config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
    config.logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
    config.logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
    config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
    config.logger("cn.taketoday.actuate.endpoint.jmx", Level.WARN);
  }

  private Appender<ILoggingEvent> consoleAppender(LogbackConfigurator config) {
    ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    ThresholdFilter filter = new ThresholdFilter();
    filter.setLevel(resolve(config, "${CONSOLE_LOG_THRESHOLD}"));
    appender.addFilter(filter);
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setPattern(resolve(config, "${CONSOLE_LOG_PATTERN}"));
    encoder.setCharset(resolveCharset(config, "${CONSOLE_LOG_CHARSET}"));
    config.start(encoder);
    appender.setEncoder(encoder);
    config.appender("CONSOLE", appender);
    return appender;
  }

  private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config, String logFile) {
    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    ThresholdFilter filter = new ThresholdFilter();
    filter.setLevel(resolve(config, "${FILE_LOG_THRESHOLD}"));
    appender.addFilter(filter);
    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
    encoder.setPattern(resolve(config, "${FILE_LOG_PATTERN}"));
    encoder.setCharset(resolveCharset(config, "${FILE_LOG_CHARSET}"));
    appender.setEncoder(encoder);
    config.start(encoder);
    appender.setFile(logFile);
    setRollingPolicy(appender, config);
    config.appender("FILE", appender);
    return appender;
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
      throw ExceptionUtils.sneakyThrow(ex);
    }
  }

}
