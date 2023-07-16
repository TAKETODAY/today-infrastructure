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

package cn.taketoday.buildpack.platform.build;

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.StringUtils;

/**
 * A set of buildpack coordinates that uniquely identifies a buildpack.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see <a href=
 * "https://github.com/buildpacks/spec/blob/main/platform.md#ordertoml-toml">Platform
 * Interface Specification</a>
 * @since 4.0
 */
final class BuildpackCoordinates {

  private final String id;

  private final String version;

  private BuildpackCoordinates(String id, String version) {
    Assert.hasText(id, "ID must not be empty");
    this.id = id;
    this.version = version;
  }

  String getId() {
    return this.id;
  }

  /**
   * Return the buildpack ID with all "/" replaced by "_".
   *
   * @return the ID
   */
  String getSanitizedId() {
    return this.id.replace("/", "_");
  }

  String getVersion() {
    return this.version;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    BuildpackCoordinates other = (BuildpackCoordinates) obj;
    return this.id.equals(other.id) && ObjectUtils.nullSafeEquals(this.version, other.version);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + this.id.hashCode();
    result = prime * result + ObjectUtils.nullSafeHashCode(this.version);
    return result;
  }

  @Override
  public String toString() {
    return this.id + ((StringUtils.hasText(this.version)) ? "@" + this.version : "");
  }

  /**
   * Create {@link BuildpackCoordinates} from a <a href=
   * "https://github.com/buildpacks/spec/blob/main/buildpack.md#buildpacktoml-toml">{@code buildpack.toml}</a>
   * file.
   *
   * @param inputStream an input stream containing {@code buildpack.toml} content
   * @param path the path to the buildpack containing the {@code buildpack.toml} file
   * @return a new {@link BuildpackCoordinates} instance
   * @throws IOException on IO error
   */
  static BuildpackCoordinates fromToml(InputStream inputStream, Path path) throws IOException {
    return fromToml(Toml.parse(inputStream), path);
  }

  private static BuildpackCoordinates fromToml(TomlParseResult toml, Path path) {
    Assert.isTrue(!toml.isEmpty(),
            () -> "Buildpack descriptor 'buildpack.toml' is required in buildpack '" + path + "'");
    Assert.hasText(toml.getString("buildpack.id"),
            () -> "Buildpack descriptor must contain ID in buildpack '" + path + "'");
    Assert.hasText(toml.getString("buildpack.version"),
            () -> "Buildpack descriptor must contain version in buildpack '" + path + "'");
    Assert.isTrue(toml.contains("stacks") || toml.contains("order"),
            () -> "Buildpack descriptor must contain either 'stacks' or 'order' in buildpack '" + path + "'");
    Assert.isTrue(!(toml.contains("stacks") && toml.contains("order")),
            () -> "Buildpack descriptor must not contain both 'stacks' and 'order' in buildpack '" + path + "'");
    return new BuildpackCoordinates(toml.getString("buildpack.id"), toml.getString("buildpack.version"));
  }

  /**
   * Create {@link BuildpackCoordinates} by extracting values from
   * {@link BuildpackMetadata}.
   *
   * @param buildpackMetadata the buildpack metadata
   * @return a new {@link BuildpackCoordinates} instance
   */
  static BuildpackCoordinates fromBuildpackMetadata(BuildpackMetadata buildpackMetadata) {
    Assert.notNull(buildpackMetadata, "BuildpackMetadata must not be null");
    return new BuildpackCoordinates(buildpackMetadata.getId(), buildpackMetadata.getVersion());
  }

  /**
   * Create {@link BuildpackCoordinates} from an ID and version.
   *
   * @param id the buildpack ID
   * @param version the buildpack version
   * @return a new {@link BuildpackCoordinates} instance
   */
  static BuildpackCoordinates of(String id, String version) {
    return new BuildpackCoordinates(id, version);
  }

}
