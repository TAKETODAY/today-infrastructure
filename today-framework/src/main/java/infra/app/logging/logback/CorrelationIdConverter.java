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

package infra.app.logging.logback;

import java.util.Map;

import ch.qos.logback.classic.pattern.MDCConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.DynamicConverter;
import infra.app.logging.CorrelationIdFormatter;
import infra.core.env.Environment;
import infra.lang.Nullable;

/**
 * Logback {@link DynamicConverter} to convert a {@link CorrelationIdFormatter} pattern
 * into formatted output using data from the {@link ILoggingEvent#getMDCPropertyMap() MDC}
 * and {@link Environment}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see MDCConverter
 * @since 4.0 2023/7/2 16:20
 */
public class CorrelationIdConverter extends DynamicConverter<ILoggingEvent> {

  @Nullable
  private CorrelationIdFormatter formatter;

  @Override
  public void start() {
    this.formatter = CorrelationIdFormatter.of(getOptionList());
    super.start();
  }

  @Override
  public void stop() {
    this.formatter = null;
    super.stop();
  }

  @Override
  public String convert(ILoggingEvent event) {
    if (this.formatter == null) {
      return "";
    }
    Map<String, String> mdc = event.getMDCPropertyMap();
    return this.formatter.format(mdc::get);
  }

}

