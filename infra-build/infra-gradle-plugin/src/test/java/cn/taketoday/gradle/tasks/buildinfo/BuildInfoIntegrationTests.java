/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.gradle.tasks.buildinfo;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Properties;

import cn.taketoday.app.loader.tools.FileUtils;
import cn.taketoday.gradle.junit.GradleCompatibility;
import cn.taketoday.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link BuildInfo} task.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
@GradleCompatibility(configurationCache = true)
class BuildInfoIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void defaultValues() {
    assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    Properties buildInfoProperties = buildInfoProperties();
    assertThat(buildInfoProperties).containsKey("build.time");
    assertThat(buildInfoProperties).doesNotContainKey("build.artifact");
    assertThat(buildInfoProperties).doesNotContainKey("build.group");
    assertThat(buildInfoProperties).containsEntry("build.name", this.gradleBuild.getProjectDir().getName());
    assertThat(buildInfoProperties).containsEntry("build.version", "unspecified");
  }

  @TestTemplate
  void basicExecution() {
    assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    Properties buildInfoProperties = buildInfoProperties();
    assertThat(buildInfoProperties).containsKey("build.time");
    assertThat(buildInfoProperties).containsEntry("build.artifact", "foo");
    assertThat(buildInfoProperties).containsEntry("build.group", "foo");
    assertThat(buildInfoProperties).containsEntry("build.additional", "foo");
    assertThat(buildInfoProperties).containsEntry("build.name", "foo");
    assertThat(buildInfoProperties).containsEntry("build.version", "0.1.0");
  }

  @TestTemplate
  void notUpToDateWhenExecutedTwiceAsTimeChanges() {
    assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    Properties first = buildInfoProperties();
    String firstBuildTime = first.getProperty("build.time");
    assertThat(firstBuildTime).isNotNull();
    assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    Properties second = buildInfoProperties();
    String secondBuildTime = second.getProperty("build.time");
    assertThat(secondBuildTime).isNotNull();
    assertThat(Instant.parse(firstBuildTime)).isBefore(Instant.parse(secondBuildTime));
  }

  @TestTemplate
  void upToDateWhenExecutedTwiceWithFixedTime() {
    assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
            .isEqualTo(TaskOutcome.UP_TO_DATE);
  }

  @TestTemplate
  void notUpToDateWhenExecutedTwiceWithFixedTimeAndChangedProjectVersion() {
    assertThat(this.gradleBuild.scriptProperty("projectVersion", "0.1.0")
            .build("buildInfo")
            .task(":buildInfo")
            .getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(this.gradleBuild.scriptProperty("projectVersion", "0.2.0")
            .build("buildInfo")
            .task(":buildInfo")
            .getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

  @TestTemplate
  void notUpToDateWhenExecutedTwiceWithFixedTimeAndChangedGradlePropertiesProjectVersion() throws IOException {
    Path gradleProperties = new File(this.gradleBuild.getProjectDir(), "gradle.properties").toPath();
    Files.writeString(gradleProperties, "version=0.1.0", StandardOpenOption.CREATE, StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING);
    assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    Files.writeString(gradleProperties, "version=0.2.0", StandardOpenOption.CREATE, StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING);
    assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
  }

  @TestTemplate
  void reproducibleOutputWithFixedTime() throws IOException, InterruptedException {
    assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    File buildInfoProperties = new File(this.gradleBuild.getProjectDir(), "build/buildInfo/build-info.properties");
    String firstHash = FileUtils.sha1Hash(buildInfoProperties);
    assertThat(buildInfoProperties.delete()).isTrue();
    Thread.sleep(1500);
    assertThat(this.gradleBuild.build("buildInfo", "-PnullTime").task(":buildInfo").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
    String secondHash = FileUtils.sha1Hash(buildInfoProperties);
    assertThat(firstHash).isEqualTo(secondHash);
  }

  @TestTemplate
  void excludeProperties() {
    assertThat(this.gradleBuild.build("buildInfo").task(":buildInfo").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    Properties buildInfoProperties = buildInfoProperties();
    assertThat(buildInfoProperties).doesNotContainKey("build.group");
    assertThat(buildInfoProperties).doesNotContainKey("build.artifact");
    assertThat(buildInfoProperties).doesNotContainKey("build.version");
    assertThat(buildInfoProperties).doesNotContainKey("build.name");
  }

  private Properties buildInfoProperties() {
    File file = new File(this.gradleBuild.getProjectDir(), "build/buildInfo/build-info.properties");
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
