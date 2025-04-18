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

package infra.test.annotation;

import infra.lang.Nullable;

/**
 * <p>
 * Strategy interface for retrieving <em>profile values</em> for a given
 * testing environment.
 * </p>
 * <p>
 * Concrete implementations must provide a {@code public} no-args
 * constructor.
 * </p>
 * <p>
 * Infra provides the following out-of-the-box implementations:
 * </p>
 * <ul>
 * <li>{@link SystemProfileValueSource}</li>
 * </ul>
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @see ProfileValueSourceConfiguration
 * @see IfProfileValue
 * @see ProfileValueUtils
 * @since 4.0
 */
public interface ProfileValueSource {

  /**
   * Get the <em>profile value</em> indicated by the specified key.
   *
   * @param key the name of the <em>profile value</em>
   * @return the String value of the <em>profile value</em>, or {@code null}
   * if there is no <em>profile value</em> with that key
   */
  @Nullable
  String get(String key);

}
