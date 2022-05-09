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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.BootstrapRegistry;
import cn.taketoday.framework.BootstrapRegistry.InstanceSupplier;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.DefaultPropertiesPropertySource;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributors.BinderOption;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.StringUtils;

/**
 * Wrapper around a {@link ConfigurableEnvironment} that can be used to import and apply
 * {@link ConfigData}. Configures the initial set of
 * {@link ConfigDataEnvironmentContributors} by wrapping property sources from the
 * {@link Environment} and adding the initial set of locations.
 * <p>
 * The initial locations can be influenced via the {@link #LOCATION_PROPERTY},
 * {@value #ADDITIONAL_LOCATION_PROPERTY} and {@value #IMPORT_PROPERTY} properties. If no
 * explicit properties are set, the {@link #DEFAULT_SEARCH_LOCATIONS} will be used.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigDataEnvironment {

  /**
   * Property used override the imported locations.
   */
  static final String LOCATION_PROPERTY = "context.config.location";

  /**
   * Property used to provide additional locations to import.
   */
  static final String ADDITIONAL_LOCATION_PROPERTY = "context.config.additional-location";

  /**
   * Property used to provide additional locations to import.
   */
  static final String IMPORT_PROPERTY = "context.config.import";

  /**
   * Property used to determine what action to take when a
   * {@code ConfigDataNotFoundAction} is thrown.
   *
   * @see ConfigDataNotFoundAction
   */
  static final String ON_NOT_FOUND_PROPERTY = "context.config.on-not-found";

  /**
   * Default search locations used if not {@link #LOCATION_PROPERTY} is found.
   */
  static final ConfigDataLocation[] DEFAULT_SEARCH_LOCATIONS = {
          ConfigDataLocation.valueOf("optional:classpath:/;optional:classpath:/config/"),
          ConfigDataLocation.valueOf("optional:file:./;optional:file:./config/;optional:file:./config/*/")
  };

  private static final ConfigDataLocation[] EMPTY_LOCATIONS = new ConfigDataLocation[0];

  private static final Bindable<ConfigDataLocation[]> CONFIG_DATA_LOCATION_ARRAY = Bindable.of(ConfigDataLocation[].class);

  private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);

  private static final BinderOption[] ALLOW_INACTIVE_BINDING = {};

  private static final BinderOption[] DENY_INACTIVE_BINDING = { BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE };

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final boolean traceEnabled = log.isTraceEnabled();

  private final ConfigDataNotFoundAction notFoundAction;

  private final ConfigurableBootstrapContext bootstrapContext;

  private final ConfigurableEnvironment environment;

  private final ConfigDataLocationResolvers resolvers;

  private final Collection<String> additionalProfiles;

  private final ConfigDataEnvironmentUpdateListener environmentUpdateListener;

  private final ConfigDataLoaders loaders;

  private final ConfigDataEnvironmentContributors contributors;

  /**
   * Create a new {@link ConfigDataEnvironment} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param environment the {@link Environment}.
   * @param resourceLoader {@link ResourceLoader} to load resource locations
   * @param additionalProfiles any additional profiles to activate
   * @param environmentUpdateListener optional
   * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
   * {@link Environment} updates.
   */
  ConfigDataEnvironment(
          ConfigurableBootstrapContext bootstrapContext,
          ConfigurableEnvironment environment,
          ResourceLoader resourceLoader,
          Collection<String> additionalProfiles,
          @Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener
  ) {
    Binder binder = Binder.get(environment);
    this.environment = environment;
    this.bootstrapContext = bootstrapContext;
    this.additionalProfiles = additionalProfiles;
    this.notFoundAction = binder.bind(
            ON_NOT_FOUND_PROPERTY, ConfigDataNotFoundAction.class).orRequired(ConfigDataNotFoundAction.FAIL);
    this.resolvers = createConfigDataLocationResolvers(bootstrapContext, binder, resourceLoader);
    this.environmentUpdateListener = Optional.ofNullable(environmentUpdateListener).orElse(
            ConfigDataEnvironmentUpdateListener.NONE
    );
    this.loaders = new ConfigDataLoaders(bootstrapContext, resourceLoader.getClassLoader());
    this.contributors = createContributors(binder);
  }

  protected ConfigDataLocationResolvers createConfigDataLocationResolvers(
          ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
    return new ConfigDataLocationResolvers(bootstrapContext, binder, resourceLoader);
  }

  private ConfigDataEnvironmentContributors createContributors(Binder binder) {
    if (traceEnabled) {
      log.trace("Building config data environment contributors");
    }
    PropertySources propertySources = environment.getPropertySources();
    var contributors = new ArrayList<ConfigDataEnvironmentContributor>(propertySources.size() + 10);
    PropertySource<?> defaultPropertySource = null;
    for (PropertySource<?> propertySource : propertySources) {
      if (DefaultPropertiesPropertySource.hasMatchingName(propertySource)) {
        defaultPropertySource = propertySource;
      }
      else {
        if (traceEnabled) {
          log.trace("Creating wrapped config data contributor for '{}'", propertySource.getName());
        }
        contributors.add(ConfigDataEnvironmentContributor.ofExisting(propertySource));
      }
    }
    contributors.addAll(getInitialImportContributors(binder));
    if (defaultPropertySource != null) {
      if (traceEnabled) {
        log.trace("Creating wrapped config data contributor for default property source");
      }
      contributors.add(ConfigDataEnvironmentContributor.ofExisting(defaultPropertySource));
    }
    return createContributors(contributors);
  }

  protected ConfigDataEnvironmentContributors createContributors(List<ConfigDataEnvironmentContributor> contributors) {
    return new ConfigDataEnvironmentContributors(bootstrapContext, contributors);
  }

  ConfigDataEnvironmentContributors getContributors() {
    return contributors;
  }

  private List<ConfigDataEnvironmentContributor> getInitialImportContributors(Binder binder) {
    var contributors = new ArrayList<ConfigDataEnvironmentContributor>();
    addInitialImportContributors(contributors, bindLocations(binder, IMPORT_PROPERTY, EMPTY_LOCATIONS));
    addInitialImportContributors(contributors, bindLocations(binder, ADDITIONAL_LOCATION_PROPERTY, EMPTY_LOCATIONS));
    addInitialImportContributors(contributors, bindLocations(binder, LOCATION_PROPERTY, DEFAULT_SEARCH_LOCATIONS));
    return contributors;
  }

  private ConfigDataLocation[] bindLocations(Binder binder, String propertyName, ConfigDataLocation[] other) {
    return Objects.requireNonNull(binder.bind(propertyName, CONFIG_DATA_LOCATION_ARRAY).orElse(other));
  }

  private void addInitialImportContributors(
          List<ConfigDataEnvironmentContributor> initialContributors, ConfigDataLocation[] locations) {
    for (int i = locations.length - 1; i >= 0; i--) {
      initialContributors.add(createInitialImportContributor(locations[i]));
    }
  }

  private ConfigDataEnvironmentContributor createInitialImportContributor(ConfigDataLocation location) {
    if (traceEnabled)
      log.trace("Adding initial config data import from location '{}'", location);
    return ConfigDataEnvironmentContributor.ofInitialImport(location);
  }

  /**
   * Process all contributions and apply any newly imported property sources to the
   * {@link Environment}.
   */
  void processAndApply() {
    ConfigDataImporter importer = new ConfigDataImporter(notFoundAction, resolvers, loaders);
    registerBootstrapBinder(contributors, null, DENY_INACTIVE_BINDING);
    ConfigDataEnvironmentContributors contributors = processInitial(this.contributors, importer);
    ConfigDataActivationContext activationContext = createActivationContext(
            contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE));
    contributors = processWithoutProfiles(contributors, importer, activationContext);
    activationContext = withProfiles(contributors, activationContext);
    contributors = processWithProfiles(contributors, importer, activationContext);
    applyToEnvironment(contributors, activationContext,
            importer.getLoadedLocations(), importer.getOptionalLocations());
  }

  private ConfigDataEnvironmentContributors processInitial(
          ConfigDataEnvironmentContributors contributors, ConfigDataImporter importer) {
    if (traceEnabled)
      log.trace("Processing initial config data environment contributors without activation context");
    contributors = contributors.withProcessedImports(importer, null);
    registerBootstrapBinder(contributors, null, DENY_INACTIVE_BINDING);
    return contributors;
  }

  private ConfigDataActivationContext createActivationContext(Binder initialBinder) {
    if (traceEnabled)
      log.trace("Creating config data activation context from initial contributions");
    try {
      return new ConfigDataActivationContext(environment, initialBinder);
    }
    catch (BindException ex) {
      if (ex.getCause() instanceof InactiveConfigDataAccessException) {
        throw (InactiveConfigDataAccessException) ex.getCause();
      }
      throw ex;
    }
  }

  private ConfigDataEnvironmentContributors processWithoutProfiles(
          ConfigDataEnvironmentContributors contributors,
          ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
    if (traceEnabled)
      log.trace("Processing config data environment contributors with initial activation context");
    contributors = contributors.withProcessedImports(importer, activationContext);
    registerBootstrapBinder(contributors, activationContext, DENY_INACTIVE_BINDING);
    return contributors;
  }

  private ConfigDataActivationContext withProfiles(
          ConfigDataEnvironmentContributors contributors, ConfigDataActivationContext activationContext) {
    if (traceEnabled)
      log.trace("Deducing profiles from current config data environment contributors");
    Binder binder = contributors.getBinder(activationContext,
            contributor -> !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES),
            BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    try {
      LinkedHashSet<String> additionalProfiles = new LinkedHashSet<>(this.additionalProfiles);
      additionalProfiles.addAll(getIncludedProfiles(contributors, activationContext));
      Profiles profiles = new Profiles(environment, binder, additionalProfiles);
      return activationContext.withProfiles(profiles);
    }
    catch (BindException ex) {
      if (ex.getCause() instanceof InactiveConfigDataAccessException inactive) {
        throw inactive;
      }
      throw ex;
    }
  }

  private Collection<? extends String> getIncludedProfiles(
          ConfigDataEnvironmentContributors contributors, ConfigDataActivationContext activationContext) {
    var placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            contributors, activationContext, null, true);
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
      if (source != null && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
        Binder binder = new Binder(Collections.singleton(source), placeholdersResolver);
        binder.bind(Profiles.INCLUDE_PROFILES, STRING_LIST).ifBound(includes -> {
          if (!contributor.isActive(activationContext)) {
            InactiveConfigDataAccessException.throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES);
            InactiveConfigDataAccessException.throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES.append("[0]"));
          }
          result.addAll(includes);
        });
      }
    }
    return result;
  }

  private ConfigDataEnvironmentContributors processWithProfiles(ConfigDataEnvironmentContributors contributors,
          ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
    if (traceEnabled)
      log.trace("Processing config data environment contributors with profile activation context");
    contributors = contributors.withProcessedImports(importer, activationContext);
    registerBootstrapBinder(contributors, activationContext, ALLOW_INACTIVE_BINDING);
    return contributors;
  }

  private void registerBootstrapBinder(ConfigDataEnvironmentContributors contributors,
          @Nullable ConfigDataActivationContext activationContext, BinderOption... binderOptions) {
    bootstrapContext.register(Binder.class,
            InstanceSupplier.from(() -> contributors.getBinder(activationContext, binderOptions))
                    .withScope(BootstrapRegistry.Scope.PROTOTYPE)
    );
  }

  private void applyToEnvironment(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations, Set<ConfigDataLocation> optionalLocations) {
    checkForInvalidProperties(contributors);
    checkMandatoryLocations(contributors, activationContext, loadedLocations, optionalLocations);
    PropertySources propertySources = environment.getPropertySources();
    applyContributor(contributors, activationContext, propertySources);
    DefaultPropertiesPropertySource.moveToEnd(propertySources);
    Profiles profiles = activationContext.getProfiles();
    if (profiles != null) {
      if (traceEnabled) {
        log.trace("Setting default profiles: {}", profiles.getDefault());
      }
      environment.setDefaultProfiles(StringUtils.toStringArray(profiles.getDefault()));
      if (traceEnabled) {
        log.trace("Setting active profiles: {}", profiles.getActive());
      }
      environment.setActiveProfiles(StringUtils.toStringArray(profiles.getActive()));
      environmentUpdateListener.onSetProfiles(profiles);
    }
    else {
      log.info("profiles not found");
    }
  }

  private void applyContributor(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, PropertySources propertySources) {
    boolean traceEnabled = this.traceEnabled;
    if (traceEnabled)
      log.trace("Applying config data environment contributions");
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      PropertySource<?> propertySource = contributor.getPropertySource();
      if (contributor.getKind() == ConfigDataEnvironmentContributor.Kind.BOUND_IMPORT && propertySource != null) {
        if (!contributor.isActive(activationContext)) {
          if (traceEnabled)
            log.trace("Skipping inactive property source '{}'", propertySource.getName());
        }
        else {
          if (traceEnabled)
            log.trace("Adding imported property source '{}'", propertySource.getName());
          propertySources.addLast(propertySource);
          environmentUpdateListener.onPropertySourceAdded(propertySource, contributor.getLocation(), contributor.getResource());
        }
      }
    }
  }

  private void checkForInvalidProperties(ConfigDataEnvironmentContributors contributors) {
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      InvalidConfigDataPropertyException.throwIfPropertyFound(contributor);
    }
  }

  private void checkMandatoryLocations(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations,
          Set<ConfigDataLocation> optionalLocations) {
    Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>();
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      if (contributor.isActive(activationContext)) {
        mandatoryLocations.addAll(getMandatoryImports(contributor));
      }
    }
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      if (contributor.getLocation() != null) {
        mandatoryLocations.remove(contributor.getLocation());
      }
    }
    mandatoryLocations.removeAll(loadedLocations);
    mandatoryLocations.removeAll(optionalLocations);
    if (!mandatoryLocations.isEmpty()) {
      for (ConfigDataLocation mandatoryLocation : mandatoryLocations) {
        notFoundAction.handle(log, new ConfigDataLocationNotFoundException(mandatoryLocation));
      }
    }
  }

  private Set<ConfigDataLocation> getMandatoryImports(ConfigDataEnvironmentContributor contributor) {
    List<ConfigDataLocation> imports = contributor.getImports();
    LinkedHashSet<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>(imports.size());
    for (ConfigDataLocation location : imports) {
      if (!location.isOptional()) {
        mandatoryLocations.add(location);
      }
    }
    return mandatoryLocations;
  }

}
