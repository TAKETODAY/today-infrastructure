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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * Base class for cache operations.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Marcin Kamionowski
 * @since 4.0
 */
public abstract class CacheOperation implements BasicOperation {

  private final String name;

  private final Set<String> cacheNames;

  private final String key;

  private final String keyGenerator;

  private final String cacheManager;

  private final String cacheResolver;

  private final String condition;

  private final String toString;

  private final boolean hasKeyString;
  private final boolean hasConditionString;

  /**
   * Create a new {@link CacheOperation} instance from the given builder.
   */
  protected CacheOperation(Builder b) {
    this.name = b.name;
    this.cacheNames = b.cacheNames;
    this.key = b.key;
    this.keyGenerator = b.keyGenerator;
    this.cacheManager = b.cacheManager;
    this.cacheResolver = b.cacheResolver;
    this.condition = b.condition;
    this.hasKeyString = StringUtils.hasText(b.key);
    this.hasConditionString = StringUtils.hasText(b.condition);
    this.toString = b.getOperationDescription().toString();
  }

  public String getName() {
    return this.name;
  }

  @Override
  public Set<String> getCacheNames() {
    return this.cacheNames;
  }

  public String getKey() {
    return this.key;
  }

  public String getKeyGenerator() {
    return this.keyGenerator;
  }

  public String getCacheManager() {
    return this.cacheManager;
  }

  public String getCacheResolver() {
    return this.cacheResolver;
  }

  public String getCondition() {
    return this.condition;
  }

  public boolean hasKeyString() {
    return hasKeyString;
  }

  public boolean hasConditionString() {
    return hasConditionString;
  }

  /**
   * This implementation compares the {@code toString()} results.
   *
   * @see #toString()
   */
  @Override
  public boolean equals(@Nullable Object other) {
    return (other instanceof CacheOperation && toString().equals(other.toString()));
  }

  /**
   * This implementation returns {@code toString()}'s hash code.
   *
   * @see #toString()
   */
  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  /**
   * Return an identifying description for this cache operation.
   * <p>Returned value is produced by calling {@link Builder#getOperationDescription()}
   * during object construction. This method is used in {@link #hashCode} and
   * {@link #equals}.
   *
   * @see Builder#getOperationDescription()
   */
  @Override
  public final String toString() {
    return this.toString;
  }

  /**
   * Base class for builders that can be used to create a {@link CacheOperation}.
   */
  public abstract static class Builder {

    private String name = "";

    private Set<String> cacheNames = Collections.emptySet();

    private String key = "";

    private String keyGenerator = "";

    private String cacheManager = "";

    private String cacheResolver = "";

    private String condition = "";

    public void setName(String name) {
      Assert.hasText(name, "Name must not be empty");
      this.name = name;
    }

    public void setCacheName(String cacheName) {
      Assert.hasText(cacheName, "Cache name must not be empty");
      this.cacheNames = Collections.singleton(cacheName);
    }

    public void setCacheNames(String... cacheNames) {
      this.cacheNames = new LinkedHashSet<>(cacheNames.length);
      for (String cacheName : cacheNames) {
        Assert.hasText(cacheName, "Cache name must be non-empty if specified");
        this.cacheNames.add(cacheName);
      }
    }

    public Set<String> getCacheNames() {
      return this.cacheNames;
    }

    public void setKey(String key) {
      Assert.notNull(key, "Key must not be null");
      this.key = key;
    }

    public String getKey() {
      return this.key;
    }

    public String getKeyGenerator() {
      return this.keyGenerator;
    }

    public String getCacheManager() {
      return this.cacheManager;
    }

    public String getCacheResolver() {
      return this.cacheResolver;
    }

    public void setKeyGenerator(String keyGenerator) {
      Assert.notNull(keyGenerator, "KeyGenerator name must not be null");
      this.keyGenerator = keyGenerator;
    }

    public void setCacheManager(String cacheManager) {
      Assert.notNull(cacheManager, "CacheManager name must not be null");
      this.cacheManager = cacheManager;
    }

    public void setCacheResolver(String cacheResolver) {
      Assert.notNull(cacheResolver, "CacheResolver name must not be null");
      this.cacheResolver = cacheResolver;
    }

    public void setCondition(String condition) {
      Assert.notNull(condition, "Condition must not be null");
      this.condition = condition;
    }

    /**
     * Return an identifying description for this caching operation.
     * <p>Available to subclasses, for inclusion in their {@code toString()} result.
     */
    protected StringBuilder getOperationDescription() {
      StringBuilder result = new StringBuilder(getClass().getSimpleName());
      result.append('[').append(this.name);
      result.append("] caches=").append(this.cacheNames);
      result.append(" | key='").append(this.key);
      result.append("' | keyGenerator='").append(this.keyGenerator);
      result.append("' | cacheManager='").append(this.cacheManager);
      result.append("' | cacheResolver='").append(this.cacheResolver);
      result.append("' | condition='").append(this.condition).append('\'');
      return result;
    }

    public abstract CacheOperation build();
  }

}
