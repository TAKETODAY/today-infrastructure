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

package cn.taketoday.cache.interceptor;

import java.lang.reflect.Method;

/**
 * Representation of the context of the invocation of a cache operation.
 *
 * <p>The cache operation is static and independent of a particular invocation;
 * this interface gathers the operation and a particular invocation.
 *
 * @param <O> the operation type
 * @author Stephane Nicoll
 * @since 4.0
 */
public interface CacheOperationInvocationContext<O extends BasicOperation> {

  /**
   * Return the cache operation.
   */
  O getOperation();

  /**
   * Return the target instance on which the method was invoked.
   */
  Object getTarget();

  /**
   * Return the method which was invoked.
   */
  Method getMethod();

  /**
   * Return the argument list used to invoke the method.
   */
  Object[] getArgs();

}
