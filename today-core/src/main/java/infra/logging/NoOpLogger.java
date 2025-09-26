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
 * @author TODAY 2021/11/5 22:53
 * @since 4.0
 */
public class NoOpLogger extends Logger {

  @Serial
  private static final long serialVersionUID = 1L;

  public NoOpLogger() {
    super(false);
  }

  @Override
  public String getName() {
    return "NoOpLogger";
  }

  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  @Override
  public boolean isInfoEnabled() {
    return false;
  }

  @Override
  public boolean isWarnEnabled() {
    return false;
  }

  @Override
  public boolean isErrorEnabled() {
    return false;
  }

  @Override
  protected void logInternal(Level level, Object msg, @Nullable Throwable t) {

  }

  @Override
  protected void logInternal(Level level, String msg, @Nullable Throwable t, @Nullable Object[] args) {

  }
}
