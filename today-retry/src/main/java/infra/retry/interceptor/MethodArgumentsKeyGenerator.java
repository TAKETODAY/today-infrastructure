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
package infra.retry.interceptor;

import infra.lang.Nullable;

/**
 * Interface that allows method parameters to be identified and tagged by a unique key.
 *
 * @author Dave Syer
 * @since 4.0
 */
public interface MethodArgumentsKeyGenerator {

  /**
   * Get a unique identifier for the item that can be used to cache it between calls if
   * necessary, and then identify it later.
   *
   * @param item the current method arguments (may be null if there are none).
   * @return a unique identifier.
   */
  @Nullable
  Object getKey(Object[] item);

}
