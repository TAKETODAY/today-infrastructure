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
import java.util.Set;

import cn.taketoday.context.properties.bind.BindException;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.bind.PlaceholdersResolver;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.core.env.ConfigurableEnvironment;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.core.env.PropertySources;
import cn.taketoday.core.io.ResourceLoader;
import cn.taketoday.framework.DefaultPropertiesPropertySource;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributors.BinderOption;
import cn.taketoday.logging.LogMessage;
import cn.taketoday.logging.Logger;
import cn.taketoday.util.StringUtils;

/**
 * Wrapper around a {@link ConfigurableEnvironment} that can be used to import and apply
 * {@link ConfigData}. Configures the initial set of
 * {@link ConfigDataEnvironmentContributors} by wrapping property sources from the Spring
 * {@link Environment} and adding the initial set of locations.
 * <p>
 * The initial locations can be influenced via the {@link #LOCATION_PROPERTY},
 * {@value #ADDITIONAL_LOCATION_PROPERTY} and {@value #IMPORT_PROPERTY} properties. If no
 * explicit properties are set, the {@link #DEFAULT_SEARCH_LOCATIONS} will be used.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
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
  static final ConfigDataLocation[] DEFAULT_SEARCH_LOCATIONS;

  static {
    List<ConfigDataLocation> locations = new ArrayList<>();
    locations.add(ConfigDataLocation.of("optional:classpath:/;optional:classpath:/config/"));
    locations.add(ConfigDataLocation.of("optional:file:./;optional:file:./config/;optional:file:./config/*/"));
    DEFAULT_SEARCH_LOCATIONS = locations.toArray(new ConfigDataLocation[0]);
  }

  private static final ConfigDataLocation[] EMPTY_LOCATIONS = new ConfigDataLocation[0];

  private static final Bindable<ConfigDataLocation[]> CONFIG_DATA_LOCATION_ARRAY = Bindable
          .of(ConfigDataLocation[].class);

  private static final Bindable<List<String>> STRING_LIST = Bindable.listOf(String.class);

  private static final BinderOption[] ALLOW_INACTIVE_BINDING = {};

  private static final BinderOption[] DENY_INACTIVE_BINDING = { BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE };

  private final DeferredLogFactory logFactory;

  private final Logger logger;

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
   * @param logFactory the deferred log factory
   * @param bootstrapContext the bootstrap context
   * @param environment the Spring {@link Environment}.
   * @param resourceLoader {@link ResourceLoader} to load resource locations
   * @param additionalProfiles any additional profiles to activate
   * @param environmentUpdateListener optional
   * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
   * {@link Environment} updates.
   */
  ConfigDataEnvironment(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,
          ConfigurableEnvironment environment, ResourceLoader resourceLoader, Collection<String> additionalProfiles,
          ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
    Binder binder = Binder.get(environment);
    this.logFactory = logFactory;
    this.logger = logFactory.getLog(getClass());
    this.notFoundAction = binder.bind(ON_NOT_FOUND_PROPERTY, ConfigDataNotFoundAction.class)
            .orElse(ConfigDataNotFoundAction.FAIL);
    this.bootstrapContext = bootstrapContext;
    this.environment = environment;
    this.resolvers = createConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader);
    this.additionalProfiles = additionalProfiles;
    this.environmentUpdateListener = (environmentUpdateListener != null) ? environmentUpdateListener
                                                                         : ConfigDataEnvironmentUpdateListener.NONE;
    this.loaders = new ConfigDataLoaders(logFactory, bootstrapContext, resourceLoader.getClassLoader());
    this.contributors = createContributors(binder);
  }

  protected ConfigDataLocationResolvers createConfigDataLocationResolvers(DeferredLogFactory logFactory,
          ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
    return new ConfigDataLocationResolvers(logFactory, bootstrapContext, binder, resourceLoader);
  }

  private ConfigDataEnvironmentContributors createContributors(Binder binder) {
    this.logger.trace("Building config data environment contributors");
    PropertySources propertySources = this.environment.getPropertySources();
    List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(propertySources.size() + 10);
    PropertySource<?> defaultPropertySource = null;
    for (PropertySource<?> propertySource : propertySources) {
      if (DefaultPropertiesPropertySource.hasMatchingName(propertySource)) {
        defaultPropertySource = propertySource;
      }
      else {
        this.logger.trace(LogMessage.format("Creating wrapped config data contributor for '%s'",
                propertySource.getName()));
        contributors.add(ConfigDataEnvironmentContributor.ofExisting(propertySource));
      }
    }
    contributors.addAll(getInitialImportContributors(binder));
    if (defaultPropertySource != null) {
      this.logger.trace("Creating wrapped config data contributor for default property source");
      contributors.add(ConfigDataEnvironmentContributor.ofExisting(defaultPropertySource));
    }
    return createContributors(contributors);
  }

  protected ConfigDataEnvironmentContributors createContributors(
          List<ConfigDataEnvironmentContributor> contributors) {
    return new ConfigDataEnvironmentContributors(this.logFactory, this.bootstrapContext, contributors);
  }

  ConfigDataEnvironmentContributors getContributors() {
    return this.contributors;
  }

  private List<ConfigDataEnvironmentContributor> getInitialImportContributors(Binder binder) {
    List<ConfigDataEnvironmentContributor> initialContributors = new ArrayList<>();
    addInitialImportContributors(initialContributors, bindLocations(binder, IMPORT_PROPERTY, EMPTY_LOCATIONS));
    addInitialImportContributors(initialContributors,
            bindLocations(binder, ADDITIONAL_LOCATION_PROPERTY, EMPTY_LOCATIONS));
    addInitialImportContributors(initialContributors,
            bindLocations(binder, LOCATION_PROPERTY, DEFAULT_SEARCH_LOCATIONS));
    return initialContributors;
  }

  private ConfigDataLocation[] bindLocations(Binder binder, String propertyName, ConfigDataLocation[] other) {
    return binder.bind(propertyName, CONFIG_DATA_LOCATION_ARRAY).orElse(other);
  }

  private void addInitialImportContributors(List<ConfigDataEnvironmentContributor> initialContributors,
          ConfigDataLocation[] locations) {
    for (int i = locations.length - 1; i >= 0; i--) {
      initialContributors.add(createInitialImportContributor(locations[i]));
    }
  }

  private ConfigDataEnvironmentContributor createInitialImportContributor(ConfigDataLocation location) {
    this.logger.trace(LogMessage.format("Adding initial config data import from location '%s'", location));
    return ConfigDataEnvironmentContributor.ofInitialImport(location);
  }

  /**
   * Process all contributions and apply any newly imported property sources to the
   * {@link Environment}.
   */
  void processAndApply() {
    ConfigDataImporter importer = new ConfigDataImporter(this.logFactory, this.notFoundAction, this.resolvers,
            this.loaders);
    registerBootstrapBinder(this.contributors, null, DENY_INACTIVE_BINDING);
    ConfigDataEnvironmentContributors contributors = processInitial(this.contributors, importer);
    ConfigDataActivationContext activationContext = createActivationContext(
            contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE));
    contributors = processWithoutProfiles(contributors, importer, activationContext);
    activationContext = withProfiles(contributors, activationContext);
    contributors = processWithProfiles(contributors, importer, activationContext);
    applyToEnvironment(contributors, activationContext, importer.getLoadedLocations(),
            importer.getOptionalLocations());
  }

  private ConfigDataEnvironmentContributors processInitial(ConfigDataEnvironmentContributors contributors,
          ConfigDataImporter importer) {
    this.logger.trace("Processing initial config data environment contributors without activation context");
    contributors = contributors.withProcessedImports(importer, null);
    registerBootstrapBinder(contributors, null, DENY_INACTIVE_BINDING);
    return contributors;
  }

  private ConfigDataActivationContext createActivationContext(Binder initialBinder) {
    this.logger.trace("Creating config data activation context from initial contributions");
    try {
      return new ConfigDataActivationContext(this.environment, initialBinder);
    }
    catch (BindException ex) {
      if (ex.getCause() instanceof InactiveConfigDataAccessException) {
        throw (InactiveConfigDataAccessException) ex.getCause();
      }
      throw ex;
    }
  }

  private ConfigDataEnvironmentContributors processWithoutProfiles(ConfigDataEnvironmentContributors contributors,
          ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
    this.logger.trace("Processing config data environment contributors with initial activation context");
    contributors = contributors.withProcessedImports(importer, activationContext);
    registerBootstrapBinder(contributors, activationContext, DENY_INACTIVE_BINDING);
    return contributors;
  }

  private ConfigDataActivationContext withProfiles(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext) {
    this.logger.trace("Deducing profiles from current config data environment contributors");
    Binder binder = contributors.getBinder(activationContext,
            (contributor) -> !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES),
            BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    try {
      Set<String> additionalProfiles = new LinkedHashSet<>(this.additionalProfiles);
      additionalProfiles.addAll(getIncludedProfiles(contributors, activationContext));
      Profiles profiles = new Profiles(this.environment, binder, additionalProfiles);
      return activationContext.withProfiles(profiles);
    }
    catch (BindException ex) {
      if (ex.getCause() instanceof InactiveConfigDataAccessException) {
        throw (InactiveConfigDataAccessException) ex.getCause();
      }
      throw ex;
    }
  }

  private Collection<? extends String> getIncludedProfiles(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext) {
    PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            contributors, activationContext, null, true);
    Set<String> result = new LinkedHashSet<>();
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      ConfigurationPropertySource source = contributor.getConfigurationPropertySource();
      if (source != null && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
        Binder binder = new Binder(Collections.singleton(source), placeholdersResolver);
        binder.bind(Profiles.INCLUDE_PROFILES, STRING_LIST).ifBound((includes) -> {
          if (!contributor.isActive(activationContext)) {
            InactiveConfigDataAccessException.throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES);
            InactiveConfigDataAccessException.throwIfPropertyFound(contributor,
                    Profiles.INCLUDE_PROFILES.append("[0]"));
          }
          result.addAll(includes);
        });
      }
    }
    return result;
  }

  private ConfigDataEnvironmentContributors processWithProfiles(ConfigDataEnvironmentContributors contributors,
          ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
    this.logger.trace("Processing config data environment contributors with profile activation context");
    contributors = contributors.withProcessedImports(importer, activationContext);
    registerBootstrapBinder(contributors, activationContext, ALLOW_INACTIVE_BINDING);
    return contributors;
  }

  private void registerBootstrapBinder(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, BinderOption... binderOptions) {
    this.bootstrapContext.register(Binder.class, InstanceSupplier
            .from(() -> contributors.getBinder(activationContext, binderOptions)).withScope(Scope.PROTOTYPE));
  }

  private void applyToEnvironment(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations,
          Set<ConfigDataLocation> optionalLocations) {
    checkForInvalidProperties(contributors);
    checkMandatoryLocations(contributors, activationContext, loadedLocations, optionalLocations);
    MutablePropertySources propertySources = this.environment.getPropertySources();
    applyContributor(contributors, activationContext, propertySources);
    DefaultPropertiesPropertySource.moveToEnd(propertySources);
    Profiles profiles = activationContext.getProfiles();
    this.logger.trace(LogMessage.format("Setting default profiles: %s", profiles.getDefault()));
    this.environment.setDefaultProfiles(StringUtils.toStringArray(profiles.getDefault()));
    this.logger.trace(LogMessage.format("Setting active profiles: %s", profiles.getActive()));
    this.environment.setActiveProfiles(StringUtils.toStringArray(profiles.getActive()));
    this.environmentUpdateListener.onSetProfiles(profiles);
  }

  private void applyContributor(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, MutablePropertySources propertySources) {
    this.logger.trace("Applying config data environment contributions");
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      PropertySource<?> propertySource = contributor.getPropertySource();
      if (contributor.getKind() == ConfigDataEnvironmentContributor.Kind.BOUND_IMPORT && propertySource != null) {
        if (!contributor.isActive(activationContext)) {
          this.logger.trace(
                  LogMessage.format("Skipping inactive property source '%s'", propertySource.getName()));
        }
        else {
          this.logger
                  .trace(LogMessage.format("Adding imported property source '%s'", propertySource.getName()));
          propertySources.addLast(propertySource);
          this.environmentUpdateListener.onPropertySourceAdded(propertySource, contributor.getLocation(),
                  contributor.getResource());
        }
      }
    }
  }

  private void checkForInvalidProperties(ConfigDataEnvironmentContributors contributors) {
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      InvalidConfigDataPropertyException.throwOrWarn(this.logger, contributor);
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
        this.notFoundAction.handle(this.logger, new ConfigDataLocationNotFoundException(mandatoryLocation));
      }
    }
  }

  private Set<ConfigDataLocation> getMandatoryImports(ConfigDataEnvironmentContributor contributor) {
    List<ConfigDataLocation> imports = contributor.getImports();
    Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>(imports.size());
    for (ConfigDataLocation location : imports) {
      if (!location.isOptional()) {
        mandatoryLocations.add(location);
      }
    }
    return mandatoryLocations;
  }

}
