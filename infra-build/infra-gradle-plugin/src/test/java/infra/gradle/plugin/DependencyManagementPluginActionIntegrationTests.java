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

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import infra.gradle.junit.GradleCompatibility;
import infra.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the configuration applied by
 * {@link DependencyManagementPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class DependencyManagementPluginActionIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void noDependencyManagementIsAppliedByDefault() {
    assertThat(this.gradleBuild.build("doesNotHaveDependencyManagement")
            .task(":doesNotHaveDependencyManagement")
            .getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

  @TestTemplate
  void bomIsImportedWhenDependencyManagementPluginIsApplied() {
    assertThat(this.gradleBuild.build("hasDependencyManagement", "-PapplyDependencyManagementPlugin")
            .task(":hasDependencyManagement")
            .getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

}
