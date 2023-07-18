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

package cn.taketoday.build.bom;

import org.apache.maven.artifact.versioning.VersionRange;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import cn.taketoday.build.bom.version.DependencyVersion;

/**
 * A collection of modules, Maven plugins, and Maven boms that are versioned and released
 * together.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Library {

  private final String name;

  private final LibraryVersion version;

  private final List<Group> groups;

  private final String versionProperty;

  private final List<ProhibitedVersion> prohibitedVersions;

  /**
   * Create a new {@code Library} with the given {@code name}, {@code version}, and
   * {@code groups}.
   *
   * @param name name of the library
   * @param version version of the library
   * @param groups groups in the library
   * @param prohibitedVersions version of the library that are prohibited
   */
  public Library(String name, LibraryVersion version, List<Group> groups,
          List<ProhibitedVersion> prohibitedVersions) {
    this.name = name;
    this.version = version;
    this.groups = groups;
    this.versionProperty = name.toLowerCase(Locale.ENGLISH).replace(' ', '-') + ".version";
    this.prohibitedVersions = prohibitedVersions;
  }

  public String getName() {
    return this.name;
  }

  public LibraryVersion getVersion() {
    return this.version;
  }

  public List<Group> getGroups() {
    return this.groups;
  }

  public String getVersionProperty() {
    return this.versionProperty;
  }

  public List<ProhibitedVersion> getProhibitedVersions() {
    return this.prohibitedVersions;
  }

  /**
   * A version or range of versions that are prohibited from being used in a bom.
   */
  public static class ProhibitedVersion {

    private final VersionRange range;

    private final List<String> startsWith;

    private final List<String> endsWith;

    private final List<String> contains;

    private final String reason;

    public ProhibitedVersion(VersionRange range, List<String> startsWith, List<String> endsWith,
            List<String> contains, String reason) {
      this.range = range;
      this.startsWith = startsWith;
      this.endsWith = endsWith;
      this.contains = contains;
      this.reason = reason;
    }

    public VersionRange getRange() {
      return this.range;
    }

    public List<String> getStartsWith() {
      return this.startsWith;
    }

    public List<String> getEndsWith() {
      return this.endsWith;
    }

    public List<String> getContains() {
      return this.contains;
    }

    public String getReason() {
      return this.reason;
    }

  }

  public static class LibraryVersion {

    private final DependencyVersion version;

    public LibraryVersion(DependencyVersion version) {
      this.version = version;
    }

    public DependencyVersion getVersion() {
      return this.version;
    }

  }

  /**
   * A collection of modules, Maven plugins, and Maven boms with the same group ID.
   */
  public static class Group {

    private final String id;

    private final List<Module> modules;

    private final List<String> plugins;

    private final List<String> boms;

    public Group(String id, List<Module> modules, List<String> plugins, List<String> boms) {
      this.id = id;
      this.modules = modules;
      this.plugins = plugins;
      this.boms = boms;
    }

    public String getId() {
      return this.id;
    }

    public List<Module> getModules() {
      return this.modules;
    }

    public List<String> getPlugins() {
      return this.plugins;
    }

    public List<String> getBoms() {
      return this.boms;
    }

  }

  /**
   * A module in a group.
   */
  public static class Module {

    private final String name;

    private final String type;

    private final String classifier;

    private final List<Exclusion> exclusions;

    public Module(String name) {
      this(name, Collections.emptyList());
    }

    public Module(String name, String type) {
      this(name, type, null, Collections.emptyList());
    }

    public Module(String name, List<Exclusion> exclusions) {
      this(name, null, null, exclusions);
    }

    public Module(String name, String type, String classifier, List<Exclusion> exclusions) {
      this.name = name;
      this.type = type;
      this.classifier = (classifier != null) ? classifier : "";
      this.exclusions = exclusions;
    }

    public String getName() {
      return this.name;
    }

    public String getClassifier() {
      return this.classifier;
    }

    public String getType() {
      return this.type;
    }

    public List<Exclusion> getExclusions() {
      return this.exclusions;
    }

  }

  /**
   * An exclusion of a dependency identified by its group ID and artifact ID.
   */
  public static class Exclusion {

    private final String groupId;

    private final String artifactId;

    public Exclusion(String groupId, String artifactId) {
      this.groupId = groupId;
      this.artifactId = artifactId;
    }

    public String getGroupId() {
      return this.groupId;
    }

    public String getArtifactId() {
      return this.artifactId;
    }

  }

}
