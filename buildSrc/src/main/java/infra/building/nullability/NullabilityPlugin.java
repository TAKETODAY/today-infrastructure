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

package infra.building.nullability;

import net.ltgt.gradle.errorprone.ErrorProneOptions;
import net.ltgt.gradle.errorprone.ErrorPronePlugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;

import java.util.regex.Pattern;

import infra.building.nullability.NullabilityOptions.Checking;

/**
 * Gradle plugin for compile-time verification of nullability.
 *
 * @author Brian Clozel
 * @author Andy Wilkinson
 */
public class NullabilityPlugin implements Plugin<Project> {

  private static final Pattern COMPILE_MAIN_SOURCES_TASK_NAME = Pattern.compile("compile(\\d+)?Java");

  @Override
  public void apply(Project project) {
    NullabilityPluginExtension nullability = project.getExtensions()
            .create("nullability", NullabilityPluginExtension.class);
    project.getPlugins().apply(ErrorPronePlugin.class);
    configureDependencies(project, nullability);
    configureJavaCompilation(project);
  }

  private void configureDependencies(Project project, NullabilityPluginExtension nullability) {
    project.getConfigurations().getByName(ErrorPronePlugin.CONFIGURATION_NAME);
    project.getDependencies()
            .add(ErrorPronePlugin.CONFIGURATION_NAME, nullability.getErrorProneVersion()
                    .map((version) -> "com.google.errorprone:error_prone_core:" + version));
    project.getDependencies()
            .add(ErrorPronePlugin.CONFIGURATION_NAME,
                    nullability.getNullAwayVersion().map((version) -> "com.uber.nullaway:nullaway:" + version));
  }

  private void configureJavaCompilation(Project project) {
    project.getTasks().withType(JavaCompile.class).configureEach((javaCompile) -> {
      CompileOptions options = javaCompile.getOptions();
      ErrorProneOptions errorProneOptions = ((ExtensionAware) options).getExtensions()
              .getByType(ErrorProneOptions.class);
      NullabilityOptions nullabilityOptions = ((ExtensionAware) javaCompile.getOptions()).getExtensions()
              .create("nullability", NullabilityOptions.class, errorProneOptions);
      nullabilityOptions.getChecking()
              .set(compilesMainSources(javaCompile) ? Checking.MAIN.name() : Checking.DISABLED.name());
    });
  }

  private boolean compilesMainSources(JavaCompile compileTask) {
    return COMPILE_MAIN_SOURCES_TASK_NAME.matcher(compileTask.getName()).matches();
  }

}