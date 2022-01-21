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

package cn.taketoday.framework;

/**
 * Internal test class providing access to {@link System#out System.out} and
 * {@link System#err System.err} output that has been captured.
 *
 * @author Madhura Bhave
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.2.0
 */
public interface CapturedOutput extends CharSequence {

  @Override
  default int length() {
    return toString().length();
  }

  @Override
  default char charAt(int index) {
    return toString().charAt(index);
  }

  @Override
  default CharSequence subSequence(int start, int end) {
    return toString().subSequence(start, end);
  }

  /**
   * Return all content (both {@link System#out System.out} and {@link System#err
   * System.err}) in the order that it was captured.
   *
   * @return all captured output
   */
  String getAll();

  /**
   * Return {@link System#out System.out} content in the order that it was captured.
   *
   * @return {@link System#out System.out} captured output
   */
  String getOut();

  /**
   * Return {@link System#err System.err} content in the order that it was captured.
   *
   * @return {@link System#err System.err} captured output
   */
  String getErr();

}
