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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import java.io.File;

import cn.taketoday.gradle.junit.GradleCompatibility;
import cn.taketoday.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for publishing Boot jars and wars using Gradle's Maven Publish
 * plugin.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class MavenPublishingIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void infraJarCanBePublished() {
    BuildResult result = this.gradleBuild.build("publish");
    assertThat(result.task(":publish").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(artifactWithSuffix("jar")).isFile();
    assertThat(artifactWithSuffix("pom")).is(pomWith().groupId("com.example")
            .artifactId(this.gradleBuild.getProjectDir().getName())
            .version("1.0")
            .noPackaging()
            .noDependencies());
  }

  @TestTemplate
  void infraWarCanBePublished() {
    BuildResult result = this.gradleBuild.build("publish");
    assertThat(result.task(":publish").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(artifactWithSuffix("war")).isFile();
    assertThat(artifactWithSuffix("pom")).is(pomWith().groupId("com.example")
            .artifactId(this.gradleBuild.getProjectDir().getName())
            .version("1.0")
            .packaging("war")
            .noDependencies());
  }

  private File artifactWithSuffix(String suffix) {
    String name = this.gradleBuild.getProjectDir().getName();
    return new File(new File(this.gradleBuild.getProjectDir(), "build/repo"),
            String.format("com/example/%s/1.0/%s-1.0.%s", name, name, suffix));
  }

  private PomCondition pomWith() {
    return new PomCondition();
  }

}
