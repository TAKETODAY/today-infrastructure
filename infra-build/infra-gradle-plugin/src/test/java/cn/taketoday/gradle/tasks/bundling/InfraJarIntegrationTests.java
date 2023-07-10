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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.TestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import cn.taketoday.gradle.junit.GradleCompatibility;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link InfraJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Paddy Drury
 */
@GradleCompatibility(configurationCache = true)
class InfraJarIntegrationTests extends AbstractInfraArchiveIntegrationTests {

  InfraJarIntegrationTests() {
    super("infraJar", "APP-INF/lib/", "APP-INF/classes/", "APP-INF/");
  }

  @TestTemplate
  void whenAResolvableCopyOfAnUnresolvableConfigurationIsResolvedThenResolutionSucceeds() {
    this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.0").build("build");
  }

  @TestTemplate
  void packagedApplicationClasspath() throws IOException {
    copyClasspathApplication();
    BuildResult result = this.gradleBuild.build("launch");
    String output = result.getOutput();
    assertThat(output).containsPattern("1\\. .*classes");
    assertThat(output).containsPattern("2\\. .*library-1.0-SNAPSHOT.jar");
    assertThat(output).containsPattern("3\\. .*commons-lang3-3.9.jar");
    assertThat(output).containsPattern("4\\. .*infra-jarmode-layertools.*.jar");
    assertThat(output).doesNotContain("5. ");
  }

  @TestTemplate
  void explodedApplicationClasspath() throws IOException {
    copyClasspathApplication();
    BuildResult result = this.gradleBuild.build("launch");
    String output = result.getOutput();
    assertThat(output).containsPattern("1\\. .*classes");
    assertThat(output).containsPattern("2\\. .*infra-jarmode-layertools.*.jar");
    assertThat(output).containsPattern("3\\. .*library-1.0-SNAPSHOT.jar");
    assertThat(output).containsPattern("4\\. .*commons-lang3-3.9.jar");
    assertThat(output).doesNotContain("5. ");
  }

  private void copyClasspathApplication() throws IOException {
    copyApplication("classpath");
  }

  @Override
  String[] getExpectedApplicationLayerContents(String... additionalFiles) {
    Set<String> contents = new TreeSet<>(Arrays.asList(additionalFiles));
    contents.addAll(Arrays.asList("APP-INF/classpath.idx", "APP-INF/layers.idx", "META-INF/"));
    return contents.toArray(new String[0]);
  }

}
