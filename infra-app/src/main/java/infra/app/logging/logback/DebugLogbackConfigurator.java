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
