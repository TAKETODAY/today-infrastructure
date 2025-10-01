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

package infra.core.ansi;

/**
 * {@link AnsiElement Ansi} background colors.
 *
 * @author Phillip Webb
 * @author Geoffrey Chandler
 * @since 4.0
 */
public enum AnsiBackground implements AnsiElement {

  DEFAULT("49"),

  BLACK("40"),

  RED("41"),

  GREEN("42"),

  YELLOW("43"),

  BLUE("44"),

  MAGENTA("45"),

  CYAN("46"),

  WHITE("47"),

  BRIGHT_BLACK("100"),

  BRIGHT_RED("101"),

  BRIGHT_GREEN("102"),

  BRIGHT_YELLOW("103"),

  BRIGHT_BLUE("104"),

  BRIGHT_MAGENTA("105"),

  BRIGHT_CYAN("106"),

  BRIGHT_WHITE("107");

  private final String code;

  AnsiBackground(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return this.code;
  }

}
