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

import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.pattern.Converter;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.Status;

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
  <T extends Converter<?>> void conversionRule(String conversionWord, Class<T> converterClass,
          Supplier<T> converterSupplier) {
    info("Adding conversion rule of type '%s' for word '%s'".formatted(converterClass.getName(), conversionWord));
    super.conversionRule(conversionWord, converterClass, converterSupplier);
  }

  @Override
  public void appender(String name, Appender<?> appender) {
    info("Adding appender '%s' named '%s'".formatted(appender, name));
    super.appender(name, appender);
  }

  @Override
  public void logger(String name, @Nullable Level level,
          boolean additive, @Nullable Appender<ILoggingEvent> appender) {
    info("Configuring logger '%s' with level '%s'. Additive: %s".formatted(name, level, additive));
    if (appender != null) {
      info("Adding appender '%s' to logger '%s'".formatted(appender, name));
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
