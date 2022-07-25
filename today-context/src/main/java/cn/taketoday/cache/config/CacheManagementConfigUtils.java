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

package cn.taketoday.cache.config;

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
public abstract class CacheManagementConfigUtils {

  /**
   * The name of the cache advisor bean.
   */
  public static final String CACHE_ADVISOR_BEAN_NAME =
          "cn.taketoday.cache.config.internalCacheAdvisor";

  /**
   * The name of the cache aspect bean.
   */
  public static final String CACHE_ASPECT_BEAN_NAME =
          "cn.taketoday.cache.config.internalCacheAspect";

  /**
   * The name of the JCache advisor bean.
   */
  public static final String JCACHE_ADVISOR_BEAN_NAME =
          "cn.taketoday.cache.config.internalJCacheAdvisor";

  /**
   * The name of the JCache advisor bean.
   */
  public static final String JCACHE_ASPECT_BEAN_NAME =
          "cn.taketoday.cache.config.internalJCacheAspect";

}
