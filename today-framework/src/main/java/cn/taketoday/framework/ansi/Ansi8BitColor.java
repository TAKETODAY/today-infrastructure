/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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

package cn.taketoday.framework.ansi;

import cn.taketoday.lang.Assert;

/**
 * {@link AnsiElement} implementation for ANSI 8-bit foreground or background color codes.
 *
 * @author Toshiaki Maki
 * @author Phillip Webb
 * @see #foreground(int)
 * @see #background(int)
 * @since 4.0
 */
public final class Ansi8BitColor implements AnsiElement {

  private final String prefix;

  private final int code;

  /**
   * Create a new {@link Ansi8BitColor} instance.
   *
   * @param prefix the prefix escape chars
   * @param code color code (must be 0-255)
   * @throws IllegalArgumentException if color code is not between 0 and 255.
   */
  private Ansi8BitColor(String prefix, int code) {
    Assert.isTrue(code >= 0 && code <= 255, "Code must be between 0 and 255");
    this.prefix = prefix;
    this.code = code;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Ansi8BitColor other = (Ansi8BitColor) obj;
    return this.prefix.equals(other.prefix) && this.code == other.code;
  }

  @Override
  public int hashCode() {
    return this.prefix.hashCode() * 31 + this.code;
  }

  @Override
  public String toString() {
    return this.prefix + this.code;
  }

  /**
   * Return a foreground ANSI color code instance for the given code.
   *
   * @param code the color code
   * @return an ANSI color code instance
   */
  public static Ansi8BitColor foreground(int code) {
    return new Ansi8BitColor("38;5;", code);
  }

  /**
   * Return a background ANSI color code instance for the given code.
   *
   * @param code the color code
   * @return an ANSI color code instance
   */
  public static Ansi8BitColor background(int code) {
    return new Ansi8BitColor("48;5;", code);
  }

}
