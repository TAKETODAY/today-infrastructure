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

package infra.app.context.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import infra.lang.Nullable;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * Imports {@link ConfigData} by {@link ConfigDataLocationResolver resolving} and
 * {@link ConfigDataLoader loading} locations. {@link ConfigDataResource resources} are
 * tracked to ensure that they are not imported multiple times.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class ConfigDataImporter {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final ConfigDataLoaders loaders;
  private final ConfigDataLocationResolvers resolvers;
  private final ConfigDataNotFoundAction notFoundAction;

  private final HashSet<ConfigDataResource> loaded = new HashSet<>();
  private final HashSet<ConfigDataLocation> loadedLocations = new HashSet<>();
  private final HashSet<ConfigDataLocation> optionalLocations = new HashSet<>();

  /**
   * Create a new {@link ConfigDataImporter} instance.
   *
   * @param notFoundAction the action to take when a location cannot be found
   * @param resolvers the config data location resolvers
   * @param loaders the config data loaders
   */
  ConfigDataImporter(ConfigDataNotFoundAction notFoundAction,
          ConfigDataLocationResolvers resolvers, ConfigDataLoaders loaders) {
    this.resolvers = resolvers;
    this.loaders = loaders;
    this.notFoundAction = notFoundAction;
  }

  /**
   * Resolve and load the given list of locations, filtering any that have been
   * previously loaded.
   *
   * @param activationContext the activation context
   * @param locationResolverContext the location resolver context
   * @param loaderContext the loader context
   * @param locations the locations to resolve
   * @return a map of the loaded locations and data
   */
  Map<ConfigDataResolutionResult, ConfigData> resolveAndLoad(
          @Nullable ConfigDataActivationContext activationContext,
          ConfigDataLocationResolverContext locationResolverContext,
          ConfigDataLoaderContext loaderContext, List<ConfigDataLocation> locations) {
    try {
      Profiles profiles = activationContext != null ? activationContext.profiles : null;
      List<ConfigDataResolutionResult> resolved = resolve(locationResolverContext, profiles, locations);
      return load(loaderContext, resolved);
    }
    catch (IOException ex) {
      throw new IllegalStateException("IO error on loading imports from " + locations, ex);
    }
  }

  private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext locationResolverContext,
          @Nullable Profiles profiles, List<ConfigDataLocation> locations) {
    List<ConfigDataResolutionResult> resolved = new ArrayList<>(locations.size());
    for (ConfigDataLocation location : locations) {
      resolved.addAll(resolve(locationResolverContext, profiles, location));
    }
    return Collections.unmodifiableList(resolved);
  }

  private List<ConfigDataResolutionResult> resolve(ConfigDataLocationResolverContext locationResolverContext,
          @Nullable Profiles profiles, ConfigDataLocation location) {
    try {
      return this.resolvers.resolve(locationResolverContext, location, profiles);
    }
    catch (ConfigDataNotFoundException ex) {
      handle(ex, location, null);
      return Collections.emptyList();
    }
  }

  private Map<ConfigDataResolutionResult, ConfigData> load(
          ConfigDataLoaderContext loaderContext, List<ConfigDataResolutionResult> candidates) throws IOException {

    LinkedHashMap<ConfigDataResolutionResult, ConfigData> result = new LinkedHashMap<>();
    for (int i = candidates.size() - 1; i >= 0; i--) {
      ConfigDataResolutionResult candidate = candidates.get(i);
      ConfigDataLocation location = candidate.getLocation();
      ConfigDataResource resource = candidate.getResource();
      if (resource.isOptional()) {
        this.optionalLocations.add(location);
      }
      if (this.loaded.contains(resource)) {
        this.loadedLocations.add(location);
      }
      else {
        try {
          ConfigData loaded = this.loaders.load(loaderContext, resource);
          if (loaded != null) {
            this.loaded.add(resource);
            this.loadedLocations.add(location);
            result.put(candidate, loaded);
          }
        }
        catch (ConfigDataNotFoundException ex) {
          handle(ex, location, resource);
        }
      }
    }
    return Collections.unmodifiableMap(result);
  }

  private void handle(ConfigDataNotFoundException ex, ConfigDataLocation location, @Nullable ConfigDataResource resource) {
    if (ex instanceof ConfigDataResourceNotFoundException) {
      ex = ((ConfigDataResourceNotFoundException) ex).withLocation(location);
    }
    getNotFoundAction(location, resource).handle(this.logger, ex);
  }

  private ConfigDataNotFoundAction getNotFoundAction(ConfigDataLocation location, @Nullable ConfigDataResource resource) {
    if (location.isOptional() || (resource != null && resource.isOptional())) {
      return ConfigDataNotFoundAction.IGNORE;
    }
    return this.notFoundAction;
  }

  Set<ConfigDataLocation> getLoadedLocations() {
    return this.loadedLocations;
  }

  Set<ConfigDataLocation> getOptionalLocations() {
    return this.optionalLocations;
  }

}
