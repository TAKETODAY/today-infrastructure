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
