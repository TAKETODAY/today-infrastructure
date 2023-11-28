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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;
import cn.taketoday.lang.Nullable;

/**
 * Custom {@link LogbackConfigurator} used to add {@link Status Statuses} when Logback
 * debugging is enabled.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class DebugLogbackConfigurator extends LogbackConfigurator {

  DebugLogbackConfigurator(LoggerContext context) {
    super(context);
  }

  @Override
  @SuppressWarnings("rawtypes")
  public void conversionRule(String conversionWord, Class<? extends Converter> converterClass) {
    info("Adding conversion rule of type '" + converterClass.getName() + "' for word '" + conversionWord + "'");
    super.conversionRule(conversionWord, converterClass);
  }

  @Override
  public void appender(String name, Appender<?> appender) {
    info("Adding appender '" + appender + "' named '" + name + "'");
    super.appender(name, appender);
  }

  @Override
  public void logger(String name, @Nullable Level level,
          boolean additive, @Nullable Appender<ILoggingEvent> appender) {
    info("Configuring logger '" + name + "' with level '" + level + "'. Additive: " + additive);
    if (appender != null) {
      info("Adding appender '" + appender + "' to logger '" + name + "'");
    }
    super.logger(name, level, additive, appender);
  }

  @Override
  public void start(LifeCycle lifeCycle) {
    info("Starting '" + lifeCycle + "'");
    super.start(lifeCycle);
  }

  private void info(String message) {
    getContext().getStatusManager().add(new InfoStatus(message, this));
  }

}
