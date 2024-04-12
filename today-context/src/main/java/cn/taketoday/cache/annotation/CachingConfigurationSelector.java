/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.cache.annotation;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.AdviceModeImportSelector;
import cn.taketoday.context.annotation.AutoProxyRegistrar;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.StringUtils;

/**
 * Selects which implementation of {@link AbstractCachingConfiguration} should
 * be used based on the value of {@link EnableCaching#mode} on the importing
 * {@code @Configuration} class.
 *
 * <p>Detects the presence of JSR-107 and enables JCache support accordingly.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see EnableCaching
 * @see ProxyCachingConfiguration
 * @since 4.0
 */
public class CachingConfigurationSelector extends AdviceModeImportSelector<EnableCaching> {

  private static final String PROXY_JCACHE_CONFIGURATION_CLASS =
          "cn.taketoday.cache.jcache.config.ProxyJCacheConfiguration";

  private static final String CACHE_ASPECT_CONFIGURATION_CLASS_NAME =
          "cn.taketoday.cache.aspectj.AspectJCachingConfiguration";

  private static final String JCACHE_ASPECT_CONFIGURATION_CLASS_NAME =
          "cn.taketoday.cache.aspectj.AspectJJCacheConfiguration";

  private static final boolean jsr107Present;

  private static final boolean jcacheImplPresent;

  static {
    ClassLoader classLoader = CachingConfigurationSelector.class.getClassLoader();
    jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
    jcacheImplPresent = ClassUtils.isPresent(PROXY_JCACHE_CONFIGURATION_CLASS, classLoader);
  }

  /**
   * Returns {@link ProxyCachingConfiguration} or {@code AspectJCachingConfiguration}
   * for {@code PROXY} and {@code ASPECTJ} values of {@link EnableCaching#mode()},
   * respectively. Potentially includes corresponding JCache configuration as well.
   */
  @Override
  public String[] selectImports(AdviceMode adviceMode) {
    return switch (adviceMode) {
      case PROXY -> getProxyImports();
      case ASPECTJ -> getAspectJImports();
    };
  }

  /**
   * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#PROXY}.
   * <p>Take care of adding the necessary JSR-107 import if it is available.
   */
  private String[] getProxyImports() {
    List<String> result = new ArrayList<>(3);
    result.add(AutoProxyRegistrar.class.getName());
    result.add(ProxyCachingConfiguration.class.getName());
    if (jsr107Present && jcacheImplPresent) {
      result.add(PROXY_JCACHE_CONFIGURATION_CLASS);
    }
    return StringUtils.toStringArray(result);
  }

  /**
   * Return the imports to use if the {@link AdviceMode} is set to {@link AdviceMode#ASPECTJ}.
   * <p>Take care of adding the necessary JSR-107 import if it is available.
   */
  private String[] getAspectJImports() {
    List<String> result = new ArrayList<>(2);
    result.add(CACHE_ASPECT_CONFIGURATION_CLASS_NAME);
    if (jsr107Present && jcacheImplPresent) {
      result.add(JCACHE_ASPECT_CONFIGURATION_CLASS_NAME);
    }
    return StringUtils.toStringArray(result);
  }

}
