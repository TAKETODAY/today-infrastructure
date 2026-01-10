/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.gradle.tasks.bundling;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.TestTemplate;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import infra.gradle.junit.GradleCompatibility;

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
    Assumptions.assumeTrue(this.gradleBuild.gradleVersionIsLessThan("9.0-milestone-1"));
    this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.0").build("build");
  }

  @TestTemplate
  void packagedApplicationClasspath() throws IOException {
    copyClasspathApplication();
    BuildResult result = this.gradleBuild.build("launch");
    String output = result.getOutput();
    if (this.gradleBuild.gradleVersionIsLessThan("9.0.0-rc-1")) {
      assertThat(output).containsPattern("1\\. .*classes");
      assertThat(output).containsPattern("2\\. .*library-1.0-SNAPSHOT.jar");
      assertThat(output).containsPattern("3\\. .*commons-lang3-3.9.jar");
      assertThat(output).containsPattern("4\\. .*infra-jarmode-layertools.*.jar");
    }
    else {
      assertThat(output).containsPattern("1\\. .*classes");
      assertThat(output).containsPattern("2\\. .*commons-lang3-3.9.jar");
      assertThat(output).containsPattern("3\\. .*library-1.0-SNAPSHOT.jar");
      assertThat(output).containsPattern("4\\. .*infra-jarmode-layertools.*.jar");
    }
    assertThat(output).doesNotContain("5. ");
  }

  @TestTemplate
  void explodedApplicationClasspath() throws IOException {
    copyClasspathApplication();
    BuildResult result = this.gradleBuild.build("launch");
    String output = result.getOutput();
    if (this.gradleBuild.gradleVersionIsLessThan("9.0.0-rc-1")) {
      assertThat(output).containsPattern("1\\. .*classes");
      assertThat(output).containsPattern("2\\. .*infra-jarmode-layertools.*.jar");
      assertThat(output).containsPattern("3\\. .*library-1.0-SNAPSHOT.jar");
      assertThat(output).containsPattern("4\\. .*commons-lang3-3.9.jar");
    }
    else {
      assertThat(output).containsPattern("1\\. .*classes");
      assertThat(output).containsPattern("2\\. .*infra-jarmode-layertools.*.jar");
      assertThat(output).containsPattern("3\\. .*commons-lang3-3.9.jar");
      assertThat(output).containsPattern("4\\. .*library-1.0-SNAPSHOT.jar");
    }
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
