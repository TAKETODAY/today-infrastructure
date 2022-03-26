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

package cn.taketoday.retry.policy;

import cn.taketoday.retry.RetryContext;

/**
 * Simple map-like abstraction for stateful retry policies to use when storing and
 * retrieving {@link RetryContext} instances. A null key should never be passed in by the
 * caller, but if it is then implementations are free to discard the context instead of
 * saving it (null key means "no information").
 *
 * @author Dave Syer
 * @see MapRetryContextCache
 */
public interface RetryContextCache {

  RetryContext get(Object key);

  void put(Object key, RetryContext context) throws RetryCacheCapacityExceededException;

  void remove(Object key);

  boolean containsKey(Object key);

}
