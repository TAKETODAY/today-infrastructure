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

import org.junit.jupiter.api.Test;

import cn.taketoday.cache.support.AbstractValueAdaptingCache;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Stephane Nicoll
 */
public abstract class AbstractValueAdaptingCacheTests<T extends AbstractValueAdaptingCache>
        extends AbstractCacheTests<T> {

  protected final static String CACHE_NAME_NO_NULL = "testCacheNoNull";

  protected abstract T getCache(boolean allowNull);

  @Test
  public void testCachePutNullValueAllowNullFalse() {
    T cache = getCache(false);
    String key = createRandomKey();
    assertThatIllegalArgumentException().isThrownBy(() ->
                    cache.put(key, null))
            .withMessageContaining(CACHE_NAME_NO_NULL)
            .withMessageContaining("is configured to not allow null values but null was provided");
  }

}
