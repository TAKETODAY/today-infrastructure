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

package cn.taketoday.app.loader.jar;

/**
 * Interface that can be used to filter and optionally rename jar entries.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
interface JarEntryFilter {

  /**
   * Apply the jar entry filter.
   *
   * @param name the current entry name. This may be different that the original entry
   * name if a previous filter has been applied
   * @return the new name of the entry or {@code null} if the entry should not be
   * included.
   */
  AsciiBytes apply(AsciiBytes name);

}
