/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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
package cn.taketoday.cache;

import java.util.Arrays;
import java.util.Objects;

import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.context.utils.Assert;

/**
 * @author TODAY <br>
 * 2019-11-03 19:45
 */
public class DefaultCacheManager extends AbstractCacheManager implements CacheManager {

    public DefaultCacheManager() {}

    public DefaultCacheManager(String... cacheNames) {
        Objects.requireNonNull(cacheNames, "cacheNames s can't be null");
        setCacheNames(Arrays.asList(cacheNames));
    }

    /**
     * @since 3.0
     */
    public DefaultCacheManager(CacheConfig... config) {
        Assert.notNull(config, "cache config can't be null");
        setCacheConfig(Arrays.asList(config));
    }

    @Override
    protected Cache doCreate(final String name, final CacheConfig cacheConfig) {
        return new ConcurrentMapCache(name);
    }
}
