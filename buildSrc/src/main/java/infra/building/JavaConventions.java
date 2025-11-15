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
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import infra.building.optional.OptionalDependenciesPlugin;

/**
 * {@link Plugin} that applies conventions for compiling Java sources in Infra Framework.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class JavaConventions {

  private static final List<String> COMPILER_ARGS;

  private static final List<String> TEST_COMPILER_ARGS;

  /**
   * The Java version we should use as the JVM baseline for building the project
   */
  private static final JavaLanguageVersion DEFAULT_LANGUAGE_VERSION = JavaLanguageVersion.of(25);

  /**
   * The Java version we should use as the baseline for the compiled bytecode
   * (the "-release" compiler argument).
   */
  private static final JavaLanguageVersion DEFAULT_RELEASE_VERSION = JavaLanguageVersion.of(17);

  static {
    List<String> commonCompilerArgs = List.of(
            /*"-Xlint:serial",*/ "-Xlint:cast", "-Xlint:classfile",/* "-Xlint:dep-ann",*/
            "-Xlint:divzero", "-Xlint:empty", "-Xlint:finally", "-Xlint:overrides",
            "-Xlint:path", "-Xlint:-processing", "-Xlint:static", /*"-Xlint:try",*/ "-Xlint:-options",
            "-parameters"
    );
    COMPILER_ARGS = new ArrayList<>();
    COMPILER_ARGS.addAll(commonCompilerArgs);
    COMPILER_ARGS.addAll(List.of(
            /* "-Xlint:varargs",*/ "-Xlint:fallthrough" // , "-Xlint:rawtypes" // "-Xlint:deprecation",
            // "-Xlint:unchecked"/*, "-Werror"*/
    ));
    TEST_COMPILER_ARGS = new ArrayList<>();
    TEST_COMPILER_ARGS.addAll(commonCompilerArgs);
    TEST_COMPILER_ARGS.addAll(Arrays.asList("-Xlint:-varargs", "-Xlint:-fallthrough", "-Xlint:-rawtypes",
            "-Xlint:-deprecation", "-Xlint:-unchecked"));
  }

  public void apply(Project project) {
    project.getPlugins().withType(JavaBasePlugin.class, javaPlugin -> {
      applyToolchainConventions(project);
      applyJavaCompileConventions(project);
      configureDependencyManagement(project);
    });
  }

  /**
   * Configure the Toolchain support for the project.
   *
   * @param project the current project
   */
  private static void applyToolchainConventions(Project project) {
    project.getExtensions().getByType(JavaPluginExtension.class).toolchain(toolchain -> {
      toolchain.getLanguageVersion().set(DEFAULT_LANGUAGE_VERSION);
    });
  }

  /**
   * Apply the common Java compiler options for main sources, test fixture sources, and
   * test sources.
   *
   * @param project the current project
   */
  private void applyJavaCompileConventions(Project project) {
    project.afterEvaluate(p -> {
      p.getTasks().withType(JavaCompile.class)
              .matching(compileTask -> compileTask.getName().startsWith(JavaPlugin.COMPILE_JAVA_TASK_NAME))
              .forEach(compileTask -> {
                compileTask.getOptions().setCompilerArgs(COMPILER_ARGS);
                compileTask.getOptions().setEncoding("UTF-8");
                setJavaRelease(compileTask);
              });
      p.getTasks().withType(JavaCompile.class)
              .matching(compileTask -> compileTask.getName().startsWith(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
                      || compileTask.getName().equals("compileTestFixturesJava"))
              .forEach(compileTask -> {
                compileTask.getOptions().setCompilerArgs(TEST_COMPILER_ARGS);
                compileTask.getOptions().setEncoding("UTF-8");
                setJavaRelease(compileTask);
              });

    });
  }

  /**
   * We should pick the {@link #DEFAULT_RELEASE_VERSION} for all compiled classes,
   * unless the current task is compiling multi-release JAR code with a higher version.
   */
  private void setJavaRelease(JavaCompile task) {
    int defaultVersion = DEFAULT_RELEASE_VERSION.asInt();
    int releaseVersion = defaultVersion;
    int compilerVersion = task.getJavaCompiler().get().getMetadata().getLanguageVersion().asInt();
    for (int version = defaultVersion; version <= compilerVersion; version++) {
      if (task.getName().contains("Java" + version)) {
        releaseVersion = version;
        break;
      }
    }
    task.getOptions().getRelease().set(releaseVersion);
  }

  private void configureDependencyManagement(Project project) {
    ConfigurationContainer configurations = project.getConfigurations();
    Configuration dependencyManagement = configurations.create("dependencyManagement", (configuration) -> {
      configuration.setVisible(false);
      configuration.setCanBeConsumed(false);
      configuration.setCanBeResolved(false);
    });

    configurations.matching(configuration -> {
              String name = configuration.getName();
              return name.endsWith("Classpath")
                      || JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME.equals(name)
                      || JavaPlugin.TEST_ANNOTATION_PROCESSOR_CONFIGURATION_NAME.equals(name);
            })
            .forEach(configuration -> configuration.extendsFrom(dependencyManagement));

    Dependency platform = project.getDependencies()
            .enforcedPlatform(project.getDependencies()
                    .project(Collections.singletonMap("path", ":infra-dependencies")));

    dependencyManagement.getDependencies().add(platform);

    project.getPlugins().withType(OptionalDependenciesPlugin.class,
            optionalDependencies -> configurations.getByName(OptionalDependenciesPlugin.OPTIONAL_CONFIGURATION_NAME)
                    .extendsFrom(dependencyManagement));
  }

}
