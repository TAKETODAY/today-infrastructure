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
