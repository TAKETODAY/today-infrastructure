/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.build.optional;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSetContainer;

/**
 * A {@code Plugin} that adds support for Maven-style optional dependencies. Creates a new
 * {@code optional} configuration. The {@code optional} configuration is part of the
 * project's compile and runtime classpaths but does not affect the classpath of
 * dependent projects.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class OptionalDependenciesPlugin implements Plugin<Project> {

  /**
   * Name of the {@code optional} configuration.
   */
  public static final String OPTIONAL_CONFIGURATION_NAME = "optional";

  @Override
  public void apply(Project project) {
    Configuration optional = project.getConfigurations().create(OPTIONAL_CONFIGURATION_NAME);
    optional.setCanBeConsumed(false);
    optional.setCanBeResolved(false);
    project.getPlugins().withType(JavaBasePlugin.class, (javaBasePlugin) -> {
      SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class)
              .getSourceSets();
      sourceSets.all((sourceSet) -> {
        project.getConfigurations().getByName(sourceSet.getCompileClasspathConfigurationName()).extendsFrom(optional);
        project.getConfigurations().getByName(sourceSet.getRuntimeClasspathConfigurationName()).extendsFrom(optional);
      });
    });
  }

}