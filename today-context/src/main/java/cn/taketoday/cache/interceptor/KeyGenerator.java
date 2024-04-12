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

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;

/**
 * Cache key generator. Used for creating a key based on the given method
 * (used as context) and its parameters.
 *
 * @author Costin Leau
 * @author Chris Beams
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
public interface KeyGenerator {

  /**
   * Generate a key for the given method and its parameters.
   *
   * @param target the target instance
   * @param method the method being called
   * @param params the method parameters (with any var-args expanded)
   * @return a generated key
   */
  Object generate(Object target, Method method, Object... params);

}
