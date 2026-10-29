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

import org.graalvm.buildtools.gradle.NativeImagePlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.plugins.ApplicationPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import infra.gradle.dsl.InfraApplicationExtension;
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

  @Test
  void applicationPluginIsAppliedByDefault() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply(InfraApplicationPlugin.class);
    assertApplicationIntegration(project);
  }

  @Test
  void applicationPluginIsNotAppliedWhenAutoApplyIsSkipped() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getExtensions().getExtraProperties().set("skipAutoApplyPlugins", true);
    project.getPlugins().apply(InfraApplicationPlugin.class);
    assertThat(project.getPlugins().hasPlugin(ApplicationPlugin.class)).isFalse();
    assertThat(project.getTasks().getNames()).doesNotContain("run", "startScripts", "infraStartScripts");
  }

  @Test
  void applicationPluginCanBeAppliedAfterAutoApplyIsSkipped() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getExtensions().getExtraProperties().set("skipAutoApplyPlugins", true);
    project.getPlugins().apply(InfraApplicationPlugin.class);
    project.getPlugins().apply(ApplicationPlugin.class);
    assertApplicationIntegration(project);
  }

  @Test
  void applicationPluginCanBeAppliedBeforeInfraApplicationPlugin() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply(ApplicationPlugin.class);
    project.getPlugins().apply(InfraApplicationPlugin.class);
    assertApplicationIntegration(project);
  }

  @Test
  void applicationPluginCanBeAppliedAgainAfterAutoApply() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply(InfraApplicationPlugin.class);
    project.getPlugins().apply(ApplicationPlugin.class);
    assertApplicationIntegration(project);
  }

  private void assertApplicationIntegration(Project project) {
    assertThat(project.getPlugins().hasPlugin(ApplicationPlugin.class)).isTrue();
    assertThat(project.getTasks().getNames()).contains("run", "startScripts", "infraStartScripts",
            "distZip", "distTar", "infraDistZip", "infraDistTar");
    assertThat(project.getExtensions().getByType(DistributionContainer.class).getNames())
            .containsExactlyInAnyOrder("main", "infra");
  }

  @Test
  void aotIsNotEnabledByDefault() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply(InfraApplicationPlugin.class);
    assertThat(project.getPlugins().hasPlugin(InfraApplicationAotPlugin.class)).isFalse();
    assertThat(project.getTasks().getNames()).doesNotContain("processAot", "processTestAot");
  }

  @Test
  void aotCanBeEnabledRepeatedlyThroughExtension() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply(InfraApplicationPlugin.class);
    InfraApplicationExtension extension = project.getExtensions().getByType(InfraApplicationExtension.class);
    extension.enableAheadOfTimeProcessing();
    extension.enableAheadOfTimeProcessing();
    assertThat(project.getPlugins().hasPlugin(InfraApplicationAotPlugin.class)).isTrue();
    assertThat(project.getTasks().getNames()).contains("processAot", "processTestAot");
  }

  @Test
  void aotCanBeEnabledBeforeJavaPluginIsApplied() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getExtensions().getExtraProperties().set("skipAutoApplyPlugins", true);
    project.getPlugins().apply(InfraApplicationPlugin.class);
    project.getExtensions().getByType(InfraApplicationExtension.class).enableAheadOfTimeProcessing();
    assertThat(project.getTasks().getNames()).doesNotContain("processAot", "processTestAot");
    project.getPlugins().apply("java");
    assertThat(project.getTasks().getNames()).contains("processAot", "processTestAot");
  }

  @Test
  void nativeImageCanBeEnabledThroughExtension() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply(InfraApplicationPlugin.class);
    InfraApplicationExtension extension = project.getExtensions().getByType(InfraApplicationExtension.class);
    extension.enableNativeImage();
    extension.enableNativeImage();
    assertThat(project.getPlugins().hasPlugin(NativeImagePlugin.class)).isTrue();
    assertThat(project.getPlugins().hasPlugin(InfraApplicationAotPlugin.class)).isTrue();
    assertThat(project.getTasks().getNames()).contains("nativeCompile", "processAot", "processTestAot");
  }

  @Test
  void developmentConfigurationsHaveRuntimeAttributesAndCannotBeConsumed() {
    Project project = GradleProjectBuilder.builder().withProjectDir(this.temp).build();
    project.getPlugins().apply("java");
    project.getPlugins().apply(InfraApplicationPlugin.class);
    Configuration runtime = project.getConfigurations().getByName("runtimeClasspath");
    for (String name : new String[] { "developmentOnly", "testAndDevelopmentOnly" }) {
      Configuration configuration = project.getConfigurations().getByName(name);
      assertThat(configuration.isCanBeConsumed()).isFalse();
      assertThat(configuration.getAttributes().keySet()).isEqualTo(runtime.getAttributes().keySet());
      for (var attribute : runtime.getAttributes().keySet()) {
        assertThat(configuration.getAttributes().getAttribute(attribute)).isEqualTo(runtime.getAttributes().getAttribute(attribute));
      }
    }
  }

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
