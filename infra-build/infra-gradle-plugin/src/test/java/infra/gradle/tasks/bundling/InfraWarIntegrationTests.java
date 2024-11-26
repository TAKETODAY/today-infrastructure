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

package infra.gradle.tasks.bundling;

import org.assertj.core.api.Assumptions;
import org.gradle.util.GradleVersion;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import infra.gradle.junit.GradleCompatibility;

/**
 * Integration tests for {@link InfraWar}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@GradleCompatibility(configurationCache = true)
class InfraWarIntegrationTests extends AbstractInfraArchiveIntegrationTests {

  InfraWarIntegrationTests() {
    super("infraWar", "WEB-INF/lib/", "WEB-INF/classes/", "WEB-INF/");
  }

  @Override
  String[] getExpectedApplicationLayerContents(String... additionalFiles) {
    Set<String> contents = new TreeSet<>(Arrays.asList(additionalFiles));
    contents.addAll(Arrays.asList("WEB-INF/classpath.idx", "WEB-INF/layers.idx", "META-INF/"));
    return contents.toArray(new String[0]);
  }

  @Override
  void multiModuleImplicitLayers() throws IOException {
    whenTestingWithTheConfigurationCacheAssumeThatTheGradleVersionIsLessThan8();
    super.multiModuleImplicitLayers();
  }

  @Override
  void multiModuleCustomLayers() throws IOException {
    whenTestingWithTheConfigurationCacheAssumeThatTheGradleVersionIsLessThan8();
    super.multiModuleCustomLayers();
  }

  private void whenTestingWithTheConfigurationCacheAssumeThatTheGradleVersionIsLessThan8() {
    if (this.gradleBuild.isConfigurationCache()) {
      // With Gradle 8.0, a configuration cache bug prevents ResolvedDependencies
      // from processing dependencies on the runtime classpath
      Assumptions.assumeThat(GradleVersion.version(this.gradleBuild.getGradleVersion()))
              .isLessThan(GradleVersion.version("8.0"));
    }
  }

}
