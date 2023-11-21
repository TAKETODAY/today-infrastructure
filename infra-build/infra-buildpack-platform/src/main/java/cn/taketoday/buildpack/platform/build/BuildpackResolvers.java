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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.taketoday.lang.Assert;
import cn.taketoday.util.CollectionUtils;

/**
 * All {@link BuildpackResolver} instances that can be used to resolve
 * {@link BuildpackReference BuildpackReferences}.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class BuildpackResolvers {

  private static final List<BuildpackResolver> resolvers = getResolvers();

  private BuildpackResolvers() {
  }

  private static List<BuildpackResolver> getResolvers() {
    return List.of(BuilderBuildpack::resolve,
            DirectoryBuildpack::resolve,
            TarGzipBuildpack::resolve,
            ImageBuildpack::resolve
    );
  }

  /**
   * Resolve a collection of {@link BuildpackReference BuildpackReferences} to a
   * {@link Buildpacks} instance.
   *
   * @param context the resolver context
   * @param references the references to resolve
   * @return a {@link Buildpacks} instance
   */
  static Buildpacks resolveAll(BuildpackResolverContext context, Collection<BuildpackReference> references) {
    Assert.notNull(context, "Context is required");
    if (CollectionUtils.isEmpty(references)) {
      return Buildpacks.EMPTY;
    }
    List<Buildpack> buildpacks = new ArrayList<>(references.size());
    for (BuildpackReference reference : references) {
      buildpacks.add(resolve(context, reference));
    }
    return Buildpacks.of(buildpacks);
  }

  private static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
    Assert.notNull(reference, "Reference is required");
    for (BuildpackResolver resolver : resolvers) {
      Buildpack buildpack = resolver.resolve(context, reference);
      if (buildpack != null) {
        return buildpack;
      }
    }
    throw new IllegalArgumentException("Invalid buildpack reference '" + reference + "'");
  }

}
