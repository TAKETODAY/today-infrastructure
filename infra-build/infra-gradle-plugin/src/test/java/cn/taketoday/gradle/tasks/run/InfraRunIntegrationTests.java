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

package cn.taketoday.gradle.tasks.run;

import org.assertj.core.api.Assumptions;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.TestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import cn.taketoday.gradle.junit.GradleCompatibility;
import cn.taketoday.gradle.testkit.GradleBuild;
import cn.taketoday.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the {@link InfraRun} task.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility(configurationCache = true)
class InfraRunIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void basicExecution() throws IOException {
    copyClasspathApplication();
    new File(this.gradleBuild.getProjectDir(), "src/main/resources").mkdirs();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("1. " + canonicalPathOf("build/classes/java/main"));
    assertThat(result.getOutput()).contains("2. " + canonicalPathOf("build/resources/main"));
    assertThat(result.getOutput()).doesNotContain(canonicalPathOf("src/main/resources"));
  }

  @TestTemplate
  void sourceResourcesCanBeUsed() throws IOException {
    copyClasspathApplication();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("1. " + canonicalPathOf("src/main/resources"));
    assertThat(result.getOutput()).contains("2. " + canonicalPathOf("build/classes/java/main"));
    assertThat(result.getOutput()).doesNotContain(canonicalPathOf("build/resources/main"));
  }

  @TestTemplate
  void infraApplicationExtensionMainClassNameIsUsed() throws IOException {
    copyMainClassApplication();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("com.example.infrarun.main.CustomMainClass");
  }

  @TestTemplate
  void applicationPluginMainClassNameIsUsed() throws IOException {
    copyMainClassApplication();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("com.example.infrarun.main.CustomMainClass");
  }

  @TestTemplate
  void applicationPluginMainClassNameIsNotUsedWhenItIsNull() throws IOException {
    copyClasspathApplication();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput())
            .contains("Main class name = com.example.infrarun.classpath.InfraRunClasspathApplication");
  }

  @TestTemplate
  void defaultJvmArgs() throws IOException {
    copyJvmArgsApplication();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("-XX:TieredStopAtLevel=1");
  }

  @TestTemplate
  void optimizedLaunchDisabledJvmArgs() throws IOException {
    copyJvmArgsApplication();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).doesNotContain("-Xverify:none").doesNotContain("-XX:TieredStopAtLevel=1");
  }

  @TestTemplate
  void applicationPluginJvmArgumentsAreUsed() throws IOException {
    if (this.gradleBuild.isConfigurationCache()) {
      // https://github.com/gradle/gradle/pull/23924
      GradleVersion gradleVersion = GradleVersion.version(this.gradleBuild.getGradleVersion());
      Assumptions.assumeThat(gradleVersion)
              .isLessThan(GradleVersion.version("8.0"))
              .isGreaterThanOrEqualTo(GradleVersion.version("8.1-rc-1"));
    }
    copyJvmArgsApplication();
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("-Dcom.bar=baz")
            .contains("-Dcom.foo=bar")
            .contains("-XX:TieredStopAtLevel=1");
  }

  @TestTemplate
  void jarTypeFilteringIsAppliedToTheClasspath() throws IOException {
    copyClasspathApplication();
    File flatDirRepository = new File(this.gradleBuild.getProjectDir(), "repository");
    createDependenciesStarterJar(new File(flatDirRepository, "starter.jar"));
    createStandardJar(new File(flatDirRepository, "standard.jar"));
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("standard.jar").doesNotContain("starter.jar");
  }

  @TestTemplate
  void classesFromASecondarySourceSetCanBeOnTheClasspath() throws IOException {
    File output = new File(this.gradleBuild.getProjectDir(), "src/secondary/java/com/example/infrarun/main");
    output.mkdirs();
    FileSystemUtils.copyRecursively(new File("src/test/java/com/example/infrarun/main"), output);
    BuildResult result = this.gradleBuild.build("infraRun");
    assertThat(result.task(":infraRun").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    assertThat(result.getOutput()).contains("com.example.infrarun.main.CustomMainClass");
  }

  private void copyMainClassApplication() throws IOException {
    copyApplication("main");
  }

  private void copyClasspathApplication() throws IOException {
    copyApplication("classpath");
  }

  private void copyJvmArgsApplication() throws IOException {
    copyApplication("jvmargs");
  }

  private void copyApplication(String name) throws IOException {
    File output = new File(this.gradleBuild.getProjectDir(), "src/main/java/com/example/infrarun/" + name);
    output.mkdirs();
    FileSystemUtils.copyRecursively(new File("src/test/java/com/example/infrarun/" + name), output);
  }

  private String canonicalPathOf(String path) throws IOException {
    return new File(this.gradleBuild.getProjectDir(), path).getCanonicalPath();
  }

  private void createStandardJar(File location) throws IOException {
    createJar(location, (attributes) -> {
    });
  }

  private void createDependenciesStarterJar(File location) throws IOException {
    createJar(location, (attributes) -> attributes.putValue("Infra-App-Jar-Type", "dependencies-starter"));
  }

  private void createJar(File location, Consumer<Attributes> attributesConfigurer) throws IOException {
    location.getParentFile().mkdirs();
    Manifest manifest = new Manifest();
    Attributes attributes = manifest.getMainAttributes();
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    attributesConfigurer.accept(attributes);
    new JarOutputStream(new FileOutputStream(location), manifest).close();
  }

}
