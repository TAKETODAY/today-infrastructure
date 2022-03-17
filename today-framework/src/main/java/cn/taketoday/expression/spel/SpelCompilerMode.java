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

package cn.taketoday.expression.spel;

/**
 * Captures the possible configuration settings for a compiler that can be
 * used when evaluating expressions.
 *
 * @author Andy Clement
 * @since 4.0
 */
public enum SpelCompilerMode {

  /**
   * The compiler is switched off; this is the default.
   */
  OFF,

  /**
   * In immediate mode, expressions are compiled as soon as possible (usually after 1 interpreted run).
   * If a compiled expression fails it will throw an exception to the caller.
   */
  IMMEDIATE,

  /**
   * In mixed mode, expression evaluation silently switches between interpreted and compiled over time.
   * After a number of runs the expression gets compiled. If it later fails (possibly due to inferred
   * type information changing) then that will be caught internally and the system switches back to
   * interpreted mode. It may subsequently compile it again later.
   */
  MIXED

}
