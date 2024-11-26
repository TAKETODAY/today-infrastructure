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

package infra.core.ansi;

/**
 * {@link AnsiElement Ansi} styles.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public enum AnsiStyle implements AnsiElement {

  NORMAL("0"),

  BOLD("1"),

  FAINT("2"),

  ITALIC("3"),

  UNDERLINE("4");

  private final String code;

  AnsiStyle(String code) {
    this.code = code;
  }

  @Override
  public String toString() {
    return this.code;
  }

}
