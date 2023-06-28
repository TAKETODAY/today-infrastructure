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
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.jvm.toolchain.JavaLanguageVersion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link Plugin} that applies conventions for compiling Java sources in Spring Framework.
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

  static {
    List<String> commonCompilerArgs = Arrays.asList(
            /*"-Xlint:serial",*/ "-Xlint:cast", "-Xlint:classfile", "-Xlint:dep-ann",
            "-Xlint:divzero", "-Xlint:empty", "-Xlint:finally", "-Xlint:overrides",
            "-Xlint:path", "-Xlint:-processing", /* "-Xlint:static", "-Xlint:try",*/ "-Xlint:-options",
            "-parameters"
    );
    COMPILER_ARGS = new ArrayList<>();
    COMPILER_ARGS.addAll(commonCompilerArgs);
    COMPILER_ARGS.addAll(Arrays.asList(
            /* "-Xlint:varargs",*/ "-Xlint:fallthrough" // , "-Xlint:rawtypes" // "-Xlint:deprecation",
            // "-Xlint:unchecked"/*, "-Werror"*/
    ));
    TEST_COMPILER_ARGS = new ArrayList<>();
    TEST_COMPILER_ARGS.addAll(commonCompilerArgs);
    TEST_COMPILER_ARGS.addAll(Arrays.asList("-Xlint:-varargs", "-Xlint:-fallthrough", "-Xlint:-rawtypes",
            "-Xlint:-deprecation", "-Xlint:-unchecked"));
  }

  public void apply(Project project) {
    project.getPlugins().withType(JavaBasePlugin.class, javaPlugin -> applyJavaCompileConventions(project));
  }

  /**
   * Applies the common Java compiler options for main sources, test fixture sources, and
   * test sources.
   *
   * @param project the current project
   */
  private void applyJavaCompileConventions(Project project) {
    project.getExtensions().getByType(JavaPluginExtension.class)
            .getToolchain().getLanguageVersion().set(JavaLanguageVersion.of(17));
    project.getTasks().withType(JavaCompile.class)
            .matching(compileTask -> compileTask.getName().equals(JavaPlugin.COMPILE_JAVA_TASK_NAME))
            .forEach(compileTask -> {
              compileTask.getOptions().setCompilerArgs(COMPILER_ARGS);
              compileTask.getOptions().setEncoding("UTF-8");
            });
    project.getTasks().withType(JavaCompile.class)
            .matching(compileTask -> compileTask.getName().equals(JavaPlugin.COMPILE_TEST_JAVA_TASK_NAME)
                    || compileTask.getName().equals("compileTestFixturesJava"))
            .forEach(compileTask -> {
              compileTask.getOptions().setCompilerArgs(TEST_COMPILER_ARGS);
              compileTask.getOptions().setEncoding("UTF-8");
            });
  }

}
