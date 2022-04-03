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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.bind.PlaceholdersResolver;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.env.PropertySource;
import cn.taketoday.framework.context.config.ConfigData.Options;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;

/**
 * A single element that may directly or indirectly contribute configuration data to the
 * {@link Environment}. There are several different {@link Kind kinds} of contributor, all
 * are immutable and will be replaced with new versions as imports are processed.
 * <p>
 * Contributors may provide a set of imports that should be processed and ultimately
 * turned into children. There are two distinct import phases:
 * <ul>
 * <li>{@link ImportPhase#BEFORE_PROFILE_ACTIVATION Before} profiles have been
 * activated.</li>
 * <li>{@link ImportPhase#AFTER_PROFILE_ACTIVATION After} profiles have been
 * activated.</li>
 * </ul>
 * In each phase <em>all</em> imports will be resolved before they are loaded.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @since 4.0
 */
class ConfigDataEnvironmentContributor implements Iterable<ConfigDataEnvironmentContributor> {

  private static final Options EMPTY_LOCATION_OPTIONS = Options.of(ConfigData.Option.IGNORE_IMPORTS);

  @Nullable
  private final ConfigDataLocation location;

  @Nullable
  private final ConfigDataResource resource;

  private final boolean fromProfileSpecificImport;

  @Nullable
  private final PropertySource<?> propertySource;

  @Nullable
  private final ConfigurationPropertySource configurationPropertySource;

  @Nullable
  private final ConfigDataProperties properties;

  private final Options configDataOptions;

  private final Map<ImportPhase, List<ConfigDataEnvironmentContributor>> children;

  private final Kind kind;

  /**
   * Create a new {@link ConfigDataEnvironmentContributor} instance.
   *
   * @param kind the contributor kind
   * @param location the location of this contributor
   * @param resource the resource that contributed the data or {@code null}
   * @param fromProfileSpecificImport if the contributor is from a profile specific
   * import
   * @param propertySource the property source for the data or {@code null}
   * @param configurationPropertySource the configuration property source for the data
   * or {@code null}
   * @param properties the config data properties or {@code null}
   * @param configDataOptions any config data options that should apply
   * @param children the children of this contributor at each {@link ImportPhase}
   */
  ConfigDataEnvironmentContributor(
          Kind kind,
          @Nullable ConfigDataLocation location,
          @Nullable ConfigDataResource resource,
          boolean fromProfileSpecificImport,
          @Nullable PropertySource<?> propertySource,
          @Nullable ConfigurationPropertySource configurationPropertySource,
          @Nullable ConfigDataProperties properties,
          @Nullable Options configDataOptions,
          @Nullable Map<ImportPhase, List<ConfigDataEnvironmentContributor>> children) {
    this.kind = kind;
    this.location = location;
    this.resource = resource;
    this.fromProfileSpecificImport = fromProfileSpecificImport;
    this.properties = properties;
    this.propertySource = propertySource;
    this.configurationPropertySource = configurationPropertySource;
    this.configDataOptions = (configDataOptions != null) ? configDataOptions : Options.NONE;
    this.children = (children != null) ? children : Collections.emptyMap();
  }

  /**
   * Return the contributor kind.
   *
   * @return the kind of contributor
   */
  Kind getKind() {
    return this.kind;
  }

  @Nullable
  ConfigDataLocation getLocation() {
    return this.location;
  }

  /**
   * Return if this contributor is currently active.
   *
   * @param activationContext the activation context
   * @return if the contributor is active
   */
  boolean isActive(@Nullable ConfigDataActivationContext activationContext) {
    if (this.kind == Kind.UNBOUND_IMPORT) {
      return false;
    }
    return this.properties == null || this.properties.isActive(activationContext);
  }

  /**
   * Return the resource that contributed this instance.
   *
   * @return the resource or {@code null}
   */
  @Nullable
  ConfigDataResource getResource() {
    return this.resource;
  }

  /**
   * Return if the contributor is from a profile specific import.
   *
   * @return if the contributor is profile specific
   */
  boolean isFromProfileSpecificImport() {
    return this.fromProfileSpecificImport;
  }

