/*
 * Copyright 2012-present the original author or authors.
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

package infra.gradle.plugin;

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

import infra.gradle.junit.GradleCompatibility;
import infra.gradle.testkit.GradleBuild;

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
    assertThat(buildLibs.listFiles()).contains(
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
