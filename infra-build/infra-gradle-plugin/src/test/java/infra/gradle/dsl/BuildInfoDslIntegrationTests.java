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

package infra.gradle.dsl;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import infra.gradle.junit.GradleCompatibility;
import infra.gradle.tasks.buildinfo.BuildInfo;
import infra.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link BuildInfo} created using the
 * {@link InfraApplicationExtension DSL}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class BuildInfoDslIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void basicJar() {
    assertThat(this.gradleBuild.build("infraBuildInfo", "--stacktrace").task(":infraBuildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    Properties properties = buildInfoProperties();
    assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.artifact", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.group", "com.example");
    assertThat(properties).containsEntry("build.version", "1.0");
  }

  @TestTemplate
  void jarWithCustomName() {
    assertThat(this.gradleBuild.build("infraBuildInfo", "--stacktrace").task(":infraBuildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    Properties properties = buildInfoProperties();
    assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.artifact", "foo");
    assertThat(properties).containsEntry("build.group", "com.example");
    assertThat(properties).containsEntry("build.version", "1.0");
  }

  @TestTemplate
  void basicWar() {
    assertThat(this.gradleBuild.build("infraBuildInfo", "--stacktrace").task(":infraBuildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    Properties properties = buildInfoProperties();
    assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.artifact", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.group", "com.example");
    assertThat(properties).containsEntry("build.version", "1.0");
  }

  @TestTemplate
  void warWithCustomName() {
    assertThat(this.gradleBuild.build("infraBuildInfo", "--stacktrace").task(":infraBuildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    Properties properties = buildInfoProperties();
    assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.artifact", "foo");
    assertThat(properties).containsEntry("build.group", "com.example");
    assertThat(properties).containsEntry("build.version", "1.0");
  }

  @TestTemplate
  void additionalProperties() {
    assertThat(this.gradleBuild.build("infraBuildInfo", "--stacktrace").task(":infraBuildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    Properties properties = buildInfoProperties();
    assertThat(properties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.artifact", this.gradleBuild.getProjectDir().getName());
    assertThat(properties).containsEntry("build.group", "com.example");
    assertThat(properties).containsEntry("build.version", "1.0");
    assertThat(properties).containsEntry("build.a", "alpha");
    assertThat(properties).containsEntry("build.b", "bravo");
  }

  @TestTemplate
  void classesDependency() {
    assertThat(this.gradleBuild.build("classes", "--stacktrace").task(":infraBuildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
  }

  private Properties buildInfoProperties() {
    File file = new File(this.gradleBuild.getProjectDir(), "build/resources/main/META-INF/build-info.properties");
    assertThat(file).isFile();
    Properties properties = new Properties();
    try (FileReader reader = new FileReader(file)) {
      properties.load(reader);
      return properties;
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