  /**
   * Return the property source for this contributor.
   *
   * @return the property source or {@code null}
   */
  @Nullable
  PropertySource<?> getPropertySource() {
    return this.propertySource;
  }

  /**
   * Return the configuration property source for this contributor.
   *
   * @return the configuration property source or {@code null}
   */
  @Nullable
  ConfigurationPropertySource getConfigurationPropertySource() {
    return this.configurationPropertySource;
  }

  /**
   * Return if the contributor has a specific config data option.
   *
   * @param option the option to check
   * @return {@code true} if the option is present
   */
  boolean hasConfigDataOption(ConfigData.Option option) {
    return this.configDataOptions.contains(option);
  }

  ConfigDataEnvironmentContributor withoutConfigDataOption(ConfigData.Option option) {
    return new ConfigDataEnvironmentContributor(this.kind, this.location, this.resource,
            this.fromProfileSpecificImport, this.propertySource, this.configurationPropertySource, this.properties,
            this.configDataOptions.without(option), this.children);
  }

  /**
   * Return any imports requested by this contributor.
   *
   * @return the imports
   */
  List<ConfigDataLocation> getImports() {
    return (this.properties != null) ? this.properties.getImports() : Collections.emptyList();
  }

  /**
   * Return true if this contributor has imports that have not yet been processed in the
   * given phase.
   *
   * @param importPhase the import phase
   * @return if there are unprocessed imports
   */
  boolean hasUnprocessedImports(ImportPhase importPhase) {
    if (getImports().isEmpty()) {
      return false;
    }
    return !this.children.containsKey(importPhase);
  }

  /**
   * Return children of this contributor for the given phase.
   *
   * @param importPhase the import phase
   * @return a list of children
   */
  List<ConfigDataEnvironmentContributor> getChildren(ImportPhase importPhase) {
    return this.children.getOrDefault(importPhase, Collections.emptyList());
  }

  /**
   * Returns a {@link Stream} that traverses this contributor and all its children in
   * priority order.
   *
   * @return the stream
   */
  Stream<ConfigDataEnvironmentContributor> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  /**
   * Returns an {@link Iterator} that traverses this contributor and all its children in
   * priority order.
   *
   * @return the iterator
   * @see Iterable#iterator()
   */
  @Override
  public Iterator<ConfigDataEnvironmentContributor> iterator() {
    return new ContributorIterator();
  }

  /**
   * Create a new {@link ConfigDataEnvironmentContributor} with bound
   * {@link ConfigDataProperties}.
   *
   * @param contributors the contributors used for binding
   * @param activationContext the activation context
   * @return a new contributor instance
   */
  ConfigDataEnvironmentContributor withBoundProperties(
          Iterable<ConfigDataEnvironmentContributor> contributors,
          @Nullable ConfigDataActivationContext activationContext) {
    Iterable<ConfigurationPropertySource> sources = Collections.singleton(getConfigurationPropertySource());
    PlaceholdersResolver placeholdersResolver = new ConfigDataEnvironmentContributorPlaceholdersResolver(
            contributors, activationContext, this, true);
    Binder binder = new Binder(sources, placeholdersResolver, null, null, null);
    ConfigDataProperties properties = ConfigDataProperties.get(binder);
    if (properties != null && this.configDataOptions.contains(ConfigData.Option.IGNORE_IMPORTS)) {
      properties = properties.withoutImports();
    }
    return new ConfigDataEnvironmentContributor(Kind.BOUND_IMPORT, this.location, this.resource,
            this.fromProfileSpecificImport, this.propertySource, this.configurationPropertySource, properties,
            this.configDataOptions, null);
  }

