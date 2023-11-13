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

import java.util.HashMap;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.ContextAware;
import ch.qos.logback.core.spi.LifeCycle;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Allows programmatic configuration of logback which is usually faster than parsing XML.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class LogbackConfigurator {

  private final LoggerContext context;

  LogbackConfigurator(LoggerContext context) {
    Assert.notNull(context, "Context is required");
    this.context = context;
  }

  LoggerContext getContext() {
    return this.context;
  }

  Object getConfigurationLock() {
    return this.context.getConfigurationLock();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  void conversionRule(String conversionWord, Class<? extends Converter> converterClass) {
    Assert.hasLength(conversionWord, "Conversion word must not be empty");
    Assert.notNull(converterClass, "Converter class is required");
    Map<String, String> registry = (Map<String, String>) this.context.getObject(
            CoreConstants.PATTERN_RULE_REGISTRY);
    if (registry == null) {
      registry = new HashMap<>();
      this.context.putObject(CoreConstants.PATTERN_RULE_REGISTRY, registry);
    }
    registry.put(conversionWord, converterClass.getName());
  }

  void appender(String name, Appender<?> appender) {
    appender.setName(name);
    start(appender);
  }

  void logger(String name, @Nullable Level level) {
    logger(name, level, true);
  }

  void logger(String name, @Nullable Level level, boolean additive) {
    logger(name, level, additive, null);
  }

  void logger(String name, @Nullable Level level, boolean additive, @Nullable Appender<ILoggingEvent> appender) {
    Logger logger = this.context.getLogger(name);
    if (level != null) {
      logger.setLevel(level);
    }
    logger.setAdditive(additive);
    if (appender != null) {
      logger.addAppender(appender);
    }
  }

  @SafeVarargs
  final void root(@Nullable Level level, Appender<ILoggingEvent>... appenders) {
    Logger logger = this.context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    if (level != null) {
      logger.setLevel(level);
    }
    for (Appender<ILoggingEvent> appender : appenders) {
      logger.addAppender(appender);
    }
  }

  void start(LifeCycle lifeCycle) {
    if (lifeCycle instanceof ContextAware contextAware) {
      contextAware.setContext(this.context);
    }
    lifeCycle.start();
  }

}
