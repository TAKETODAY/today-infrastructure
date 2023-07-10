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

package cn.taketoday.build.maven;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.plugins.JavaLibraryPlugin;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A plugin to make a project's {@code deployment} publication available as a Maven
 * repository. The repository can be consumed by depending upon the project using the
 * {@code mavenRepository} configuration.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class MavenRepositoryPlugin implements Plugin<Project> {

  /**
   * Name of the {@code mavenRepository} configuration.
   */
  public static final String MAVEN_REPOSITORY_CONFIGURATION_NAME = "mavenRepository";

  /**
   * Name of the task that publishes to the project repository.
   */
  public static final String PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME = "publishMavenPublicationToProjectRepository";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(MavenPublishPlugin.class);
    PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
    File repositoryLocation = new File(project.getBuildDir(), "maven-repository");

    publishing.getRepositories().maven(mavenRepository -> {
      mavenRepository.setName("project");
      mavenRepository.setUrl(repositoryLocation.toURI());
    });

    project.getTasks()
            .matching(task -> task.getName().equals(PUBLISH_TO_PROJECT_REPOSITORY_TASK_NAME))
            .all(task -> setUpProjectRepository(project, task, repositoryLocation));

    project.getTasks()
            .matching(task -> task.getName().equals("publishPluginMavenPublicationToProjectRepository"))
            .all(task -> setUpProjectRepository(project, task, repositoryLocation));
  }

  private void setUpProjectRepository(Project project, Task publishTask, File repositoryLocation) {
    publishTask.doFirst(new CleanAction(repositoryLocation));
    Configuration projectRepository = project.getConfigurations().create(MAVEN_REPOSITORY_CONFIGURATION_NAME);

    project.getArtifacts().add(projectRepository.getName(),
            repositoryLocation, artifact -> artifact.builtBy(publishTask));

    DependencySet target = projectRepository.getDependencies();

    project.getPlugins()
            .withType(JavaPlugin.class)
            .all(javaPlugin -> addMavenRepositoryDependencies(
                    project, JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME, target));

    project.getPlugins()
            .withType(JavaLibraryPlugin.class)
            .all(javaLibraryPlugin -> addMavenRepositoryDependencies(
                    project, JavaPlugin.API_CONFIGURATION_NAME, target));

    project.getPlugins()
            .withType(JavaPlatformPlugin.class)
            .all(javaPlugin -> addMavenRepositoryDependencies(
                    project, JavaPlatformPlugin.API_CONFIGURATION_NAME, target));
  }

  private void addMavenRepositoryDependencies(Project project, String sourceConfigurationName, DependencySet target) {
    project.getConfigurations()
            .getByName(sourceConfigurationName)
            .getDependencies()
            .withType(ProjectDependency.class)
            .all(dependency -> {
              Map<String, String> dependencyDescriptor = new HashMap<>();
              dependencyDescriptor.put("path", dependency.getDependencyProject().getPath());
              dependencyDescriptor.put("configuration", MAVEN_REPOSITORY_CONFIGURATION_NAME);
              target.add(project.getDependencies().project(dependencyDescriptor));
            });
  }

  private static final class CleanAction implements Action<Task> {

    private final File location;

    private CleanAction(File location) {
      this.location = location;
    }

    @Override
    public void execute(Task task) {
      task.getProject().delete(this.location);
    }

  }

}