  /**
   * Create a new {@link ConfigDataEnvironmentContributor} instance with a new set of
   * children for the given phase.
   *
   * @param importPhase the import phase
   * @param children the new children
   * @return a new contributor instance
   */
  ConfigDataEnvironmentContributor withChildren(
          ImportPhase importPhase, List<ConfigDataEnvironmentContributor> children) {
    var updatedChildren = new LinkedHashMap<>(this.children);
    updatedChildren.put(importPhase, children);
    if (importPhase == ImportPhase.AFTER_PROFILE_ACTIVATION) {
      moveProfileSpecific(updatedChildren);
    }
    return new ConfigDataEnvironmentContributor(this.kind, this.location, this.resource,
            this.fromProfileSpecificImport, this.propertySource, this.configurationPropertySource, this.properties,
            this.configDataOptions, updatedChildren);
  }

  private void moveProfileSpecific(Map<ImportPhase, List<ConfigDataEnvironmentContributor>> children) {
    var before = children.get(ImportPhase.BEFORE_PROFILE_ACTIVATION);
    if (hasAnyProfileSpecificChildren(before)) {
      var updatedAfter = new ArrayList<ConfigDataEnvironmentContributor>();
      var updatedBefore = new ArrayList<ConfigDataEnvironmentContributor>(before.size());
      for (ConfigDataEnvironmentContributor contributor : before) {
        updatedBefore.add(moveProfileSpecificChildren(contributor, updatedAfter));
      }
      updatedAfter.addAll(children.getOrDefault(ImportPhase.AFTER_PROFILE_ACTIVATION, Collections.emptyList()));
      children.put(ImportPhase.BEFORE_PROFILE_ACTIVATION, updatedBefore);
      children.put(ImportPhase.AFTER_PROFILE_ACTIVATION, updatedAfter);
    }
  }

  private ConfigDataEnvironmentContributor moveProfileSpecificChildren(
          ConfigDataEnvironmentContributor contributor, List<ConfigDataEnvironmentContributor> removed) {
    for (ImportPhase importPhase : ImportPhase.values()) {
      var children = contributor.getChildren(importPhase);
      var updatedChildren = new ArrayList<ConfigDataEnvironmentContributor>(children.size());
      for (ConfigDataEnvironmentContributor child : children) {
        if (child.hasConfigDataOption(ConfigData.Option.PROFILE_SPECIFIC)) {
          removed.add(child.withoutConfigDataOption(ConfigData.Option.PROFILE_SPECIFIC));
        }
        else {
          updatedChildren.add(child);
        }
      }
      contributor = contributor.withChildren(importPhase, updatedChildren);
    }
    return contributor;
  }

  private boolean hasAnyProfileSpecificChildren(List<ConfigDataEnvironmentContributor> contributors) {
    if (CollectionUtils.isNotEmpty(contributors)) {
      for (ConfigDataEnvironmentContributor contributor : contributors) {
        for (ImportPhase importPhase : ImportPhase.values()) {
          if (contributor.getChildren(importPhase)
                  .stream()
                  .anyMatch((child) -> child.hasConfigDataOption(ConfigData.Option.PROFILE_SPECIFIC))) {
            return true;
          }
        }
      }
    }
    return false;
  }

  /**
   * Create a new {@link ConfigDataEnvironmentContributor} instance where an existing
   * child is replaced.
   *
   * @param existing the existing node that should be replaced
   * @param replacement the replacement node that should be used instead
   * @return a new {@link ConfigDataEnvironmentContributor} instance
   */
  ConfigDataEnvironmentContributor withReplacement(
          ConfigDataEnvironmentContributor existing, ConfigDataEnvironmentContributor replacement) {
    if (this == existing) {
      return replacement;
    }

    var updatedChildren = new LinkedHashMap<ImportPhase, List<ConfigDataEnvironmentContributor>>(children.size());
    for (var entry : children.entrySet()) {
      ImportPhase importPhase = entry.getKey();
      List<ConfigDataEnvironmentContributor> contributors = entry.getValue();
      var updatedContributors = new ArrayList<ConfigDataEnvironmentContributor>(contributors.size());
      for (ConfigDataEnvironmentContributor contributor : contributors) {
        updatedContributors.add(contributor.withReplacement(existing, replacement));
      }
      updatedChildren.put(importPhase, Collections.unmodifiableList(updatedContributors));
    }

    return new ConfigDataEnvironmentContributor(this.kind, this.location, this.resource,
            this.fromProfileSpecificImport, this.propertySource, this.configurationPropertySource, this.properties,
            this.configDataOptions, updatedChildren);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    buildToString("", builder);
    return builder.toString();
  }

