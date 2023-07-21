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

package cn.taketoday.gradle.dsl;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

import java.io.File;

import cn.taketoday.gradle.tasks.buildinfo.BuildInfo;

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
  public void buildInfo(Action<BuildInfo> configurer) {
    TaskContainer tasks = this.project.getTasks();
    TaskProvider<BuildInfo> infraBuildInfo = tasks.register("infraBuildInfo", BuildInfo.class,
            this::configureBuildInfoTask);
    this.project.getPlugins().withType(JavaPlugin.class, (plugin) -> {
      tasks.named(JavaPlugin.CLASSES_TASK_NAME).configure((task) -> task.dependsOn(infraBuildInfo));
      infraBuildInfo.configure((buildInfo) -> buildInfo.getProperties()
              .getArtifact()
              .convention(this.project.provider(this::determineArtifactBaseName)));
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

  private String determineArtifactBaseName() {
    Jar artifactTask = findArtifactTask();
    return (artifactTask != null) ? artifactTask.getArchiveBaseName().get() : null;
  }

  private Jar findArtifactTask() {
    Jar artifactTask = (Jar) this.project.getTasks().findByName("infraWar");
    if (artifactTask != null) {
      return artifactTask;
    }
    return (Jar) this.project.getTasks().findByName("infraJar");
  }

}
