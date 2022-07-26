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

package cn.taketoday.retry.support;

/**
 * A root object containing the method arguments to use in expression evaluation.
 * IMPORTANT; the arguments are not available (will contain nulls) until after the first
 * call to the retryable method; this is generally only an issue for the
 * {@code maxAttempts}, meaning the arguments cannot be used to indicate
 * {@code maxAttempts = 0}.
 *
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @author Gary Russell
 * @since 4.0
 */
public class Args {

  /**
   * An empty {@link Args} with 100 null arguments.
   */
  public static final Args NO_ARGS = new Args(new Object[100]);

  private final Object[] args;

  public Args(Object[] args) {
    this.args = args;
  }

  public Object[] getArgs() {
    return args;
  }

}