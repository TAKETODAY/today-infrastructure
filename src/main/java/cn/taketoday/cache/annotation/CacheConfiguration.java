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
package cn.taketoday.cache.annotation;

import java.lang.annotation.Annotation;
import java.util.concurrent.TimeUnit;

import cn.taketoday.context.Constant;
import cn.taketoday.context.utils.StringUtils;

/**
 * @author TODAY <br>
 *         2019-03-01 13:17
 */
@SuppressWarnings("all")
public final class CacheConfiguration implements Cacheable, CacheEvict, CachePut, CacheConfig, Annotation {

    private boolean sync = false;
    private boolean allEntries = false;
    private String key = Constant.BLANK;
    /** cache unless expression */
    private String unless = Constant.BLANK;
    private boolean beforeInvocation = false;
    /** cache name */
    private String cacheName = Constant.BLANK;
    private String condition = Constant.BLANK;

    private int maxSize = 0;
    private long expire = 0;
    private long maxIdleTime = 0;
    private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

    private final Class<? extends Annotation> annotationType;

    public CacheConfiguration(Class<? extends Annotation> annotationType) {
        this.annotationType = annotationType;
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        return annotationType;
    }

    @Override
    public String cacheName() {
        return cacheName;
    }

    @Override
    public long expire() {
        return expire;
    }

    @Override
    public TimeUnit timeUnit() {
        return timeUnit;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String condition() {
        return condition;
    }

    @Override
    public String unless() {
        return unless;
    }

    @Override
    public boolean sync() {
        return sync;
    }

    @Override
    public boolean allEntries() {
        return allEntries;
    }

    @Override
    public boolean beforeInvocation() {
        return beforeInvocation;
    }

    /**
     * Merge {@link CacheConfig} {@link Annotation}
     * 
     * @param annotationAttributes
     */
    public void mergeCacheConfigAttributes(CacheConfig cacheConfig) {

        if (StringUtils.isEmpty(this.cacheName)) {
            this.cacheName = cacheConfig.cacheName();
        }
        if (this.expire == 0) {
            this.expire = cacheConfig.expire();
        }
        if (this.maxSize == 0) {
            this.maxSize = cacheConfig.maxSize();
        }
        if (this.maxIdleTime == 0) {
            this.maxIdleTime = cacheConfig.maxIdleTime();
        }
        if (this.timeUnit == TimeUnit.MILLISECONDS) {
            TimeUnit timeUnit = cacheConfig.timeUnit();
            if (timeUnit != TimeUnit.MILLISECONDS) {
                this.timeUnit = timeUnit;
            }
        }
    }

    @Override
    public int maxSize() {
        return maxSize;
    }

    @Override
    public long maxIdleTime() {
        return maxIdleTime;
    }

}
