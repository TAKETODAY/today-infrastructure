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
package cn.taketoday.cache;

import org.redisson.api.RMap;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.context.ResourceLoaderAware;
import cn.taketoday.core.io.Resource;
import cn.taketoday.core.io.ResourceLoader;

/**
 * A {@link CacheManager} implementation
 * backed by Redisson instance.
 *
 * @author Nikita Koksharov
 * @author TODAY <br>
 * 2018-12-24 19:06
 */
@SuppressWarnings("unchecked")
public class RedissonCacheManager implements CacheManager, ResourceLoaderAware, InitializingBean {

  ResourceLoader resourceLoader;

  private boolean dynamic = true;

  private boolean allowNullValues = true;

  Codec codec;

  RedissonClient redisson;

  Map<String, CacheConfig> configMap = new ConcurrentHashMap<>();
  ConcurrentMap<String, Cache> instanceMap = new ConcurrentHashMap<>();

  String configLocation;

  /**
   * Creates CacheManager supplied by Redisson instance
   *
   * @param redisson object
   */
  public RedissonCacheManager(RedissonClient redisson) {
    this(redisson, (String) null, null);
  }

  /**
   * Creates CacheManager supplied by Redisson instance and
   * Cache config mapped by Cache name
   *
   * @param redisson object
   * @param config object
   */
  public RedissonCacheManager(RedissonClient redisson, Map<String, ? extends CacheConfig> config) {
    this(redisson, config, null);
  }

  /**
   * Creates CacheManager supplied by Redisson instance, Codec instance
   * and Cache config mapped by Cache name.
   * <p>
   * Each Cache instance share one Codec instance.
   *
   * @param redisson object
   * @param config object
   * @param codec object
   */
  public RedissonCacheManager(RedissonClient redisson, Map<String, ? extends CacheConfig> config, Codec codec) {
    this.redisson = redisson;
    this.configMap = (Map<String, CacheConfig>) config;
    this.codec = codec;
  }

  /**
   * Creates CacheManager supplied by Redisson instance
   * and Cache config mapped by Cache name.
   * <p>
   * Loads the config file from the class path, interpreting plain paths as class path resource names
   * that include the package path (e.g. "mypackage/myresource.txt").
   *
   * @param redisson object
   * @param configLocation path
   */
  public RedissonCacheManager(RedissonClient redisson, String configLocation) {
    this(redisson, configLocation, null);
  }

  /**
   * Creates CacheManager supplied by Redisson instance, Codec instance
   * and Config location path.
   * <p>
   * Each Cache instance share one Codec instance.
   * <p>
   * Loads the config file from the class path, interpreting plain paths as class path resource names
   * that include the package path (e.g. "mypackage/myresource.txt").
   *
   * @param redisson object
   * @param configLocation path
   * @param codec object
   */
  public RedissonCacheManager(RedissonClient redisson, String configLocation, Codec codec) {
    this.redisson = redisson;
    this.configLocation = configLocation;
    this.codec = codec;
  }

  /**
   * Defines possibility of storing {@code null} values.
   * <p>
   * Default is <code>true</code>
   *
   * @param allowNullValues - stores if <code>true</code>
   */
  public void setAllowNullValues(boolean allowNullValues) {
    this.allowNullValues = allowNullValues;
  }

  /**
   * Defines 'fixed' cache names.
   * A new cache instance will not be created in dynamic for non-defined names.
   * <p>
   * `null` parameter setups dynamic mode
   *
   * @param names of caches
   */
  public void setCacheNames(Collection<String> names) {
    if (names != null) {
      for (String name : names) {
        getCache(name);
      }
      dynamic = false;
    }
    else {
      dynamic = true;
    }
  }

  /**
   * Set cache config location
   *
   * @param configLocation object
   */
  public void setConfigLocation(String configLocation) {
    this.configLocation = configLocation;
  }

  /**
   * Set cache config mapped by cache name
   *
   * @param config object
   */
  public void setConfig(Map<String, ? extends CacheConfig> config) {
    this.configMap = (Map<String, CacheConfig>) config;
  }

  /**
   * Set Redisson instance
   *
   * @param redisson instance
   */
  public void setRedisson(RedissonClient redisson) {
    this.redisson = redisson;
  }

  /**
   * Set Codec instance shared between all Cache instances
   *
   * @param codec object
   */
  public void setCodec(Codec codec) {
    this.codec = codec;
  }

  protected CacheConfig createDefaultConfig() {
    return new CacheConfig();
  }

  @Override
  public Cache getCache(String name) {
    Cache cache = instanceMap.get(name);
    if (cache != null) {
      return cache;
    }
    if (!dynamic) {
      return cache;
    }

    CacheConfig config = configMap.get(name);
    if (config == null) {
      config = createDefaultConfig();
      configMap.put(name, config);
    }

    if (config.getMaxIdleTime() == 0 && config.getTTL() == 0 && config.getMaxSize() == 0) {
      return createMap(name, config);
    }

    return createMapCache(name, config);
  }

  private Cache createMap(String name, CacheConfig config) {
    RMap<Object, Object> map = getMap(name, config);

    Cache cache = new RedissonCache(map, allowNullValues);
    Cache oldCache = instanceMap.putIfAbsent(name, cache);
    if (oldCache != null) {
      cache = oldCache;
    }
    return cache;
  }

  protected RMap<Object, Object> getMap(String name, CacheConfig config) {
    if (codec != null) {
      return redisson.getMap(name, codec);
    }
    return redisson.getMap(name);
  }

  private Cache createMapCache(String name, CacheConfig config) {
    RMapCache<Object, Object> map = getMapCache(name, config);

    Cache cache = new RedissonCache(map, config, allowNullValues);
    Cache oldCache = instanceMap.putIfAbsent(name, cache);
    if (oldCache != null) {
      cache = oldCache;
    }
    else {
      map.setMaxSize(config.getMaxSize());
    }
    return cache;
  }

  protected RMapCache<Object, Object> getMapCache(String name, CacheConfig config) {
    if (codec != null) {
      return redisson.getMapCache(name, codec);
    }
    return redisson.getMapCache(name);
  }

  @Override
  public Collection<String> getCacheNames() {
    return Collections.unmodifiableSet(configMap.keySet());
  }

  @Override
  public void setResourceLoader(ResourceLoader resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    if (configLocation == null) {
      return;
    }

    Resource resource = resourceLoader.getResource(configLocation);
    try {
      this.configMap = (Map<String, CacheConfig>) CacheConfig.fromJSON(resource.getInputStream());
    }
    catch (IOException e) {
      // try to read yaml
      try {
        this.configMap = (Map<String, CacheConfig>) CacheConfig.fromYAML(resource.getInputStream());
      }
      catch (IOException e1) {
        throw new BeanDefinitionStoreException(
                "Could not parse cache configuration at [" + configLocation + "]", e1);
      }
    }
  }

}