  private void buildToString(String prefix, StringBuilder builder) {
    builder.append(prefix);
    builder.append(this.kind);
    builder.append(" ");
    builder.append(this.location);
    builder.append(" ");
    builder.append(this.resource);
    builder.append(" ");
    builder.append(this.configDataOptions);
    builder.append("\n");
    for (ConfigDataEnvironmentContributor child : this.children.getOrDefault(ImportPhase.BEFORE_PROFILE_ACTIVATION,
            Collections.emptyList())) {
      child.buildToString(prefix + "    ", builder);
    }
    for (ConfigDataEnvironmentContributor child : this.children.getOrDefault(ImportPhase.AFTER_PROFILE_ACTIVATION,
            Collections.emptyList())) {
      child.buildToString(prefix + "    ", builder);
    }
  }

  /**
   * Factory method to create a {@link Kind#ROOT root} contributor.
   *
   * @param contributors the immediate children of the root
   * @return a new {@link ConfigDataEnvironmentContributor} instance
   */
  static ConfigDataEnvironmentContributor of(List<ConfigDataEnvironmentContributor> contributors) {
    var children = new LinkedHashMap<ImportPhase, List<ConfigDataEnvironmentContributor>>();
    children.put(ImportPhase.BEFORE_PROFILE_ACTIVATION, Collections.unmodifiableList(contributors));
    return new ConfigDataEnvironmentContributor(Kind.ROOT, null, null, false, null, null, null, null, children);
  }

  /**
   * Factory method to create a {@link Kind#INITIAL_IMPORT initial import} contributor.
   * This contributor is used to trigger initial imports of additional contributors. It
   * does not contribute any properties itself.
   *
   * @param initialImport the initial import location (with placeholders resolved)
   * @return a new {@link ConfigDataEnvironmentContributor} instance
   */
  static ConfigDataEnvironmentContributor ofInitialImport(ConfigDataLocation initialImport) {
    List<ConfigDataLocation> imports = Collections.singletonList(initialImport);
    ConfigDataProperties properties = new ConfigDataProperties(imports, null);
    return new ConfigDataEnvironmentContributor(Kind.INITIAL_IMPORT, null, null, false, null, null, properties,
            null, null);
  }

  /**
   * Factory method to create a contributor that wraps an {@link Kind#EXISTING existing}
   * property source. The contributor provides access to existing properties, but
   * doesn't actively import any additional contributors.
   *
   * @param propertySource the property source to wrap
   * @return a new {@link ConfigDataEnvironmentContributor} instance
   */
  static ConfigDataEnvironmentContributor ofExisting(PropertySource<?> propertySource) {
    return new ConfigDataEnvironmentContributor(Kind.EXISTING, null, null, false, propertySource,
            ConfigurationPropertySource.from(propertySource), null, null, null);
  }

  /**
   * Factory method to create an {@link Kind#UNBOUND_IMPORT unbound import} contributor.
   * This contributor has been actively imported from another contributor and may itself
   * import further contributors later.
   *
   * @param location the location of this contributor
   * @param resource the config data resource
   * @param profileSpecific if the contributor is from a profile specific import
   * @param configData the config data
   * @param propertySourceIndex the index of the property source that should be used
   * @return a new {@link ConfigDataEnvironmentContributor} instance
   */
  static ConfigDataEnvironmentContributor ofUnboundImport(
          @Nullable ConfigDataLocation location, @Nullable ConfigDataResource resource,
          boolean profileSpecific, ConfigData configData, int propertySourceIndex) {
    PropertySource<?> propertySource = configData.getPropertySources().get(propertySourceIndex);
    Options options = configData.getOptions(propertySource);
    ConfigurationPropertySource configurationPropertySource = ConfigurationPropertySource.from(propertySource);
    return new ConfigDataEnvironmentContributor(Kind.UNBOUND_IMPORT, location, resource, profileSpecific,
            propertySource, configurationPropertySource, null, options, null);
  }

