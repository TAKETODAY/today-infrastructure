/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.gradle.tasks.buildinfo;

import org.gradle.api.Project;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;

import cn.taketoday.util.function.SingletonSupplier;

/**
 * The properties that are written into the {@code build-info.properties} file.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@SuppressWarnings("serial")
public abstract class BuildInfoProperties implements Serializable {

  private final SetProperty<String> excludes;

  private final Supplier<String> creationTime = SingletonSupplier.from(new CurrentIsoInstantSupplier());

  @Inject
  public BuildInfoProperties(Project project, SetProperty<String> excludes) {
    this.excludes = excludes;
    getGroup().convention(project.provider(() -> project.getGroup().toString()));
    getVersion().convention(project.provider(() -> project.getVersion().toString()));
    getArtifact().convention(project.provider(() -> project.findProperty("archivesBaseName")).map(Object::toString));
    getName().convention(project.provider(project::getName));
  }

  /**
   * Returns the {@code build.group} property. Defaults to the {@link Project#getGroup()
   * Project's group}.
   *
   * @return the group property
   */
  @Internal
  public abstract Property<String> getGroup();

  /**
   * Returns the {@code build.artifact} property.
   *
   * @return the artifact property
   */
  @Internal
  public abstract Property<String> getArtifact();

  /**
   * Returns the {@code build.version} property. Defaults to the
   * {@link Project#getVersion() Project's version}.
   *
   * @return the version
   */
  @Internal
  public abstract Property<String> getVersion();

  /**
   * Returns the {@code build.name} property. Defaults to the {@link Project#getName()
   * Project's name}.
   *
   * @return the name
   */
  @Internal
  public abstract Property<String> getName();

  /**
   * Returns the {@code build.time} property.
   *
   * @return the time
   */
  @Internal
  public abstract Property<String> getTime();

  /**
   * Returns the additional properties that will be included. When written, the name of
   * each additional property is prefixed with {@code build.}.
   *
   * @return the additional properties
   */
  @Internal
  public abstract MapProperty<String, Object> getAdditional();

  @Input
  @Optional
  String getArtifactIfNotExcluded() {
    return getIfNotExcluded(getArtifact(), "artifact");
  }

  @Input
  @Optional
  String getGroupIfNotExcluded() {
    return getIfNotExcluded(getGroup(), "group");
  }

  @Input
  @Optional
  String getNameIfNotExcluded() {
    return getIfNotExcluded(getName(), "name");
  }

  @Input
  @Optional
  Instant getTimeIfNotExcluded() {
    String time = getIfNotExcluded(getTime(), "time", this.creationTime);
    return (time != null) ? Instant.parse(time) : null;
  }

  @Input
  @Optional
  String getVersionIfNotExcluded() {
    return getIfNotExcluded(getVersion(), "version");
  }

  @Input
  Map<String, String> getAdditionalIfNotExcluded() {
    return coerceToStringValues(applyExclusions(getAdditional().getOrElse(Collections.emptyMap())));
  }

  private <T> T getIfNotExcluded(Property<T> property, String name) {
    return getIfNotExcluded(property, name, () -> null);
  }

  private <T> T getIfNotExcluded(Property<T> property, String name, Supplier<T> defaultValue) {
    if (this.excludes.getOrElse(Collections.emptySet()).contains(name)) {
      return null;
    }
    return property.getOrElse(defaultValue.get());
  }

  private Map<String, String> coerceToStringValues(Map<String, Object> input) {
    Map<String, String> output = new HashMap<>();
    input.forEach((key, value) -> output.put(key, (value != null) ? value.toString() : null));
    return output;
  }

  private Map<String, Object> applyExclusions(Map<String, Object> input) {
    Map<String, Object> output = new HashMap<>();
    Set<String> exclusions = this.excludes.getOrElse(Collections.emptySet());
    input.forEach((key, value) -> output.put(key, (!exclusions.contains(key)) ? value : null));
    return output;
  }

  private static final class CurrentIsoInstantSupplier implements Supplier<String> {

    @Override
    public String get() {
      return DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    }

  }

}
