/*
 * Copyright 2017 - 2025 the original author or authors.
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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import infra.app.ConfigurableBootstrapContext;
import infra.app.DefaultPropertiesPropertySource;
import infra.context.properties.bind.BindException;
import infra.context.properties.bind.Bindable;
import infra.context.properties.bind.Binder;
import infra.context.properties.source.ConfigurationPropertySource;
import infra.core.env.ConfigurableEnvironment;
import infra.core.env.Environment;
import infra.core.env.PropertySource;
import infra.core.env.PropertySources;
import infra.core.io.ResourceLoader;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.util.StringUtils;

import static infra.app.BootstrapRegistry.InstanceSupplier;
import static infra.app.BootstrapRegistry.Scope;
import static infra.app.context.config.InactiveConfigDataAccessException.throwIfPropertyFound;

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
  static final String LOCATION_PROPERTY = "app.config.location";

  /**
   * Property used to provide additional locations to import.
   */
  static final String ADDITIONAL_LOCATION_PROPERTY = "app.config.additional-location";

  /**
   * Property used to provide additional locations to import.
   */
  static final String IMPORT_PROPERTY = "app.config.import";

  /**
   * Property used to determine what action to take when a
   * {@code ConfigDataNotFoundAction} is thrown.
   *
   * @see ConfigDataNotFoundAction
   */
  static final String ON_NOT_FOUND_PROPERTY = "app.config.on-not-found";

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

  private static final ConfigDataEnvironmentContributors.BinderOption[] ALLOW_INACTIVE_BINDING = {};

  private static final ConfigDataEnvironmentContributors.BinderOption[] DENY_INACTIVE_BINDING = { ConfigDataEnvironmentContributors.BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE };

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private final boolean traceEnabled = logger.isTraceEnabled();

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
   * @param bootstrap the bootstrap context
   * @param environment the {@link Environment}.
   * @param resourceLoader {@link ResourceLoader} to load resource locations
   * @param additionalProfiles any additional profiles to activate
   * @param environmentUpdateListener optional
   * {@link ConfigDataEnvironmentUpdateListener} that can be used to track
   * {@link Environment} updates.
   */
  ConfigDataEnvironment(ConfigurableBootstrapContext bootstrap, ConfigurableEnvironment environment,
          ResourceLoader resourceLoader, Collection<String> additionalProfiles,
          @Nullable ConfigDataEnvironmentUpdateListener environmentUpdateListener) {
    Binder binder = Binder.get(environment);
    this.environment = environment;
    this.bootstrapContext = bootstrap;
    this.additionalProfiles = additionalProfiles;
    this.notFoundAction = binder.bind(ON_NOT_FOUND_PROPERTY, ConfigDataNotFoundAction.class)
            .orRequired(ConfigDataNotFoundAction.FAIL);
    this.resolvers = createConfigDataLocationResolvers(bootstrap, binder, resourceLoader);
    this.environmentUpdateListener = Optional.ofNullable(environmentUpdateListener).orElse(
            ConfigDataEnvironmentUpdateListener.NONE
    );
    this.loaders = new ConfigDataLoaders(bootstrap, resourceLoader.getClassLoader());
    this.contributors = createContributors(binder);
  }

  protected ConfigDataLocationResolvers createConfigDataLocationResolvers(ConfigurableBootstrapContext bootstrapContext, Binder binder, ResourceLoader resourceLoader) {
    return new ConfigDataLocationResolvers(bootstrapContext, binder, resourceLoader);
  }

  private ConfigDataEnvironmentContributors createContributors(Binder binder) {
    if (traceEnabled) {
      logger.trace("Building config data environment contributors");
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
          logger.trace("Creating wrapped config data contributor for '{}'", propertySource.getName());
        }
        contributors.add(ConfigDataEnvironmentContributor.ofExisting(propertySource, environment.getConversionService()));
      }
    }
    contributors.addAll(getInitialImportContributors(binder));
    if (defaultPropertySource != null) {
      if (traceEnabled) {
        logger.trace("Creating wrapped config data contributor for default property source");
      }
      contributors.add(ConfigDataEnvironmentContributor.ofExisting(defaultPropertySource, environment.getConversionService()));
    }
    return createContributors(contributors);
  }

  protected ConfigDataEnvironmentContributors createContributors(List<ConfigDataEnvironmentContributor> contributors) {
    return new ConfigDataEnvironmentContributors(bootstrapContext, contributors, environment.getConversionService());
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

  private void addInitialImportContributors(List<ConfigDataEnvironmentContributor> initialContributors, ConfigDataLocation[] locations) {
    for (int i = locations.length - 1; i >= 0; i--) {
      if (ConfigDataLocation.isNotEmpty(locations[i])) {
        initialContributors.add(createInitialImportContributor(locations[i]));
      }
    }
  }

  private ConfigDataEnvironmentContributor createInitialImportContributor(ConfigDataLocation location) {
    if (traceEnabled)
      logger.trace("Adding initial config data import from location '{}'", location);
    return ConfigDataEnvironmentContributor.ofInitialImport(location, environment.getConversionService());
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
            contributors.getBinder(null, ConfigDataEnvironmentContributors.BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE));
    contributors = processWithoutProfiles(contributors, importer, activationContext);
    activationContext = withProfiles(contributors, activationContext);
    contributors = processWithProfiles(contributors, importer, activationContext);
    applyToEnvironment(contributors, activationContext,
            importer.getLoadedLocations(), importer.getOptionalLocations());
  }

  private ConfigDataEnvironmentContributors processInitial(ConfigDataEnvironmentContributors contributors, ConfigDataImporter importer) {
    if (traceEnabled)
      logger.trace("Processing initial config data environment contributors without activation context");
    contributors = contributors.withProcessedImports(importer, null);
    registerBootstrapBinder(contributors, null, DENY_INACTIVE_BINDING);
    return contributors;
  }

  private ConfigDataActivationContext createActivationContext(Binder initialBinder) {
    if (traceEnabled)
      logger.trace("Creating config data activation context from initial contributions");
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
          ConfigDataEnvironmentContributors contributors, ConfigDataImporter importer, ConfigDataActivationContext activationContext) {
    if (traceEnabled)
      logger.trace("Processing config data environment contributors with initial activation context");
    contributors = contributors.withProcessedImports(importer, activationContext);
    registerBootstrapBinder(contributors, activationContext, DENY_INACTIVE_BINDING);
    return contributors;
  }

  private ConfigDataActivationContext withProfiles(ConfigDataEnvironmentContributors contributors, ConfigDataActivationContext activationContext) {
    if (traceEnabled)
      logger.trace("Deducing profiles from current config data environment contributors");
    Binder binder = contributors.getBinder(activationContext,
            contributor -> !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES),
            ConfigDataEnvironmentContributors.BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
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

  private Collection<? extends String> getIncludedProfiles(ConfigDataEnvironmentContributors contributors, ConfigDataActivationContext activationContext) {
    var placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            contributors, activationContext, null, true, environment.getConversionService());
    LinkedHashSet<String> result = new LinkedHashSet<>();
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      ConfigurationPropertySource source = contributor.configurationPropertySource;
      if (source != null && !contributor.hasConfigDataOption(ConfigData.Option.IGNORE_PROFILES)) {
        new Binder(Collections.singleton(source), placeholdersResolver)
                .bind(Profiles.INCLUDE_PROFILES, STRING_LIST)
                .ifBound(includes -> {
                  if (!contributor.isActive(activationContext)) {
                    throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES);
                    throwIfPropertyFound(contributor, Profiles.INCLUDE_PROFILES.append("[0]"));
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
      logger.trace("Processing config data environment contributors with profile activation context");
    contributors = contributors.withProcessedImports(importer, activationContext);
    registerBootstrapBinder(contributors, activationContext, ALLOW_INACTIVE_BINDING);
    return contributors;
  }

  private void registerBootstrapBinder(ConfigDataEnvironmentContributors contributors,
          @Nullable ConfigDataActivationContext activationContext, ConfigDataEnvironmentContributors.BinderOption... binderOptions) {
    bootstrapContext.register(Binder.class,
            InstanceSupplier.from(() -> contributors.getBinder(activationContext, binderOptions))
                    .withScope(Scope.PROTOTYPE)
    );
  }

  private void applyToEnvironment(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations, Set<ConfigDataLocation> optionalLocations) {
    checkForInvalidProperties(contributors);
    checkMandatoryLocations(contributors, activationContext, loadedLocations, optionalLocations);
    PropertySources propertySources = environment.getPropertySources();
    applyContributor(contributors, activationContext, propertySources);
    DefaultPropertiesPropertySource.moveToEnd(propertySources);
    Profiles profiles = activationContext.profiles;
    if (profiles != null) {
      if (traceEnabled) {
        logger.trace("Setting default profiles: {}", profiles.getDefault());
      }
      environment.setDefaultProfiles(StringUtils.toStringArray(profiles.getDefault()));
      if (traceEnabled) {
        logger.trace("Setting active profiles: {}", profiles.getActive());
      }
      environment.setActiveProfiles(StringUtils.toStringArray(profiles.getActive()));
      environmentUpdateListener.onSetProfiles(profiles);
    }
    else {
      logger.info("profiles not found");
    }
  }

  private void applyContributor(ConfigDataEnvironmentContributors contributors,
          ConfigDataActivationContext activationContext, PropertySources propertySources) {
    boolean traceEnabled = this.traceEnabled;
    if (traceEnabled)
      logger.trace("Applying config data environment contributions");
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      PropertySource<?> propertySource = contributor.propertySource;
      if (contributor.kind == ConfigDataEnvironmentContributor.Kind.BOUND_IMPORT && propertySource != null) {
        if (!contributor.isActive(activationContext)) {
          if (traceEnabled)
            logger.trace("Skipping inactive property source '{}'", propertySource.getName());
        }
        else {
          if (traceEnabled)
            logger.trace("Adding imported property source '{}'", propertySource.getName());
          propertySources.addLast(propertySource);
          ConfigDataLocation location = contributor.location;
          Assert.state(location != null, "location is required");
          environmentUpdateListener.onPropertySourceAdded(propertySource, location, contributor.resource);
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
          ConfigDataActivationContext activationContext, Set<ConfigDataLocation> loadedLocations, Set<ConfigDataLocation> optionalLocations) {
    Set<ConfigDataLocation> mandatoryLocations = new LinkedHashSet<>();
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      if (contributor.isActive(activationContext)) {
        mandatoryLocations.addAll(getMandatoryImports(contributor));
      }
    }
    for (ConfigDataEnvironmentContributor contributor : contributors) {
      if (contributor.location != null) {
        mandatoryLocations.remove(contributor.location);
      }
    }
    mandatoryLocations.removeAll(loadedLocations);
    mandatoryLocations.removeAll(optionalLocations);
    if (!mandatoryLocations.isEmpty()) {
      for (ConfigDataLocation mandatoryLocation : mandatoryLocations) {
        notFoundAction.handle(logger, new ConfigDataLocationNotFoundException(mandatoryLocation));
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
