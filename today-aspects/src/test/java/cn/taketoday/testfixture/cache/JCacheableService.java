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

package cn.taketoday.testfixture.cache;

import java.io.IOException;

/**
 * @author Stephane Nicoll
 */
public interface JCacheableService<T> {

  T cache(String id);

  T cacheNull(String id);

  T cacheWithException(String id, boolean matchFilter);

  T cacheWithCheckedException(String id, boolean matchFilter) throws IOException;

  T cacheAlwaysInvoke(String id);

  T cacheWithPartialKey(String id, boolean notUsed);

  T cacheWithCustomCacheResolver(String id);

  T cacheWithCustomKeyGenerator(String id, String anotherId);

  void put(String id, Object value);

  void putWithException(String id, Object value, boolean matchFilter);

  void earlyPut(String id, Object value);

  void earlyPutWithException(String id, Object value, boolean matchFilter);

  void remove(String id);

  void removeWithException(String id, boolean matchFilter);

  void earlyRemove(String id);

  void earlyRemoveWithException(String id, boolean matchFilter);

  void removeAll();

  void removeAllWithException(boolean matchFilter);

  void earlyRemoveAll();

  void earlyRemoveAllWithException(boolean matchFilter);

  long exceptionInvocations();

}
