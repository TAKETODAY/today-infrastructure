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

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Logback {@link CompositeConverter} used help format optional values that should be
 * shown enclosed in square brackets.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class EnclosedInSquareBracketsConverter extends CompositeConverter<ILoggingEvent> {

  @Override
  protected String transform(ILoggingEvent event, String in) {
    in = StringUtils.isEmpty(in) ? resolveFromFirstOption(event) : in;
    return StringUtils.isEmpty(in) ? "" : "[%s] ".formatted(in);
  }

  @Nullable
  private String resolveFromFirstOption(ILoggingEvent event) {
    String name = getFirstOption();
    if (name == null) {
      return null;
    }
    String value = event.getLoggerContextVO().getPropertyMap().get(name);
    return (value != null) ? value : System.getProperty(name);
  }

}
