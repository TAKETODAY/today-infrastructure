/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.infra.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Base class for {@link ArtifactsFilter} based on a {@link FilterableDependency} list.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class DependencyFilter extends AbstractArtifactsFilter {

  private final List<? extends FilterableDependency> filters;

  /**
   * Create a new instance with the list of {@link FilterableDependency} instance(s) to
   * use.
   *
   * @param dependencies the source dependencies
   */
  public DependencyFilter(List<? extends FilterableDependency> dependencies) {
    this.filters = dependencies;
  }

  @Override
  public Set<Artifact> filter(Set<Artifact> artifacts) throws ArtifactFilterException {
    Set<Artifact> result = new HashSet<>();
    for (Artifact artifact : artifacts) {
      if (!filter(artifact)) {
        result.add(artifact);
      }
    }
    return result;
  }

  protected abstract boolean filter(Artifact artifact);

  /**
   * Check if the specified {@link Artifact} matches the
   * specified {@link FilterableDependency}. Returns
   * {@code true} if it should be excluded
   *
   * @param artifact the Maven {@link Artifact}
   * @param dependency the {@link FilterableDependency}
   * @return {@code true} if the artifact matches the dependency
   */
  protected final boolean equals(Artifact artifact, FilterableDependency dependency) {
    if (!dependency.getGroupId().equals(artifact.getGroupId())) {
      return false;
    }
    if (!dependency.getArtifactId().equals(artifact.getArtifactId())) {
      return false;
    }
    return (dependency.getClassifier() == null
            || artifact.getClassifier() != null && dependency.getClassifier().equals(artifact.getClassifier()));
  }

  protected final List<? extends FilterableDependency> getFilters() {
    return this.filters;
  }

}
