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
import org.apache.maven.shared.artifact.filter.collection.AbstractArtifactFeatureFilter;

/**
 * An {@link org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter
 * ArtifactsFilter} that filters by matching groupId.
 *
 * Preferred over the
 * {@link org.apache.maven.shared.artifact.filter.collection.GroupIdFilter} due to that
 * classes use of {@link String#startsWith} to match on prefix.
 *
 * @author Mark Ingram
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MatchingGroupIdFilter extends AbstractArtifactFeatureFilter {

  /**
   * Create a new instance with the CSV groupId values that should be excluded.
   *
   * @param exclude the group values to exclude
   */
  public MatchingGroupIdFilter(String exclude) {
    super("", exclude);
  }

  @Override
  protected String getArtifactFeature(Artifact artifact) {
    return artifact.getGroupId();
  }

}
