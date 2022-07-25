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

package cn.taketoday.cache.jcache.testfixture.cache.beans;

/**
 * Basic service interface for caching tests.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public interface CacheableService<T> {

  T cache(Object arg1);

  T cacheNull(Object arg1);

  T cacheSync(Object arg1);

  T cacheSyncNull(Object arg1);

  void evict(Object arg1, Object arg2);

  void evictWithException(Object arg1);

  void evictEarly(Object arg1);

  void evictAll(Object arg1);

  void evictAllEarly(Object arg1);

  T conditional(int field);

  T conditionalSync(int field);

  T unless(int arg);

  T key(Object arg1, Object arg2);

  T varArgsKey(Object... args);

  T name(Object arg1);

  T nullValue(Object arg1);

  T update(Object arg1);

  T conditionalUpdate(Object arg2);

  Number nullInvocations();

  T rootVars(Object arg1);

  T customKeyGenerator(Object arg1);

  T unknownCustomKeyGenerator(Object arg1);

  T customCacheManager(Object arg1);

  T unknownCustomCacheManager(Object arg1);

  T throwChecked(Object arg1) throws Exception;

  T throwUnchecked(Object arg1);

  T throwCheckedSync(Object arg1) throws Exception;

  T throwUncheckedSync(Object arg1);

  T multiCache(Object arg1);

  T multiEvict(Object arg1);

  T multiCacheAndEvict(Object arg1);

  T multiConditionalCacheAndEvict(Object arg1);

  T multiUpdate(Object arg1);

  TestEntity putRefersToResult(TestEntity arg1);

}
