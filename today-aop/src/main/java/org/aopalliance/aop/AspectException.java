/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package org.aopalliance.aop;

import java.io.Serial;

/**
 * Superclass for all AOP infrastructure exceptions. Unchecked, as such
 * exceptions are fatal and end user code shouldn't be forced to catch them.
 *
 * @author Rod Johnson
 * @author Bob Lee
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class AspectException extends RuntimeException {

  @Serial
  private static final long serialVersionUID = 371663334385751868L;

  /**
   * Constructor for AspectException.
   */
  public AspectException(String s) {
    super(s);
  }

  /**
   * Constructor for AspectException.
   */
  public AspectException(String s, Throwable t) {
    super(s, t);
  }

}
