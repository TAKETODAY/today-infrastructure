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

package cn.taketoday.app.loader.tools;

/**
 * The scope of a library. The common {@link #COMPILE}, {@link #RUNTIME} and
 * {@link #PROVIDED} scopes are defined here and supported by the common {@link Layouts}.
 * A custom {@link Layout} can handle additional scopes as required.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface LibraryScope {

  @Override
  String toString();

  /**
   * The library is used at compile time and runtime.
   */
  LibraryScope COMPILE = new LibraryScope() {

    @Override
    public String toString() {
      return "compile";
    }

  };

  /**
   * The library is used at runtime but not needed for compile.
   */
  LibraryScope RUNTIME = new LibraryScope() {

    @Override
    public String toString() {
      return "runtime";
    }

  };

  /**
   * The library is needed for compile but is usually provided when running.
   */
  LibraryScope PROVIDED = new LibraryScope() {

    @Override
    public String toString() {
      return "provided";
    }

  };

  /**
   * Marker for custom scope when custom configuration is used.
   */
  LibraryScope CUSTOM = new LibraryScope() {

    @Override
    public String toString() {
      return "custom";
    }

  };

}
