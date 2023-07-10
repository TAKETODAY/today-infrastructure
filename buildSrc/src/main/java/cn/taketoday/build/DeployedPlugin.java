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

package cn.taketoday.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlatformPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.bundling.Jar;

import cn.taketoday.build.maven.MavenRepositoryPlugin;

/**
 * A plugin applied to a project that should be deployed.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DeployedPlugin implements Plugin<Project> {

  /**
   * Name of the task that generates the deployed pom file.
   */
  public static final String GENERATE_POM_TASK_NAME = "generatePomFileForMavenPublication";

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(MavenPublishPlugin.class);
    project.getPlugins().apply(MavenRepositoryPlugin.class);
    PublishingExtension publishing = project.getExtensions().getByType(PublishingExtension.class);
    MavenPublication mavenPublication = publishing.getPublications().create("maven", MavenPublication.class);

    project.afterEvaluate(evaluated -> project.getPlugins().withType(JavaPlugin.class).all(javaPlugin -> {
      if (((Jar) project.getTasks().getByName(JavaPlugin.JAR_TASK_NAME)).isEnabled()) {
        project.getComponents()
                .matching((component) -> component.getName().equals("java"))
                .all(mavenPublication::from);
      }
    }));
    project.getPlugins()
            .withType(JavaPlatformPlugin.class)
            .all(javaPlugin -> project.getComponents()
                    .matching(component -> component.getName().equals("javaPlatform"))
                    .all(mavenPublication::from));
  }

}
