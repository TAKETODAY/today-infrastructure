/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
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
package cn.taketoday.cache;

import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import cn.taketoday.cache.annotation.CacheConfig;
import cn.taketoday.lang.Autowired;

/**
 * A {@link CacheManager} implementation backed by Redisson instance.
 *
 * @author TODAY <br>
 * 2018-12-24 19:06
 */
public class RedissonCacheManager extends AbstractCacheManager implements CacheManager {

  private Codec codec;
  private final RedissonClient redisson;

  public RedissonCacheManager(RedissonClient redisson) {
    this(null, redisson);
  }

  @Autowired
  public RedissonCacheManager(
          @Autowired(required = false) Codec codec, @Autowired RedissonClient redisson) {
    this.codec = codec;
    this.redisson = redisson;
  }

  @Override
  protected Cache doCreate(final String name, final CacheConfig cacheConfig) {
    return isDefaultConfig(cacheConfig) ? createMap(name) : createMapCache(name, cacheConfig);
  }

  /**
   * Set Codec instance shared between all Cache instances
   *
   * @param codec
   *         object
   */
  public void setCodec(Codec codec) {
    this.codec = codec;
  }

  protected Cache createMap(String name) {
    return new RedissonCache(getMap(name), name);
  }

  protected RMap<Object, Object> getMap(String name) {
    return codec != null ? redisson.getMap(name, codec) : redisson.getMap(name);
  }

  /**
   * Not a default config
   *
   * @param name
   *         the name of cache
   * @param config
   *         config instance
   */
  private Cache createMapCache(String name, CacheConfig config) {
    RMapCache<Object, Object> map = getMapCache(name);
    Cache cache = new RedissonCache(map, name, config);
    map.setMaxSize(config.maxSize());
    return cache;
  }

  protected RMapCache<Object, Object> getMapCache(String name) {
    return codec != null ? redisson.getMapCache(name, codec) : redisson.getMapCache(name);
  }

}
