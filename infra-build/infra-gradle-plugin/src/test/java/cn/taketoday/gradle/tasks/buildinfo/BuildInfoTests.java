/*
 * Copyright 2012 - 2023 the original author or authors.
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

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.initialization.GradlePropertiesController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import cn.taketoday.gradle.junit.GradleProjectBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link BuildInfo}.
 *
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
class BuildInfoTests {

  @TempDir
  File temp;

  @Test
  void basicExecution() {
    Properties properties = buildInfoProperties(createTask(createProject("test")));
    assertThat(properties).containsKey("build.time");
    assertThat(properties).doesNotContainKey("build.artifact");
    assertThat(properties).doesNotContainKey("build.group");
    assertThat(properties).containsEntry("build.name", "test");
    assertThat(properties).containsEntry("build.version", "unspecified");
  }

  @Test
  void customArtifactIsReflectedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getProperties().getArtifact().set("custom");
    assertThat(buildInfoProperties(task)).containsEntry("build.artifact", "custom");
  }

  @Test
  void artifactCanBeExcludedFromProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getExcludes().addAll("artifact");
    assertThat(buildInfoProperties(task)).doesNotContainKey("build.artifact");
  }

  @Test
  void projectGroupIsReflectedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getProject().setGroup("com.example");
    assertThat(buildInfoProperties(task)).containsEntry("build.group", "com.example");
  }

  @Test
  void customGroupIsReflectedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getProperties().getGroup().set("com.example");
    assertThat(buildInfoProperties(task)).containsEntry("build.group", "com.example");
  }

  @Test
  void groupCanBeExcludedFromProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getExcludes().add("group");
    assertThat(buildInfoProperties(task)).doesNotContainKey("build.group");
  }

  @Test
  void customNameIsReflectedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getProperties().getName().set("Example");
    assertThat(buildInfoProperties(task)).containsEntry("build.name", "Example");
  }

  @Test
  void nameCanBeExludedRemovedFromProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getExcludes().add("name");
    assertThat(buildInfoProperties(task)).doesNotContainKey("build.name");
  }

  @Test
  void projectVersionIsReflectedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getProject().setVersion("1.2.3");
    assertThat(buildInfoProperties(task)).containsEntry("build.version", "1.2.3");
  }

  @Test
  void customVersionIsReflectedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getProperties().getVersion().set("2.3.4");
    assertThat(buildInfoProperties(task)).containsEntry("build.version", "2.3.4");
  }

  @Test
  void versionCanBeExcludedFromProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getExcludes().add("version");
    assertThat(buildInfoProperties(task)).doesNotContainKey("build.version");
  }

  @Test
  void timeIsSetInProperties() {
    BuildInfo task = createTask(createProject("test"));
    assertThat(buildInfoProperties(task)).containsKey("build.time");
  }

  @Test
  void timeCanBeExcludedFromProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getExcludes().add("time");
    assertThat(buildInfoProperties(task)).doesNotContainKey("build.time");
  }

  @Test
  void timeCanBeCustomizedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    String isoTime = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
    task.getProperties().getTime().set(isoTime);
    assertThat(buildInfoProperties(task)).containsEntry("build.time", isoTime);
  }

  @Test
  void additionalPropertiesAreReflectedInProperties() {
    BuildInfo task = createTask(createProject("test"));
    task.getProperties().getAdditional().put("a", "alpha");
    task.getProperties().getAdditional().put("b", "bravo");
    assertThat(buildInfoProperties(task)).containsEntry("build.a", "alpha").containsEntry("build.b", "bravo");
  }

  @Test
  void additionalPropertiesCanBeExcluded() {
    BuildInfo task = createTask(createProject("test"));
    task.getProperties().getAdditional().put("a", "alpha");
    task.getExcludes().add("b");
    assertThat(buildInfoProperties(task)).containsEntry("build.a", "alpha").doesNotContainKey("b");
  }

  @Test
  void nullAdditionalPropertyProducesInformativeFailure() {
    BuildInfo task = createTask(createProject("test"));
    assertThatThrownBy(() -> task.getProperties().getAdditional().put("a", null))
            .hasMessage("Cannot add an entry with a null value to a property of type Map.");
  }

  private Project createProject(String projectName) {
    File projectDir = new File(this.temp, projectName);
    Project project = GradleProjectBuilder.builder().withProjectDir(projectDir).withName(projectName).build();
    ((ProjectInternal) project).getServices()
            .get(GradlePropertiesController.class)
            .loadGradlePropertiesFrom(projectDir, false);
    return project;
  }

  private BuildInfo createTask(Project project) {
    return project.getTasks().create("testBuildInfo", BuildInfo.class);
  }

  private Properties buildInfoProperties(BuildInfo task) {
    task.generateBuildProperties();
    return buildInfoProperties(new File(task.getDestinationDir().get().getAsFile(), "build-info.properties"));
  }

  private Properties buildInfoProperties(File file) {
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
