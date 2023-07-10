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
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.testing.Test;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * A {@link Plugin} to configure integration testing support in a {@link Project}.
 *
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class IntegrationTestPlugin implements Plugin<Project> {

  /**
   * Name of the {@code intTest} task.
   */
  public static final String INT_TEST_TASK_NAME = "intTest";

  /**
   * Name of the {@code intTest} source set.
   */
  public static final String INT_TEST_SOURCE_SET_NAME = "intTest";

  @Override
  public void apply(Project project) {
    project.getPlugins().withType(JavaPlugin.class, (javaPlugin) -> configureIntegrationTesting(project));
  }

  private void configureIntegrationTesting(Project project) {
    SourceSet intTestSourceSet = createSourceSet(project);
    Test intTest = createTestTask(project, intTestSourceSet);
    project.getTasks().getByName(LifecycleBasePlugin.CHECK_TASK_NAME).dependsOn(intTest);
  }

  private SourceSet createSourceSet(Project project) {
    SourceSetContainer sourceSets = project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets();
    SourceSet intTestSourceSet = sourceSets.create(INT_TEST_SOURCE_SET_NAME);
    SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
    intTestSourceSet.setCompileClasspath(intTestSourceSet.getCompileClasspath().plus(main.getOutput()));
    intTestSourceSet.setRuntimeClasspath(intTestSourceSet.getRuntimeClasspath().plus(main.getOutput()));
    return intTestSourceSet;
  }

  private Test createTestTask(Project project, SourceSet intTestSourceSet) {
    Test intTest = project.getTasks().create(INT_TEST_TASK_NAME, Test.class);
    intTest.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
    intTest.setDescription("Runs integration tests.");
    intTest.setTestClassesDirs(intTestSourceSet.getOutput().getClassesDirs());
    intTest.setClasspath(intTestSourceSet.getRuntimeClasspath());
    intTest.shouldRunAfter(JavaPlugin.TEST_TASK_NAME);
    return intTest;
  }

}
