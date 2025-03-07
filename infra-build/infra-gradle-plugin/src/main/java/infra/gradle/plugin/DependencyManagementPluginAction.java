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

package infra.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import io.spring.gradle.dependencymanagement.DependencyManagementPlugin;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;

/**
 * {@link Action} that is performed in response to the {@link DependencyManagementPlugin}
 * being applied.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DependencyManagementPluginAction implements PluginApplicationAction {

  @Override
  public void execute(Project project) {
    project.getExtensions()
            .getByType(DependencyManagementExtension.class)
            .imports(importsHandler -> importsHandler.mavenBom(InfraApplicationPlugin.dependenciesCoordinates(project)));
  }

  @Override
  public Class<? extends Plugin<Project>> getPluginClass() {
    return DependencyManagementPlugin.class;
  }

}
