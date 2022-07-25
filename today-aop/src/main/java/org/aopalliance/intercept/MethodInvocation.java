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

import java.lang.reflect.Method;

/**
 * Description of an invocation to a method, given to an interceptor upon
 * method-call.
 *
 * <p>
 * A method invocation is a joinpoint and can be intercepted by a method
 * interceptor.
 *
 * @see MethodInterceptor
 */
public interface MethodInvocation extends Invocation {

  /**
   * Gets the method being called.
   *
   * <p>
   * This method is a friendly implementation of the
   * {@link Joinpoint#getStaticPart()} method (same result).
   *
   * @return the method being called.
   */
  Method getMethod();

}
