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

package cn.taketoday.build.shadow;

import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.query.ArtifactResolutionQuery;
import org.gradle.api.artifacts.result.ArtifactResolutionResult;
import org.gradle.api.artifacts.result.ComponentArtifactsResult;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedArtifactResult;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.file.FileTree;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.jvm.JvmLibrary;
import org.gradle.language.base.artifact.SourcesArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Gradle task to add source from shadowed jars into our own source jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ShadowSource extends DefaultTask {

  private final DirectoryProperty outputDirectory = getProject().getObjects().directoryProperty();

  private List<Configuration> configurations = new ArrayList<>();

  private final List<Relocation> relocations = new ArrayList<>();

  @Classpath
  @Optional
  public List<Configuration> getConfigurations() {
    return this.configurations;
  }

  public void setConfigurations(List<Configuration> configurations) {
    this.configurations = configurations;
  }

  @Nested
  public List<Relocation> getRelocations() {
    return this.relocations;
  }

  public void relocate(String pattern, String destination) {
    this.relocations.add(new Relocation(pattern, destination));
  }

  @OutputDirectory
  DirectoryProperty getOutputDirectory() {
    return this.outputDirectory;
  }

  @TaskAction
  void syncSourceJarFiles() {
    sync(getSourceJarFiles());
  }

  private List<File> getSourceJarFiles() {
    List<File> sourceJarFiles = new ArrayList<>();
    for (Configuration configuration : this.configurations) {
      ResolutionResult resolutionResult = configuration.getIncoming().getResolutionResult();
      resolutionResult.getRootComponent().get().getDependencies().forEach(dependency -> {
        Set<ComponentArtifactsResult> artifactsResults = resolveSourceArtifacts(dependency);
        for (ComponentArtifactsResult artifactResult : artifactsResults) {
          artifactResult.getArtifacts(SourcesArtifact.class).forEach(sourceArtifact -> {
            sourceJarFiles.add(((ResolvedArtifactResult) sourceArtifact).getFile());
          });
        }
      });
    }
    return Collections.unmodifiableList(sourceJarFiles);
  }

  private Set<ComponentArtifactsResult> resolveSourceArtifacts(DependencyResult dependency) {
    ModuleComponentSelector componentSelector = (ModuleComponentSelector) dependency.getRequested();
    ArtifactResolutionQuery query = getProject().getDependencies().createArtifactResolutionQuery()
            .forModule(componentSelector.getGroup(), componentSelector.getModule(), componentSelector.getVersion());
    return executeQuery(query).getResolvedComponents();
  }

  @SuppressWarnings("unchecked")
  private ArtifactResolutionResult executeQuery(ArtifactResolutionQuery query) {
    return query.withArtifacts(JvmLibrary.class, SourcesArtifact.class).execute();
  }

  private void sync(List<File> sourceJarFiles) {
    getProject().sync(spec -> {
      spec.into(this.outputDirectory);
      spec.eachFile(this::relocateFile);
      spec.filter(this::transformContent);
      spec.exclude("META-INF/**");
      spec.setIncludeEmptyDirs(false);
      sourceJarFiles.forEach(sourceJar -> spec.from(zipTree(sourceJar)));
    });
  }

  private void relocateFile(FileCopyDetails details) {
    String path = details.getPath();
    for (Relocation relocation : this.relocations) {
      path = relocation.relocatePath(path);
    }
    details.setPath(path);
  }

  private String transformContent(String content) {
    for (Relocation relocation : this.relocations) {
      content = relocation.transformContent(content);
    }
    return content;
  }

  private FileTree zipTree(File sourceJar) {
    return getProject().zipTree(sourceJar);
  }

  /**
   * A single relocation.
   */
  static class Relocation {

    private final String pattern;

    private final String pathPattern;

    private final String destination;

    private final String pathDestination;

    Relocation(String pattern, String destination) {
      this.pattern = pattern;
      this.pathPattern = pattern.replace('.', '/');
      this.destination = destination;
      this.pathDestination = destination.replace('.', '/');
    }

    @Input
    public String getPattern() {
      return this.pattern;
    }

    @Input
    public String getDestination() {
      return this.destination;
    }

    String relocatePath(String path) {
      return path.replace(this.pathPattern, this.pathDestination);
    }

    public String transformContent(String content) {
      return content.replaceAll("\\b" + this.pattern, this.destination);
    }

  }

}
