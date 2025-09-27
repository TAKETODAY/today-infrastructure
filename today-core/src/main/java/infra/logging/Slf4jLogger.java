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

package infra.logging;

import org.jspecify.annotations.Nullable;

import java.io.Serial;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 2019-11-03 13:55
 */
class Slf4jLogger extends Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  private final String name;

  private final transient org.slf4j.Logger target;

  Slf4jLogger(org.slf4j.Logger target) {
    super(target.isDebugEnabled());
    this.target = target;
    this.name = target.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return debugEnabled && target.isTraceEnabled();
  }

  @Override
  public boolean isInfoEnabled() {
    return target.isInfoEnabled();
  }

  @Override
  public boolean isWarnEnabled() {
    return target.isWarnEnabled();
  }

  @Override
  public boolean isErrorEnabled() {
    return target.isErrorEnabled();
  }

  @Override
  public String getName() {
    return target.getName();
  }

  @Override
  @SuppressWarnings("NullAway")
  protected void logInternal(Level level, String format, @Nullable Throwable t, @Nullable Object @Nullable [] args) {
    final String msg = MessageFormatter.format(format, args);
    switch (level) {
      case DEBUG -> target.debug(msg, t);
      case ERROR -> target.error(msg, t);
      case TRACE -> target.trace(msg, t);
      case WARN -> target.warn(msg, t);
      default -> target.info(msg, t);
    }
  }

  @Serial
  protected Object readResolve() {
    return Slf4jLoggerFactory.createLog(this.name);
  }
}
