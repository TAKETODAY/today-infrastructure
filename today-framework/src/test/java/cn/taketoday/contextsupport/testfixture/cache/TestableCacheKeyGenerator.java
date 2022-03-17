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

package cn.taketoday.contextsupport.testfixture.cache;

import java.lang.annotation.Annotation;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.GeneratedCacheKey;

import cn.taketoday.cache.interceptor.SimpleKey;

/**
 * A simple test key generator that only takes the first key arguments into
 * account. To be used with a multi parameters key to validate it has been
 * used properly.
 *
 * @author Stephane Nicoll
 */
public class TestableCacheKeyGenerator implements CacheKeyGenerator {

  @Override
  public GeneratedCacheKey generateCacheKey(CacheKeyInvocationContext<? extends Annotation> context) {
    return new SimpleGeneratedCacheKey(context.getKeyParameters()[0]);
  }

  @SuppressWarnings("serial")
  private static class SimpleGeneratedCacheKey extends SimpleKey implements GeneratedCacheKey {

    public SimpleGeneratedCacheKey(Object... elements) {
      super(elements);
    }

  }

}
