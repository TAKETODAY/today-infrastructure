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

package infra.gradle.testkit;

/**
 * The DSLs supported by Gradle and demonstrated in the documentation samples.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public enum Dsl {

  /**
   * Supported DSL variants.
   */
  GROOVY("Groovy", ".gradle");

  private final String name;

  private final String extension;

  Dsl(String name, String extension) {
    this.name = name;
    this.extension = extension;
  }

  public String getName() {
    return this.name;
  }

  String getExtension() {
    return this.extension;
  }

}
