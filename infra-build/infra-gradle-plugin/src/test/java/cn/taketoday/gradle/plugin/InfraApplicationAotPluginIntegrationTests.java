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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.gradle.plugin;

import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import cn.taketoday.gradle.junit.GradleCompatibility;
import cn.taketoday.gradle.testkit.GradleBuild;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * Integration tests for {@link InfraApplicationAotPlugin}.
 *
 * @author Andy Wilkinson
 */
@GradleCompatibility
class InfraApplicationAotPluginIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void noProcessAotTaskWithoutAotPluginApplied() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processAot").getOutput())
            .contains("processAot exists = false");
  }

  @TestTemplate
  void noProcessTestAotTaskWithoutAotPluginApplied() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processTestAot").getOutput())
            .contains("processTestAot exists = false");
  }

  @TestTemplate
  void applyingAotPluginCreatesProcessAotTask() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processAot").getOutput())
            .contains("processAot exists = true");
  }

  @TestTemplate
  void applyingAotPluginCreatesProcessTestAotTask() {
    assertThat(this.gradleBuild.build("taskExists", "-PtaskName=processTestAot").getOutput())
            .contains("processTestAot exists = true");
  }

  @TestTemplate
  void processAotHasLibraryResourcesOnItsClasspath() throws IOException {
    File settings = new File(this.gradleBuild.getProjectDir(), "settings.gradle");
    Files.write(settings.toPath(), List.of("include 'library'"));
    File library = new File(this.gradleBuild.getProjectDir(), "library");
    library.mkdirs();
    Files.write(library.toPath().resolve("build.gradle"), List.of("plugins {", "    id 'java-library'", "}"));
    assertThat(this.gradleBuild.build("processAotClasspath").getOutput()).contains("library.jar");
  }

  @TestTemplate
  void processTestAotHasLibraryResourcesOnItsClasspath() throws IOException {
    File settings = new File(this.gradleBuild.getProjectDir(), "settings.gradle");
    Files.write(settings.toPath(), List.of("include 'library'"));
    File library = new File(this.gradleBuild.getProjectDir(), "library");
    library.mkdirs();
    Files.write(library.toPath().resolve("build.gradle"), List.of("plugins {", "    id 'java-library'", "}"));
    assertThat(this.gradleBuild.build("processTestAotClasspath").getOutput()).contains("library.jar");
  }

  @TestTemplate
  void processAotHasTransitiveRuntimeDependenciesOnItsClasspath() {
    String output = this.gradleBuild.build("processAotClasspath").getOutput();
    assertThat(output).contains("org.jboss.logging" + File.separatorChar + "jboss-logging");
  }

  @TestTemplate
  void processTestAotHasTransitiveRuntimeDependenciesOnItsClasspath() {
    String output = this.gradleBuild.build("processTestAotClasspath").getOutput();
    assertThat(output).contains("org.jboss.logging" + File.separatorChar + "jboss-logging");
  }

  @TestTemplate
  void processAotDoesNotHaveDevelopmentOnlyDependenciesOnItsClasspath() {
    String output = this.gradleBuild.build("processAotClasspath").getOutput();
    assertThat(output).doesNotContain("commons-lang");
  }

  @TestTemplate
  void processTestAotDoesNotHaveDevelopmentOnlyDependenciesOnItsClasspath() {
    String output = this.gradleBuild.build("processTestAotClasspath").getOutput();
    assertThat(output).doesNotContain("commons-lang");
  }

  @TestTemplate
  void processAotDoesNotHaveTestAndDevelopmentOnlyDependenciesOnItsClasspath() {
    String output = this.gradleBuild.build("processAotClasspath").getOutput();
    assertThat(output).doesNotContain("commons-lang");
  }

  @TestTemplate
  void processTestAotHasTestAndDevelopmentOnlyDependenciesOnItsClasspath() {
    String output = this.gradleBuild.build("processTestAotClasspath").getOutput();
    assertThat(output).contains("commons-lang");
  }

  @TestTemplate
  void processAotRunsWhenProjectHasMainSource() throws IOException {
    writeMainClass("cn.taketoday.framework", "ApplicationAotProcessor");
    writeMainClass("com.example", "Main");
    assertThat(this.gradleBuild.build("processAot").task(":processAot").getOutcome())
            .isEqualTo(TaskOutcome.SUCCESS);
  }

  @TestTemplate
  void processTestAotIsSkippedWhenProjectHasNoTestSource() {
    assertThat(this.gradleBuild.build("processTestAot").task(":processTestAot").getOutcome())
            .isEqualTo(TaskOutcome.NO_SOURCE);
  }

  @TestTemplate
  @EnabledOnJre(JRE.JAVA_17)
  void applyingAotPluginDoesNotPreventConfigurationOfJavaToolchainLanguageVersion() {
    assertThatNoException().isThrownBy(() -> this.gradleBuild.build("help").getOutput());
  }

  private void writeMainClass(String packageName, String className) throws IOException {
    File java = new File(this.gradleBuild.getProjectDir(),
            "src/main/java/" + packageName.replace(".", "/") + "/" + className + ".java");
    java.getParentFile().mkdirs();
    Files.writeString(java.toPath(), """
            package %s;

            public class %s {

            	public static void main(String[] args) {

            	}

            }
            """.formatted(packageName, className));
  }

}
