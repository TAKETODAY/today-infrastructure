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

package cn.taketoday.infra.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.taketoday.app.loader.tools.Libraries;
import cn.taketoday.app.loader.tools.Library;
import cn.taketoday.app.loader.tools.LibraryCallback;
import cn.taketoday.app.loader.tools.LibraryCoordinates;
import cn.taketoday.app.loader.tools.LibraryScope;

/**
 * {@link Libraries} backed by Maven {@link Artifact}s.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ArtifactsLibraries implements Libraries {

  private static final Map<String, LibraryScope> SCOPES = Map.of(
          Artifact.SCOPE_COMPILE, LibraryScope.COMPILE,
          Artifact.SCOPE_RUNTIME, LibraryScope.RUNTIME,
          Artifact.SCOPE_PROVIDED, LibraryScope.PROVIDED,
          Artifact.SCOPE_SYSTEM, LibraryScope.PROVIDED
  );

  private final Set<Artifact> artifacts;

  private final Set<Artifact> includedArtifacts;

  private final Collection<MavenProject> localProjects;

  private final Collection<Dependency> unpacks;

  private final Log log;

  /**
   * Creates a new {@code ArtifactsLibraries} from the given {@code artifacts}.
   *
   * @param artifacts the artifacts to represent as libraries
   * @param localProjects projects for which {@link Library#isLocal() local} libraries
   * should be created
   * @param unpacks artifacts that should be unpacked on launch
   * @param log the log
   */
  public ArtifactsLibraries(Set<Artifact> artifacts, Collection<MavenProject> localProjects,
          Collection<Dependency> unpacks, Log log) {
    this(artifacts, artifacts, localProjects, unpacks, log);
  }

  /**
   * Creates a new {@code ArtifactsLibraries} from the given {@code artifacts}.
   *
   * @param artifacts all artifacts that can be represented as libraries
   * @param includedArtifacts the actual artifacts to include in the fat jar
   * @param localProjects projects for which {@link Library#isLocal() local} libraries
   * should be created
   * @param unpacks artifacts that should be unpacked on launch
   * @param log the log
   */
  public ArtifactsLibraries(Set<Artifact> artifacts, Set<Artifact> includedArtifacts,
          Collection<MavenProject> localProjects, Collection<Dependency> unpacks, Log log) {
    this.artifacts = artifacts;
    this.includedArtifacts = includedArtifacts;
    this.localProjects = localProjects;
    this.unpacks = unpacks;
    this.log = log;
  }

  @Override
  public void doWithLibraries(LibraryCallback callback) throws IOException {
    Set<String> duplicates = getDuplicates(this.artifacts);
    for (Artifact artifact : this.artifacts) {
      String name = getFileName(artifact);
      File file = artifact.getFile();
      LibraryScope scope = SCOPES.get(artifact.getScope());
      if (scope == null || file == null) {
        continue;
      }
      if (duplicates.contains(name)) {
        this.log.debug("Duplicate found: " + name);
        name = artifact.getGroupId() + "-" + name;
        this.log.debug("Renamed to: " + name);
      }
      LibraryCoordinates coordinates = new ArtifactLibraryCoordinates(artifact);
      boolean unpackRequired = isUnpackRequired(artifact);
      boolean local = isLocal(artifact);
      boolean included = this.includedArtifacts.contains(artifact);
      callback.library(new Library(name, file, scope, coordinates, unpackRequired, local, included));
    }
  }

  private Set<String> getDuplicates(Set<Artifact> artifacts) {
    Set<String> duplicates = new HashSet<>();
    Set<String> seen = new HashSet<>();
    for (Artifact artifact : artifacts) {
      String fileName = getFileName(artifact);
      if (artifact.getFile() != null && !seen.add(fileName)) {
        duplicates.add(fileName);
      }
    }
    return duplicates;
  }

  private boolean isUnpackRequired(Artifact artifact) {
    if (this.unpacks != null) {
      for (Dependency unpack : this.unpacks) {
        if (artifact.getGroupId().equals(unpack.getGroupId())
                && artifact.getArtifactId().equals(unpack.getArtifactId())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isLocal(Artifact artifact) {
    for (MavenProject localProject : this.localProjects) {
      if (localProject.getArtifact().equals(artifact)) {
        return true;
      }
      for (Artifact attachedArtifact : localProject.getAttachedArtifacts()) {
        if (attachedArtifact.equals(artifact)) {
          return true;
        }
      }
    }
    return false;
  }

  private String getFileName(Artifact artifact) {
    StringBuilder sb = new StringBuilder();
    sb.append(artifact.getArtifactId()).append("-").append(artifact.getBaseVersion());
    String classifier = artifact.getClassifier();
    if (classifier != null) {
      sb.append("-").append(classifier);
    }
    sb.append(".").append(artifact.getArtifactHandler().getExtension());
    return sb.toString();
  }

  /**
   * {@link LibraryCoordinates} backed by a Maven {@link Artifact}.
   */
  private static class ArtifactLibraryCoordinates implements LibraryCoordinates {

    private final Artifact artifact;

    ArtifactLibraryCoordinates(Artifact artifact) {
      this.artifact = artifact;
    }

    @Override
    public String getGroupId() {
      return this.artifact.getGroupId();
    }

    @Override
    public String getArtifactId() {
      return this.artifact.getArtifactId();
    }

    @Override
    public String getVersion() {
      return this.artifact.getBaseVersion();
    }

    @Override
    public String toString() {
      return this.artifact.toString();
    }

  }

}
