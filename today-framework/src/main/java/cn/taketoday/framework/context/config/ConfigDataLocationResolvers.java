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

package cn.taketoday.framework.context.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.BootstrapContext;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.Instantiator;

/**
 * A collection of {@link ConfigDataLocationResolver} instances loaded via
 * {@code today-strategies.properties}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class ConfigDataLocationResolvers {

  private final List<ConfigDataLocationResolver<?>> resolvers;

  /**
   * Create a new {@link ConfigDataLocationResolvers} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param binder a binder providing values from the initial {@link Environment}
   * @param resourceLoader {@link ResourceLoader} to load resource locations
   */
  ConfigDataLocationResolvers(ConfigurableBootstrapContext bootstrapContext,
          Binder binder, ResourceLoader resourceLoader) {
    this(bootstrapContext, binder, resourceLoader, TodayStrategies.getStrategiesNames(
            ConfigDataLocationResolver.class, resourceLoader.getClassLoader()));
  }

  /**
   * Create a new {@link ConfigDataLocationResolvers} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param binder {@link Binder} providing values from the initial {@link Environment}
   * @param resourceLoader {@link ResourceLoader} to load resource locations
   * @param names the {@link ConfigDataLocationResolver} class names
   */
  ConfigDataLocationResolvers(ConfigurableBootstrapContext bootstrapContext,
          Binder binder, ResourceLoader resourceLoader, List<String> names) {
    Instantiator<ConfigDataLocationResolver<?>> instantiator = new Instantiator<>(ConfigDataLocationResolver.class,
            (availableParameters) -> {
              availableParameters.add(Binder.class, binder);
              availableParameters.add(ResourceLoader.class, resourceLoader);
              availableParameters.add(ConfigurableBootstrapContext.class, bootstrapContext);
              availableParameters.add(BootstrapContext.class, bootstrapContext);
              availableParameters.add(BootstrapRegistry.class, bootstrapContext);
            });
    this.resolvers = reorder(instantiator.instantiate(resourceLoader.getClassLoader(), names));
  }

  private List<ConfigDataLocationResolver<?>> reorder(List<ConfigDataLocationResolver<?>> resolvers) {
    List<ConfigDataLocationResolver<?>> reordered = new ArrayList<>(resolvers.size());
    StandardConfigDataLocationResolver resourceResolver = null;
    for (ConfigDataLocationResolver<?> resolver : resolvers) {
      if (resolver instanceof StandardConfigDataLocationResolver) {
        resourceResolver = (StandardConfigDataLocationResolver) resolver;
      }
      else {
        reordered.add(resolver);
      }
    }
    if (resourceResolver != null) {
      reordered.add(resourceResolver);
    }
    return Collections.unmodifiableList(reordered);
  }

  List<ConfigDataResolutionResult> resolve(
          ConfigDataLocationResolverContext context,
          @Nullable ConfigDataLocation location, @Nullable Profiles profiles) {
    if (location == null) {
      return Collections.emptyList();
    }
    for (ConfigDataLocationResolver<?> resolver : getResolvers()) {
      if (resolver.isResolvable(context, location)) {
        return resolve(resolver, context, location, profiles);
      }
    }
    throw new UnsupportedConfigDataLocationException(location);
  }

  private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolver<?> resolver,
          ConfigDataLocationResolverContext context, ConfigDataLocation location, @Nullable Profiles profiles) {
    List<ConfigDataResolutionResult> resolved = resolve(location, false, () -> resolver.resolve(context, location));
    if (profiles == null) {
      return resolved;
    }
    List<ConfigDataResolutionResult> profileSpecific = resolve(
            location, true, () -> resolver.resolveProfileSpecific(context, location, profiles));
    return merge(resolved, profileSpecific);
  }

  private List<ConfigDataResolutionResult> resolve(ConfigDataLocation location, boolean profileSpecific,
          Supplier<List<? extends ConfigDataResource>> resolveAction) {
    List<ConfigDataResource> resources = nonNullList(resolveAction.get());
    List<ConfigDataResolutionResult> resolved = new ArrayList<>(resources.size());
    for (ConfigDataResource resource : resources) {
      resolved.add(new ConfigDataResolutionResult(location, resource, profileSpecific));
    }
    return resolved;
  }

  @SuppressWarnings("unchecked")
  private <T> List<T> nonNullList(@Nullable List<? extends T> list) {
    return (list != null) ? (List<T>) list : Collections.emptyList();
  }

  private <T> List<T> merge(List<T> list1, List<T> list2) {
    List<T> merged = new ArrayList<>(list1.size() + list2.size());
    merged.addAll(list1);
    merged.addAll(list2);
    return merged;
  }

  /**
   * Return the resolvers managed by this object.
   *
   * @return the resolvers
   */
  List<ConfigDataLocationResolver<?>> getResolvers() {
    return this.resolvers;
  }

}
