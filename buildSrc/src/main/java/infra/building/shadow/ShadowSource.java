/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.building.shadow;

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
import org.gradle.api.file.FileSystemOperations;
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

import javax.inject.Inject;

/**
 * Gradle task to add source from shadowed jars into our own source jars.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ShadowSource extends DefaultTask {

  private final DirectoryProperty outputDirectory = getProject().getObjects().directoryProperty();

  private List<File> sourceJarFiles = new ArrayList<>();

  private List<FileTree> sourceTrees = new ArrayList<>();

  @Inject
  protected FileSystemOperations getFileSystemOperations() {
    throw new UnsupportedOperationException();
  }

  private final List<Relocation> relocations = new ArrayList<>();

  public void setConfigurations(List<Configuration> configurations) {
    List<File> sourceJarFiles = new ArrayList<>();
    for (Configuration configuration : configurations) {
      ResolutionResult resolutionResult = configuration.getIncoming().getResolutionResult();
      resolutionResult.getRootComponent().get().getDependencies().forEach(dependency -> {
        Set<ComponentArtifactsResult> artifactsResults = resolveSourceArtifacts(dependency);
        for (ComponentArtifactsResult artifactResult : artifactsResults) {
          artifactResult.getArtifacts(SourcesArtifact.class).forEach(sourceArtifact ->
                  sourceJarFiles.add(((ResolvedArtifactResult) sourceArtifact).getFile()));
        }
      });
    }
    this.sourceJarFiles = Collections.unmodifiableList(sourceJarFiles);
    this.sourceTrees = sourceJarFiles.stream().map(this::zipTree).toList();
  }

  @Classpath
  @Optional
  public List<File> getSourceJarFiles() {
    return this.sourceJarFiles;
  }

  @Nested
  public List<Relocation> getRelocations() {
    return this.relocations;
  }

  public void relocate(String pattern, String destination) {
    this.relocations.add(new Relocation(pattern, destination));
  }

  @OutputDirectory
  public DirectoryProperty getOutputDirectory() {
    return this.outputDirectory;
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

  @TaskAction
  void syncSourceJarFiles() {
    sync();
  }

  private void sync() {
    getFileSystemOperations().sync(spec -> {
      spec.into(this.outputDirectory);
      spec.eachFile(this::relocateFile);
      spec.filter(this::transformContent);
      spec.exclude("META-INF/**");
      spec.setIncludeEmptyDirs(false);
      this.sourceTrees.forEach(spec::from);
    });
  }

  private FileTree zipTree(File sourceJar) {
    return getProject().zipTree(sourceJar);
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
