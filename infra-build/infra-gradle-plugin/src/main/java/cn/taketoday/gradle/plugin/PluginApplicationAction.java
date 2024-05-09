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

package cn.taketoday.gradle.plugin;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * An {@link Action} to be executed on a {@link Project} in response to a particular type
 * of {@link Plugin} being applied.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
interface PluginApplicationAction extends Action<Project> {

  /**
   * The class of the {@code Plugin} that, when applied, will trigger the execution of
   * this action.
   *
   * @return the plugin class
   * @throws ClassNotFoundException if the plugin class cannot be found
   * @throws NoClassDefFoundError if an error occurs when defining the plugin class
   */
  Class<? extends Plugin<? extends Project>> getPluginClass() throws ClassNotFoundException, NoClassDefFoundError;

  default boolean autoApply(Project project) {
    return !project.hasProperty("skipAutoApplyPlugins");
  }

}
