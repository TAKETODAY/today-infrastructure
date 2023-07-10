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

package cn.taketoday.gradle.plugin;

import org.gradle.api.Buildable;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.artifacts.PublishArtifactSet;
import org.gradle.api.artifacts.dsl.ArtifactHandler;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import cn.taketoday.gradle.tasks.bundling.InfraJar;
import cn.taketoday.gradle.tasks.bundling.InfraWar;

/**
 * A wrapper for a {@link PublishArtifactSet} that ensures that only a single artifact is
 * published, with a war file taking precedence over a jar file.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class SinglePublishedArtifact implements Buildable {

  private final Configuration configuration;

  private final ArtifactHandler handler;

  private PublishArtifact currentArtifact;

  SinglePublishedArtifact(Configuration configuration, ArtifactHandler handler) {
    this.configuration = configuration;
    this.handler = handler;
  }

  void addWarCandidate(TaskProvider<InfraWar> candidate) {
    add(candidate);
  }

  void addJarCandidate(TaskProvider<InfraJar> candidate) {
    if (this.currentArtifact == null) {
      add(candidate);
    }
  }

  private void add(TaskProvider<? extends Jar> artifact) {
    this.configuration.getArtifacts().remove(this.currentArtifact);
    this.currentArtifact = this.handler.add(this.configuration.getName(), artifact);
  }

  @Override
  public TaskDependency getBuildDependencies() {
    return this.configuration.getArtifacts().getBuildDependencies();
  }

}
