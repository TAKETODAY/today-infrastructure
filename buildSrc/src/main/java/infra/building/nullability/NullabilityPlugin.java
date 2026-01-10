/*
 * Copyright 2002-present the original author or authors.
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