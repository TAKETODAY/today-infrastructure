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

package cn.taketoday.app.loader.jarmode;

/**
 * Interface registered in {@code today.strategies} to provides extended 'jarmode'
 * support.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface JarMode {

  /**
   * Returns if this accepts and can run the given mode.
   *
   * @param mode the mode to check
   * @return if this instance accepts the mode
   */
  boolean accepts(String mode);

  /**
   * Run the jar in the given mode.
   *
   * @param mode the mode to use
   * @param args any program arguments
   */
  void run(String mode, String[] args);

}