  /**
   * Factory method to create an {@link Kind#EMPTY_LOCATION empty location} contributor.
   *
   * @param location the location of this contributor
   * @param profileSpecific if the contributor is from a profile specific import
   * @return a new {@link ConfigDataEnvironmentContributor} instance
   */
  static ConfigDataEnvironmentContributor ofEmptyLocation(ConfigDataLocation location, boolean profileSpecific) {
    return new ConfigDataEnvironmentContributor(Kind.EMPTY_LOCATION, location, null, profileSpecific, null, null,
            null, EMPTY_LOCATION_OPTIONS, null);
  }

  /**
   * The various kinds of contributor.
   */
  enum Kind {

    /**
     * A root contributor used contain the initial set of children.
     */
    ROOT,

    /**
     * An initial import that needs to be processed.
     */
    INITIAL_IMPORT,

    /**
     * An existing property source that contributes properties but no imports.
     */
    EXISTING,

    /**
     * A contributor with {@link ConfigData} imported from another contributor but not
     * yet bound.
     */
    UNBOUND_IMPORT,

    /**
     * A contributor with {@link ConfigData} imported from another contributor that
     * has been.
     */
    BOUND_IMPORT,

    /**
     * A valid location that contained nothing to load.
     */
    EMPTY_LOCATION

  }

  /**
   * Import phases that can be used when obtaining imports.
   */
  enum ImportPhase {

    /**
     * The phase before profiles have been activated.
     */
    BEFORE_PROFILE_ACTIVATION,

    /**
     * The phase after profiles have been activated.
     */
    AFTER_PROFILE_ACTIVATION;

    /**
     * Return the {@link ImportPhase} based on the given activation context.
     *
     * @param activationContext the activation context
     * @return the import phase
     */
    static ImportPhase get(@Nullable ConfigDataActivationContext activationContext) {
      if (activationContext != null && activationContext.getProfiles() != null) {
        return AFTER_PROFILE_ACTIVATION;
      }
      return BEFORE_PROFILE_ACTIVATION;
    }

  }

  /**
   * Iterator that traverses the contributor tree.
   */
  private final class ContributorIterator implements Iterator<ConfigDataEnvironmentContributor> {

    @Nullable
    private ImportPhase phase;

    private Iterator<ConfigDataEnvironmentContributor> children;

    private Iterator<ConfigDataEnvironmentContributor> current;

    @Nullable
    private ConfigDataEnvironmentContributor next;

    private ContributorIterator() {
      this.phase = ImportPhase.AFTER_PROFILE_ACTIVATION;
      this.children = getChildren(this.phase).iterator();
      this.current = Collections.emptyIterator();
    }

    @Override
    public boolean hasNext() {
      return fetchIfNecessary() != null;
    }

    @Override
    public ConfigDataEnvironmentContributor next() {
      ConfigDataEnvironmentContributor next = fetchIfNecessary();
      if (next == null) {
        throw new NoSuchElementException();
      }
      this.next = null;
      return next;
    }

    @Nullable
    private ConfigDataEnvironmentContributor fetchIfNecessary() {
      if (this.next != null) {
        return this.next;
      }
      if (this.current.hasNext()) {
        this.next = this.current.next();
        return this.next;
      }
      if (this.children.hasNext()) {
        this.current = this.children.next().iterator();
        return fetchIfNecessary();
      }
      if (this.phase == ImportPhase.AFTER_PROFILE_ACTIVATION) {
        this.phase = ImportPhase.BEFORE_PROFILE_ACTIVATION;
        this.children = getChildren(this.phase).iterator();
        return fetchIfNecessary();
      }
      if (this.phase == ImportPhase.BEFORE_PROFILE_ACTIVATION) {
        this.phase = null;
        this.next = ConfigDataEnvironmentContributor.this;
        return this.next;
      }
      return null;
    }

  }

}
