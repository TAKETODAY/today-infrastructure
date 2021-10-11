/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import cn.taketoday.cache.Cache;

/**
 * Default implementation of {@link CacheExceptionResolver}
 *
 * @author TODAY <br>
 * 2019-02-27 17:13
 */
public class DefaultCacheExceptionResolver implements CacheExceptionResolver {

  @Override
  public void resolveClearException(RuntimeException exception, Cache cache) {
    throw exception;
  }

  @Override
  public Object resolveGetException(RuntimeException exception, Cache cache, Object key) {
    return null;
  }

  @Override
  public void resolveEvictException(RuntimeException exception, Cache cache, Object key) {
    throw exception;
  }

  @Override
  public void resolvePutException(RuntimeException exception, Cache cache, Object key, Object value) {
    throw exception;
  }

}
