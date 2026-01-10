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

package infra.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import infra.gradle.junit.GradleProjectBuilder;
import infra.test.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraApplicationPlugin}.
 *
 * @author Martin Chalupa
 * @author Andy Wilkinson
 */
@ClassPathExclusions("kotlin-daemon-client-*.jar")
class InfraApplicationPluginTests {

  @TempDir
  File temp;

  @Test
  void infraArchivesConfigurationsCannotBeResolved() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply(InfraApplicationPlugin.class);
    Configuration infraArchives = project.getConfigurations()
            .getByName(InfraApplicationPlugin.INFRA_ARCHIVES_CONFIGURATION_NAME);
    assertThat(infraArchives.isCanBeResolved()).isFalse();
  }

}
