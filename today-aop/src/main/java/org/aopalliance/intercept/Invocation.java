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

package org.aopalliance.intercept;

/**
 * This interface represents an invocation in the program.
 *
 * <p>
 * An invocation is a joinpoint and can be intercepted by an interceptor.
 *
 * @author Rod Johnson
 */
public interface Invocation extends Joinpoint {

  /**
   * Get the arguments as an array object. It is possible to change element values
   * within this array to change the arguments.
   *
   * @return the argument of the invocation
   */
  Object[] getArguments();

}
