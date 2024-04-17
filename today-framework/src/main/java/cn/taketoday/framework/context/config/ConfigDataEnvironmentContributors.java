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

package cn.taketoday.framework.context.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import cn.taketoday.context.properties.bind.BindContext;
import cn.taketoday.context.properties.bind.BindHandler;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.core.conversion.ConversionService;
import cn.taketoday.framework.ConfigurableBootstrapContext;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributor.ImportPhase;
import cn.taketoday.framework.context.config.ConfigDataEnvironmentContributor.Kind;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.ObjectUtils;

/**
 * An immutable tree structure of {@link ConfigDataEnvironmentContributors} used to
 * process imports.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ConfigDataEnvironmentContributors implements Iterable<ConfigDataEnvironmentContributor> {

  private static final Logger log = LoggerFactory.getLogger(ConfigDataEnvironmentContributors.class);

  private static final Predicate<ConfigDataEnvironmentContributor> NO_CONTRIBUTOR_FILTER = (contributor) -> true;

  /**
   * the root contributor.
   */
  public final ConfigDataEnvironmentContributor root;

  public final ConfigurableBootstrapContext bootstrapContext;

  private final ConversionService conversionService;

  /**
   * Create a new {@link ConfigDataEnvironmentContributors} instance.
   *
   * @param bootstrapContext the bootstrap context
   * @param contributors the initial set of contributors
   */
  ConfigDataEnvironmentContributors(ConfigurableBootstrapContext bootstrapContext,
          List<ConfigDataEnvironmentContributor> contributors, ConversionService conversionService) {
    this.bootstrapContext = bootstrapContext;
    this.root = ConfigDataEnvironmentContributor.of(contributors, conversionService);
    this.conversionService = conversionService;
  }

  private ConfigDataEnvironmentContributors(ConfigurableBootstrapContext bootstrapContext,
          ConfigDataEnvironmentContributor root, ConversionService conversionService) {
    this.bootstrapContext = bootstrapContext;
    this.root = root;
    this.conversionService = conversionService;
  }

  /**
   * Processes imports from all active contributors and return a new
   * {@link ConfigDataEnvironmentContributors} instance.
   *
   * @param importer the importer used to import {@link ConfigData}
   * @param activationContext the current activation context or {@code null} if the
   * context has not yet been created
   * @return a {@link ConfigDataEnvironmentContributors} instance with all relevant
   * imports have been processed
   */
  ConfigDataEnvironmentContributors withProcessedImports(
          ConfigDataImporter importer, @Nullable ConfigDataActivationContext activationContext) {
    ImportPhase importPhase = ImportPhase.get(activationContext);

    boolean traceEnabled = log.isTraceEnabled();
    if (traceEnabled) {
      log.trace("Processing imports for phase {}. {}", importPhase,
              activationContext != null ? activationContext : "no activation context");
    }
    ConfigDataEnvironmentContributors result = this;
    int processed = 0;
    while (true) {
      ConfigDataEnvironmentContributor contributor = getNextToProcess(result, activationContext, importPhase);
      if (contributor == null) {
        if (traceEnabled) {
          log.trace("Processed imports for of {} contributors", processed);
        }
        return result;
      }
      if (contributor.kind == Kind.UNBOUND_IMPORT) {
        ConfigDataEnvironmentContributor bound = contributor.withBoundProperties(result, activationContext);
        result = new ConfigDataEnvironmentContributors(bootstrapContext,
                result.root.withReplacement(contributor, bound), conversionService);
        continue;
      }
      var locationResolverContext = new ContributorConfigDataLocationResolverContext(
              result, contributor, activationContext);
      ConfigDataLoaderContext loaderContext = new ContributorDataLoaderContext(this);
      List<ConfigDataLocation> imports = contributor.getImports();

      if (traceEnabled) {
        log.trace("Processing imports {}", imports);
      }
      var imported = importer.resolveAndLoad(activationContext, locationResolverContext, loaderContext, imports);
      if (traceEnabled) {
        log.trace(getImportedMessage(imported.keySet()));
      }
      var contributorAndChildren = contributor.withChildren(importPhase, asContributors(imported));
      result = new ConfigDataEnvironmentContributors(bootstrapContext,
              result.root.withReplacement(contributor, contributorAndChildren), conversionService);
      processed++;
    }
  }

  private CharSequence getImportedMessage(Set<ConfigDataResolutionResult> results) {
    if (results.isEmpty()) {
      return "Nothing imported";
    }
    StringBuilder message = new StringBuilder();
    message.append("Imported ")
            .append(results.size())
            .append(" resource")
            .append((results.size() != 1) ? "s " : " ");
    message.append(results.stream()
            .map(ConfigDataResolutionResult::getResource)
            .collect(Collectors.toList()));
    return message;
  }

  @Nullable
  private ConfigDataEnvironmentContributor getNextToProcess(ConfigDataEnvironmentContributors contributors,
          @Nullable ConfigDataActivationContext activationContext, ImportPhase importPhase) {
    for (ConfigDataEnvironmentContributor contributor : contributors.root) {
      if (contributor.kind == Kind.UNBOUND_IMPORT
              || isActiveWithUnprocessedImports(activationContext, importPhase, contributor)) {
        return contributor;
      }
    }
    return null;
  }

  private boolean isActiveWithUnprocessedImports(@Nullable ConfigDataActivationContext activationContext,
          ImportPhase importPhase, ConfigDataEnvironmentContributor contributor) {
    return contributor.isActive(activationContext) && contributor.hasUnprocessedImports(importPhase);
  }

  private List<ConfigDataEnvironmentContributor> asContributors(Map<ConfigDataResolutionResult, ConfigData> imported) {
    List<ConfigDataEnvironmentContributor> contributors = new ArrayList<>(imported.size() * 5);

    for (Map.Entry<ConfigDataResolutionResult, ConfigData> entry : imported.entrySet()) {
      ConfigData data = entry.getValue();
      ConfigDataResolutionResult resolutionResult = entry.getKey();

      ConfigDataLocation location = resolutionResult.getLocation();
      ConfigDataResource resource = resolutionResult.getResource();
      boolean profileSpecific = resolutionResult.isProfileSpecific();
      if (data.getPropertySources().isEmpty()) {
        contributors.add(ConfigDataEnvironmentContributor.ofEmptyLocation(location, profileSpecific, conversionService));
      }
      else {
        for (int i = data.getPropertySources().size() - 1; i >= 0; i--) {
          contributors.add(ConfigDataEnvironmentContributor.ofUnboundImport(location,
                  resource, profileSpecific, data, i, conversionService));
        }
      }
    }
    return Collections.unmodifiableList(contributors);
  }

  /**
   * Return a {@link Binder} backed by the contributors.
   *
   * @param activationContext the activation context
   * @param options binder options to apply
   * @return a binder instance
   */
  Binder getBinder(@Nullable ConfigDataActivationContext activationContext, BinderOption... options) {
    return getBinder(activationContext, NO_CONTRIBUTOR_FILTER, options);
  }

  /**
   * Return a {@link Binder} backed by the contributors.
   *
   * @param activationContext the activation context
   * @param filter a filter used to limit the contributors
   * @param options binder options to apply
   * @return a binder instance
   */
  Binder getBinder(@Nullable ConfigDataActivationContext activationContext,
          Predicate<ConfigDataEnvironmentContributor> filter, BinderOption... options) {
    return getBinder(activationContext, filter, asBinderOptionsSet(options));
  }

  private Set<BinderOption> asBinderOptionsSet(BinderOption... options) {
    return ObjectUtils.isEmpty(options) ? EnumSet.noneOf(BinderOption.class)
            : EnumSet.copyOf(Arrays.asList(options));
  }

  private Binder getBinder(@Nullable ConfigDataActivationContext activationContext,
          Predicate<ConfigDataEnvironmentContributor> filter, Set<BinderOption> options) {
    boolean failOnInactiveSource = options.contains(BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE);
    Iterable<ConfigurationPropertySource> sources = () -> getBinderSources(
            filter.and((contributor) -> failOnInactiveSource || contributor.isActive(activationContext)));

    var placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            this.root, activationContext, null, failOnInactiveSource, conversionService);
    BindHandler bindHandler = !failOnInactiveSource ? null : new InactiveSourceChecker(activationContext);
    return new Binder(sources, placeholdersResolver, null, null, bindHandler);
  }

  private Iterator<ConfigurationPropertySource> getBinderSources(Predicate<ConfigDataEnvironmentContributor> filter) {
    return this.root.stream()
            .filter(this::hasConfigurationPropertySource)
            .filter(filter)
            .map(cdec -> cdec.configurationPropertySource)
            .iterator();
  }

  private boolean hasConfigurationPropertySource(ConfigDataEnvironmentContributor contributor) {
    return contributor.configurationPropertySource != null;
  }

  @Override
  public Iterator<ConfigDataEnvironmentContributor> iterator() {
    return this.root.iterator();
  }

  /**
   * {@link ConfigDataLocationResolverContext} for a contributor.
   */
  private static class ContributorDataLoaderContext implements ConfigDataLoaderContext {

    private final ConfigDataEnvironmentContributors contributors;

    ContributorDataLoaderContext(ConfigDataEnvironmentContributors contributors) {
      this.contributors = contributors;
    }

    @Override
    public ConfigurableBootstrapContext getBootstrapContext() {
      return this.contributors.bootstrapContext;
    }

  }

  /**
   * {@link ConfigDataLocationResolverContext} for a contributor.
   */
  private static class ContributorConfigDataLocationResolverContext implements ConfigDataLocationResolverContext {

    private final ConfigDataEnvironmentContributors contributors;

    private final ConfigDataEnvironmentContributor contributor;

    @Nullable
    private final ConfigDataActivationContext activationContext;

    @Nullable
    private volatile Binder binder;

    ContributorConfigDataLocationResolverContext(ConfigDataEnvironmentContributors contributors,
            ConfigDataEnvironmentContributor contributor, @Nullable ConfigDataActivationContext activationContext) {
      this.contributors = contributors;
      this.contributor = contributor;
      this.activationContext = activationContext;
    }

    @Override
    public Binder getBinder() {
      Binder binder = this.binder;
      if (binder == null) {
        binder = this.contributors.getBinder(this.activationContext);
        this.binder = binder;
      }
      return binder;
    }

    @Override
    public ConfigDataResource getParent() {
      return this.contributor.resource;
    }

    @Override
    public ConfigurableBootstrapContext getBootstrapContext() {
      return this.contributors.bootstrapContext;
    }

  }

  private class InactiveSourceChecker implements BindHandler {

    @Nullable
    private final ConfigDataActivationContext activationContext;

    InactiveSourceChecker(@Nullable ConfigDataActivationContext activationContext) {
      this.activationContext = activationContext;
    }

    @Override
    public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
      for (ConfigDataEnvironmentContributor contributor : ConfigDataEnvironmentContributors.this) {
        if (!contributor.isActive(activationContext)) {
          InactiveConfigDataAccessException.throwIfPropertyFound(contributor, name);
        }
      }
      return result;
    }

  }

  /**
   * Binder options that can be used with
   * {@link ConfigDataEnvironmentContributors#getBinder(ConfigDataActivationContext, BinderOption...)}.
   */
  enum BinderOption {

    /**
     * Throw an exception if an inactive contributor contains a bound value.
     */
    FAIL_ON_BIND_TO_INACTIVE_SOURCE

  }

}
