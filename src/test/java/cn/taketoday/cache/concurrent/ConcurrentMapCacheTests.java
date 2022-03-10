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

package cn.taketoday.cache.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.contextsupport.testfixture.cache.AbstractValueAdaptingCacheTests;
import cn.taketoday.core.serializer.support.SerializationDelegate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class ConcurrentMapCacheTests extends AbstractValueAdaptingCacheTests<ConcurrentMapCache> {

  protected ConcurrentMap<Object, Object> nativeCache;

  protected ConcurrentMapCache cache;

  protected ConcurrentMap<Object, Object> nativeCacheNoNull;

  protected ConcurrentMapCache cacheNoNull;

  @BeforeEach
  public void setup() {
    this.nativeCache = new ConcurrentHashMap<>();
    this.cache = new ConcurrentMapCache(CACHE_NAME, this.nativeCache, true);
    this.nativeCacheNoNull = new ConcurrentHashMap<>();
    this.cacheNoNull = new ConcurrentMapCache(CACHE_NAME_NO_NULL, this.nativeCacheNoNull, false);
    this.cache.clear();
  }

  @Override
  protected ConcurrentMapCache getCache() {
    return getCache(true);
  }

  @Override
  protected ConcurrentMapCache getCache(boolean allowNull) {
    return allowNull ? this.cache : this.cacheNoNull;
  }

  @Override
  protected ConcurrentMap<Object, Object> getNativeCache() {
    return this.nativeCache;
  }

  @Test
  public void testIsStoreByReferenceByDefault() {
    assertThat(this.cache.isStoreByValue()).isFalse();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSerializer() {
    ConcurrentMapCache serializeCache = createCacheWithStoreByValue();
    assertThat(serializeCache.isStoreByValue()).isTrue();

    Object key = createRandomKey();
    List<String> content = new ArrayList<>(Arrays.asList("one", "two", "three"));
    serializeCache.put(key, content);
    content.remove(0);
    List<String> entry = (List<String>) serializeCache.get(key).get();
    assertThat(entry.size()).isEqualTo(3);
    assertThat(entry.get(0)).isEqualTo("one");
  }

  @Test
  public void testNonSerializableContent() {
    ConcurrentMapCache serializeCache = createCacheWithStoreByValue();

    assertThatIllegalArgumentException().isThrownBy(() ->
                    serializeCache.put(createRandomKey(), this.cache))
            .withMessageContaining("Failed to serialize")
            .withMessageContaining(this.cache.getClass().getName());

  }

  @Test
  public void testInvalidSerializedContent() {
    ConcurrentMapCache serializeCache = createCacheWithStoreByValue();

    String key = createRandomKey();
    this.nativeCache.put(key, "Some garbage");
    assertThatIllegalArgumentException().isThrownBy(() ->
                    serializeCache.get(key))
            .withMessageContaining("Failed to deserialize")
            .withMessageContaining("Some garbage");
  }

  private ConcurrentMapCache createCacheWithStoreByValue() {
    return new ConcurrentMapCache(CACHE_NAME, this.nativeCache, true,
            new SerializationDelegate(ConcurrentMapCacheTests.class.getClassLoader()));
  }

}
