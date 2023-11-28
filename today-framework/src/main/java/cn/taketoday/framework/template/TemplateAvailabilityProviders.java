/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;

/**
 * Collection of {@link TemplateAvailabilityProvider} beans that can be used to check
 * which (if any) templating engine supports a given view. Caches responses unless the
 * {@code infra.template.provider.cache} property is set to {@code false}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class TemplateAvailabilityProviders {

  private final List<TemplateAvailabilityProvider> providers;

  private static final int CACHE_LIMIT = 1024;

  private static final TemplateAvailabilityProvider NONE = new NoTemplateAvailabilityProvider();

  /**
   * Resolved template views, returning already cached instances without a global lock.
   */
  private final Map<String, TemplateAvailabilityProvider> resolved = new ConcurrentHashMap<>(CACHE_LIMIT);

  /**
   * Map from view name resolve template view, synchronized when accessed.
   */
  private final Map<String, TemplateAvailabilityProvider> cache = new LinkedHashMap<>(CACHE_LIMIT, 0.75f, true) {

    @Override
    protected boolean removeEldestEntry(Map.Entry<String, TemplateAvailabilityProvider> eldest) {
      if (size() > CACHE_LIMIT) {
        TemplateAvailabilityProviders.this.resolved.remove(eldest.getKey());
        return true;
      }
      return false;
    }

  };

  /**
   * Create a new {@link TemplateAvailabilityProviders} instance.
   *
   * @param applicationContext the source application context
   */
  public TemplateAvailabilityProviders(@Nullable ApplicationContext applicationContext) {
    this((applicationContext != null) ? applicationContext.getClassLoader() : null);
  }

  /**
   * Create a new {@link TemplateAvailabilityProviders} instance.
   *
   * @param classLoader the source class loader
   */
  public TemplateAvailabilityProviders(@Nullable ClassLoader classLoader) {
    this.providers = TodayStrategies.find(TemplateAvailabilityProvider.class, classLoader);
  }

  /**
   * Create a new {@link TemplateAvailabilityProviders} instance.
   *
   * @param providers the underlying providers
   */
  protected TemplateAvailabilityProviders(Collection<? extends TemplateAvailabilityProvider> providers) {
    Assert.notNull(providers, "Providers is required");
    this.providers = new ArrayList<>(providers);
  }

  /**
   * Return the underlying providers being used.
   *
   * @return the providers being used
   */
  public List<TemplateAvailabilityProvider> getProviders() {
    return this.providers;
  }

  /**
   * Get the provider that can be used to render the given view.
   *
   * @param view the view to render
   * @param applicationContext the application context
   * @return a {@link TemplateAvailabilityProvider} or null
   */
  @Nullable
  public TemplateAvailabilityProvider getProvider(String view, ApplicationContext applicationContext) {
    Assert.notNull(applicationContext, "ApplicationContext is required");
    return getProvider(view, applicationContext.getEnvironment(),
            applicationContext.getClassLoader(), applicationContext);
  }

  /**
   * Get the provider that can be used to render the given view.
   *
   * @param view the view to render
   * @param environment the environment
   * @param classLoader the class loader
   * @param resourceLoader the resource loader
   * @return a {@link TemplateAvailabilityProvider} or null
   */
  @Nullable
  public TemplateAvailabilityProvider getProvider(String view,
          Environment environment, ClassLoader classLoader, ResourceLoader resourceLoader) {
    Assert.notNull(view, "View is required");
    Assert.notNull(environment, "Environment is required");
    Assert.notNull(classLoader, "ClassLoader is required");
    Assert.notNull(resourceLoader, "ResourceLoader is required");
    boolean useCache = environment.getFlag("infra.template.provider.cache", true);
    if (!useCache) {
      return findProvider(view, environment, classLoader, resourceLoader);
    }
    TemplateAvailabilityProvider provider = this.resolved.get(view);
    if (provider == null) {
      synchronized(this.cache) {
        provider = findProvider(view, environment, classLoader, resourceLoader);
        if (provider == null) {
          provider = NONE;
        }
        this.resolved.put(view, provider);
        this.cache.put(view, provider);
      }
    }
    return provider != NONE ? provider : null;
  }

  @Nullable
  private TemplateAvailabilityProvider findProvider(String view,
          Environment environment, ClassLoader classLoader, ResourceLoader resourceLoader) {
    for (TemplateAvailabilityProvider candidate : this.providers) {
      if (candidate.isTemplateAvailable(view, environment, classLoader, resourceLoader)) {
        return candidate;
      }
    }
    return null;
  }

  private static class NoTemplateAvailabilityProvider implements TemplateAvailabilityProvider {

    @Override
    public boolean isTemplateAvailable(String view, Environment environment, ClassLoader classLoader,
            ResourceLoader resourceLoader) {
      return false;
    }

  }

}
