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

package cn.taketoday.gradle.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import cn.taketoday.gradle.junit.GradleCompatibility;
import cn.taketoday.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JavaPluginAction}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class WarPluginActionIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void noInfraWarTaskWithoutWarPluginApplied() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=infraWar").getOutput())
            .contains("infraWar exists = false");
  }

  @TestTemplate
  void applyingWarPluginCreatesInfraWarTask() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=infraWar", "-PapplyWarPlugin").getOutput())
            .contains("infraWar exists = true");
  }

  @TestTemplate
  void assembleRunsInfraWarAndWar() {
    BuildResult result = this.gradleBuild.build("assemble");
    assertThat(result.task(":infraWar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.task(":war").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
    assertThat(buildLibs.listFiles()).containsExactlyInAnyOrder(
            new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".war"),
            new File(buildLibs, this.gradleBuild.getProjectDir().getName() + "-plain.war"));
  }

  @TestTemplate
  void errorMessageIsHelpfulWhenMainClassCannotBeResolved() {
    BuildResult result = this.gradleBuild.buildAndFail("build", "-PapplyWarPlugin");
    assertThat(result.task(":infraWar").getOutcome()).isEqualTo(TaskOutcome.FAILED);
    assertThat(result.getOutput()).contains("Main class name has not been configured and it could not be resolved");
  }

  @TestTemplate
  void taskConfigurationIsAvoided() throws IOException {
    BuildResult result = this.gradleBuild.build("help");
    String output = result.getOutput();
    BufferedReader reader = new BufferedReader(new StringReader(output));
    String line;
    Set<String> configured = new HashSet<>();
    while ((line = reader.readLine()) != null) {
      if (line.startsWith("Configuring :")) {
        configured.add(line.substring("Configuring :".length()));
      }
    }
    if (GradleVersion.version(this.gradleBuild.getGradleVersion()).compareTo(GradleVersion.version("7.3.3")) < 0) {
      assertThat(configured).containsExactly("help");
    }
    else {
      assertThat(configured).containsExactlyInAnyOrder("help", "clean");
    }
  }

}
