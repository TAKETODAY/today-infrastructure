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

package infra.building;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;

/**
 * {@link Plugin} that applies conventions for checkstyle.
 *
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0
 */
public class CheckstyleConventions {

  /**
   * Applies the  Java Format and Checkstyle plugins with the project conventions.
   *
   * @param project the current project
   */
  public void apply(Project project) {
    project.getPlugins().withType(JavaBasePlugin.class, (java) -> {
      project.getPlugins().apply(CheckstylePlugin.class);
      project.getTasks().withType(Checkstyle.class).forEach(checkstyle -> checkstyle.getMaxHeapSize().set("1g"));
      CheckstyleExtension checkstyle = project.getExtensions().getByType(CheckstyleExtension.class);
      checkstyle.setToolVersion("10.23.0");
      checkstyle.getConfigDirectory().set(project.getRootProject().file("checkstyle"));
    });
  }


}
