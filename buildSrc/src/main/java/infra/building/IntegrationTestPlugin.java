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

package infra.building;

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
