/*
 * Copyright 2012-present the original author or authors.
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

package infra.gradle.dsl;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import java.io.File;

import infra.gradle.tasks.buildinfo.BuildInfo;
import org.jspecify.annotations.Nullable;

/**
 * Entry point to Infra Gradle DSL.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class InfraApplicationExtension {

  private final Project project;

  private final Property<String> mainClass;

  /**
   * Creates a new {@code InfraApplicationExtension} that is associated with the given
   * {@code project}.
   *
   * @param project the project
   */
  public InfraApplicationExtension(Project project) {
    this.project = project;
    this.mainClass = project.getObjects().property(String.class);
  }

  /**
   * Returns the fully-qualified name of the application's main class.
   *
   * @return the fully-qualified name of the application's main class
   */
  public Property<String> getMainClass() {
    return this.mainClass;
  }

  /**
   * Creates a new {@link BuildInfo} task named {@code infraBuildInfo} and configures the
   * Java plugin's {@code classes} task to depend upon it.
   * <p>
   * By default, the task's destination dir will be a directory named {@code META-INF}
   * beneath the main source set's resources output directory, and the task's project
   * artifact will be the base name of the {@code infraWar} or {@code infraJar} task.
   */
  public void buildInfo() {
    buildInfo(null);
  }

  /**
   * Creates a new {@link BuildInfo} task named {@code infraBuildInfo} and configures the
   * Java plugin's {@code classes} task to depend upon it. The task is passed to the
   * given {@code configurer} for further configuration.
   * <p>
   * By default, the task's destination dir will be a directory named {@code META-INF}
   * beneath the main source set's resources output directory, and the task's project
   * artifact will be the base name of the {@code infraWar} or {@code infraJar} task.
   *
   * @param configurer the task configurer
   */
  public void buildInfo(@Nullable Action<BuildInfo> configurer) {
    TaskContainer tasks = project.getTasks();
    TaskProvider<BuildInfo> infraBuildInfo = tasks.register("infraBuildInfo", BuildInfo.class, this::configureBuildInfoTask);
    project.getPlugins().withType(JavaPlugin.class, plugin -> {
      tasks.named(JavaPlugin.CLASSES_TASK_NAME)
              .configure(task -> task.dependsOn(infraBuildInfo));
      infraBuildInfo.configure(buildInfo -> buildInfo.getProperties()
              .getArtifact()
              .convention(project.provider(this::determineArtifactBaseName)));
    });
    if (configurer != null) {
      infraBuildInfo.configure(configurer);
    }
  }

  private void configureBuildInfoTask(BuildInfo task) {
    task.setGroup(BasePlugin.BUILD_GROUP);
    task.setDescription("Generates a META-INF/build-info.properties file.");
    task.getDestinationDir()
            .convention(this.project.getLayout()
                    .dir(this.project.provider(() -> new File(determineMainSourceSetResourcesOutputDir(), "META-INF"))));
  }

  private File determineMainSourceSetResourcesOutputDir() {
    return this.project.getExtensions()
            .getByType(JavaPluginExtension.class)
            .getSourceSets()
            .getByName(SourceSet.MAIN_SOURCE_SET_NAME)
            .getOutput()
            .getResourcesDir();
  }

  @Nullable
  private String determineArtifactBaseName() {
    Jar artifactTask = findArtifactTask();
    return artifactTask != null ? artifactTask.getArchiveBaseName().get() : null;
  }

  @Nullable
  private Jar findArtifactTask() {
    Jar artifactTask = (Jar) this.project.getTasks().findByName("infraWar");
    if (artifactTask != null) {
      return artifactTask;
    }
    return (Jar) this.project.getTasks().findByName("infraJar");
  }

}
