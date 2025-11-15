/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.building.multirelease;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtensionContainer;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.jvm.toolchain.JavaToolchainService;

import javax.inject.Inject;

/**
 * A plugin which adds support for building multi-release jars
 * with Gradle.
 *
 * @author Cedric Champeau
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see <a href="https://github.com/melix/mrjar-gradle-plugin">original project</a>
 * @since 5.0
 */
public class MultiReleaseJarPlugin implements Plugin<Project> {

  @Inject
  protected JavaToolchainService getToolchains() {
    throw new UnsupportedOperationException();
  }

  public void apply(Project project) {
    project.getPlugins().apply(JavaPlugin.class);
    ExtensionContainer extensions = project.getExtensions();
    JavaPluginExtension javaPluginExtension = extensions.getByType(JavaPluginExtension.class);
    ConfigurationContainer configurations = project.getConfigurations();
    TaskContainer tasks = project.getTasks();
    DependencyHandler dependencies = project.getDependencies();
    ObjectFactory objects = project.getObjects();
    extensions.create("multiRelease", MultiReleaseExtension.class,
            javaPluginExtension.getSourceSets(),
            configurations,
            tasks,
            dependencies,
            objects);
  }
}
