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

package infra.util;

import org.jspecify.annotations.Nullable;

/**
 * An {@link ErrorHandler} implementation that logs the Throwable at error
 * level and then propagates it.
 *
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2024/12/2 16:44
 */
final class PropagatingErrorHandler extends LoggingErrorHandler {

  public PropagatingErrorHandler(@Nullable String message, @Nullable String loggerName) {
    super(message, loggerName);
  }

  @Override
  public void handleError(Throwable t) {
    super.handleError(t);
    ReflectionUtils.rethrowRuntimeException(t);
  }

}
