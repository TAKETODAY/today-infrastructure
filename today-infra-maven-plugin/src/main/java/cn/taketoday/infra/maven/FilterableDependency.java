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

import org.apache.maven.plugins.annotations.Parameter;

/**
 * A model for a dependency to include or exclude.
 *
 * @author Stephane Nicoll
 * @author David Turanski
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class FilterableDependency {

  /**
   * The groupId of the artifact to exclude.
   */
  @Parameter(required = true)
  private String groupId;

  /**
   * The artifactId of the artifact to exclude.
   */
  @Parameter(required = true)
  private String artifactId;

  /**
   * The classifier of the artifact to exclude.
   */
  @Parameter
  private String classifier;

  String getGroupId() {
    return this.groupId;
  }

  void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  String getArtifactId() {
    return this.artifactId;
  }

  void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  String getClassifier() {
    return this.classifier;
  }

  void setClassifier(String classifier) {
    this.classifier = classifier;
  }

}
