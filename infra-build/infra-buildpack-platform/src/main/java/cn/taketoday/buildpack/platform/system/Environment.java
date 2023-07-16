/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.buildpack.platform.system;

/**
 * Provides access to environment variable values.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @since 4.0
 */
@FunctionalInterface
public interface Environment {

  /**
   * Standard {@link Environment} implementation backed by
   * {@link System#getenv(String)}.
   */
  Environment SYSTEM = System::getenv;

  /**
   * Gets the value of the specified environment variable.
   *
   * @param name the name of the environment variable
   * @return the string value of the variable, or {@code null} if the variable is not
   * defined in the environment
   */
  String get(String name);

}
