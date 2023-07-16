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

import java.io.IOException;

import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.io.IOConsumer;

import cn.taketoday.lang.Assert;

/**
 * A {@link Buildpack} that references a buildpack contained in the builder.
 *
 * The buildpack reference must contain a buildpack ID (for example,
 * {@code "example/buildpack"}) or a buildpack ID and version (for example,
 * {@code "example/buildpack@1.0.0"}). The reference can optionally contain a prefix
 * {@code urn:cnb:builder:} to unambiguously identify it as a builder buildpack reference.
 * If a version is not provided, the reference will match any version of a buildpack with
 * the same ID as the reference.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class BuilderBuildpack implements Buildpack {

  private static final String PREFIX = "urn:cnb:builder:";

  private final BuildpackCoordinates coordinates;

  BuilderBuildpack(BuildpackMetadata buildpackMetadata) {
    this.coordinates = BuildpackCoordinates.fromBuildpackMetadata(buildpackMetadata);
  }

  @Override
  public BuildpackCoordinates getCoordinates() {
    return this.coordinates;
  }

  @Override
  public void apply(IOConsumer<Layer> layers) throws IOException {
  }

  /**
   * A {@link BuildpackResolver} compatible method to resolve builder buildpacks.
   *
   * @param context the resolver context
   * @param reference the buildpack reference
   * @return the resolved {@link Buildpack} or {@code null}
   */
  static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
    boolean unambiguous = reference.hasPrefix(PREFIX);
    BuilderReference builderReference = BuilderReference
            .of(unambiguous ? reference.getSubReference(PREFIX) : reference.toString());
    BuildpackMetadata buildpackMetadata = findBuildpackMetadata(context, builderReference);
    if (unambiguous) {
      Assert.isTrue(buildpackMetadata != null, () -> "Buildpack '" + reference + "' not found in builder");
    }
    return (buildpackMetadata != null) ? new BuilderBuildpack(buildpackMetadata) : null;
  }

  private static BuildpackMetadata findBuildpackMetadata(BuildpackResolverContext context,
          BuilderReference builderReference) {
    for (BuildpackMetadata candidate : context.getBuildpackMetadata()) {
      if (builderReference.matches(candidate)) {
        return candidate;
      }
    }
    return null;
  }

  /**
   * A reference to a buildpack builder.
   */
  static class BuilderReference {

    private final String id;

    private final String version;

    BuilderReference(String id, String version) {
      this.id = id;
      this.version = version;
    }

    @Override
    public String toString() {
      return (this.version != null) ? this.id + "@" + this.version : this.id;
    }

    boolean matches(BuildpackMetadata candidate) {
      return this.id.equals(candidate.getId())
              && (this.version == null || this.version.equals(candidate.getVersion()));
    }

    static BuilderReference of(String value) {
      if (value.contains("@")) {
        String[] parts = value.split("@");
        return new BuilderReference(parts[0], parts[1]);
      }
      return new BuilderReference(value, null);
    }

  }

}
