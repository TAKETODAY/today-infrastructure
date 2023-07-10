/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ComponentIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.internal.component.external.model.ModuleComponentArtifactIdentifier;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import cn.taketoday.app.loader.tools.LibraryCoordinates;

/**
 * Maps from {@link File} to {@link ComponentArtifactIdentifier}.
 *
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Phillip Webb
 * @author Paddy Drury
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
class ResolvedDependencies {

  private final Map<String, LibraryCoordinates> projectCoordinatesByPath;

  private final ListProperty<ComponentArtifactIdentifier> artifactIds;

  private final ListProperty<File> artifactFiles;

  ResolvedDependencies(Project project) {
    this.artifactIds = project.getObjects().listProperty(ComponentArtifactIdentifier.class);
    this.artifactFiles = project.getObjects().listProperty(File.class);
    this.projectCoordinatesByPath = projectCoordinatesByPath(project);
  }

  private static Map<String, LibraryCoordinates> projectCoordinatesByPath(Project project) {
    return project.getRootProject()
            .getAllprojects()
            .stream()
            .collect(Collectors.toMap(Project::getPath, ResolvedDependencies::libraryCoordinates));
  }

  private static LibraryCoordinates libraryCoordinates(Project project) {
    return LibraryCoordinates.of(Objects.toString(project.getGroup()), project.getName(),
            Objects.toString(project.getVersion()));
  }

  @Input
  ListProperty<ComponentArtifactIdentifier> getArtifactIds() {
    return this.artifactIds;
  }

  @Classpath
  ListProperty<File> getArtifactFiles() {
    return this.artifactFiles;
  }

  void resolvedArtifacts(Provider<Set<ResolvedArtifactResult>> resolvedArtifacts) {
    this.artifactFiles.addAll(resolvedArtifacts
            .map((artifacts) -> artifacts.stream().map(ResolvedArtifactResult::getFile).collect(Collectors.toList())));
    this.artifactIds.addAll(resolvedArtifacts
            .map((artifacts) -> artifacts.stream().map(ResolvedArtifactResult::getId).collect(Collectors.toList())));
  }

  DependencyDescriptor find(File file) {
    ComponentArtifactIdentifier id = findArtifactIdentifier(file);
    if (id == null) {
      return null;
    }
    if (id instanceof ModuleComponentArtifactIdentifier moduleComponentId) {
      ModuleComponentIdentifier moduleId = moduleComponentId.getComponentIdentifier();
      return new DependencyDescriptor(
              LibraryCoordinates.of(moduleId.getGroup(), moduleId.getModule(), moduleId.getVersion()), false);
    }
    ComponentIdentifier componentIdentifier = id.getComponentIdentifier();
    if (componentIdentifier instanceof ProjectComponentIdentifier projectComponentId) {
      String projectPath = projectComponentId.getProjectPath();
      LibraryCoordinates projectCoordinates = this.projectCoordinatesByPath.get(projectPath);
      if (projectCoordinates != null) {
        return new DependencyDescriptor(projectCoordinates, true);
      }
    }
    return null;
  }

  private ComponentArtifactIdentifier findArtifactIdentifier(File file) {
    List<File> files = this.artifactFiles.get();
    for (int i = 0; i < files.size(); i++) {
      if (file.equals(files.get(i))) {
        return this.artifactIds.get().get(i);
      }
    }
    return null;
  }

  /**
   * Describes a dependency in a {@link ResolvedConfiguration}.
   */
  static final class DependencyDescriptor {

    private final LibraryCoordinates coordinates;

    private final boolean projectDependency;

    private DependencyDescriptor(LibraryCoordinates coordinates, boolean projectDependency) {
      this.coordinates = coordinates;
      this.projectDependency = projectDependency;
    }

    LibraryCoordinates getCoordinates() {
      return this.coordinates;
    }

    boolean isProjectDependency() {
      return this.projectDependency;
    }

  }

}
