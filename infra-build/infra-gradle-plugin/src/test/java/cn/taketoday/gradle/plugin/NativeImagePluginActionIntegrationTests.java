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

package cn.taketoday.gradle.plugin;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.TestTemplate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import cn.taketoday.gradle.junit.GradleCompatibility;
import cn.taketoday.gradle.testkit.GradleBuild;
import cn.taketoday.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link NativeImagePluginAction}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 */
@GradleCompatibility(configurationCache = false)
class NativeImagePluginActionIntegrationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void applyingNativeImagePluginAppliesAotPlugin() {
    assertThat(this.gradleBuild.build("aotPluginApplied").getOutput())
            .contains("cn.taketoday.application.aot applied = true");
  }

  @TestTemplate
  void reachabilityMetadataConfigurationFilesAreCopiedToJar() throws IOException {
    writeDummyApplicationAotProcessorMainClass();
    BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.2-rc-1").build("infraJar");
    assertThat(result.task(":infraJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
    File jarFile = new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(buildLibs.listFiles()).contains(jarFile);
    assertThat(getEntryNames(jarFile)).contains(
            "META-INF/native-image/ch.qos.logback/logback-classic/1.2.11/reflect-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/jni-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/proxy-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/reflect-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/resource-config.json");
  }

  @TestTemplate
  void reachabilityMetadataConfigurationFilesFromFileRepositoryAreCopiedToJar() throws IOException {
    writeDummyApplicationAotProcessorMainClass();
    FileSystemUtils.copyRecursively(new File("src/test/resources/reachability-metadata-repository"),
            new File(this.gradleBuild.getProjectDir(), "reachability-metadata-repository"));
    BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.2-rc-1").build("infraJar");
    assertThat(result.task(":infraJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
    File jarFile = new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(buildLibs.listFiles()).contains(jarFile);
    assertThat(getEntryNames(jarFile)).contains(
            "META-INF/native-image/ch.qos.logback/logback-classic/1.2.11/reflect-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/jni-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/proxy-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/reflect-config.json",
            "META-INF/native-image/org.jline/jline/3.21.0/resource-config.json");
  }

  @TestTemplate
  void infraBuildImageIsConfiguredToBuildANativeImage() {
    writeDummyApplicationAotProcessorMainClass();
    BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.2-rc-1")
            .build("infraBuildImageConfiguration");
    assertThat(result.getOutput()).contains("paketobuildpacks/builder:tiny").contains("BP_NATIVE_IMAGE = true");
  }

  @TestTemplate
  void developmentOnlyDependenciesDoNotAppearInNativeImageClasspath() {
    writeDummyApplicationAotProcessorMainClass();
    BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.2-rc-1")
            .build("checkNativeImageClasspath");
    assertThat(result.getOutput()).doesNotContain("commons-lang");
  }

  @TestTemplate
  void classesGeneratedDuringAotProcessingAreOnTheNativeImageClasspath() {
    BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.2-rc-1")
            .build("checkNativeImageClasspath");
    assertThat(result.getOutput()).contains(projectPath("build/classes/java/aot"),
            projectPath("build/resources/aot"), projectPath("build/generated/aotClasses"));
  }

  @TestTemplate
  void classesGeneratedDuringAotTestProcessingAreOnTheTestNativeImageClasspath() {
    BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.2-rc-1")
            .build("checkTestNativeImageClasspath");
    assertThat(result.getOutput()).contains(projectPath("build/classes/java/aotTest"),
            projectPath("build/resources/aotTest"), projectPath("build/generated/aotTestClasses"));
  }

  @TestTemplate
  void nativeEntryIsAddedToManifest() throws IOException {
    writeDummyApplicationAotProcessorMainClass();
    BuildResult result = this.gradleBuild.expectDeprecationWarningsWithAtLeastVersion("8.2-rc-1").build("infraJar");
    assertThat(result.task(":infraJar").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
    File buildLibs = new File(this.gradleBuild.getProjectDir(), "build/libs");
    JarFile jarFile = new JarFile(new File(buildLibs, this.gradleBuild.getProjectDir().getName() + ".jar"));
    Manifest manifest = jarFile.getManifest();
    assertThat(manifest.getMainAttributes().getValue("Infra-App-Native-Processed")).isEqualTo("true");
  }

  private String projectPath(String path) {
    try {
      return new File(this.gradleBuild.getProjectDir(), path).getCanonicalPath();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void writeDummyApplicationAotProcessorMainClass() {
    File examplePackage = new File(this.gradleBuild.getProjectDir(), "src/main/java/cn/taketoday/framework");
    examplePackage.mkdirs();
    File main = new File(examplePackage, "ApplicationAotProcessor.java");
    try (PrintWriter writer = new PrintWriter(new FileWriter(main))) {
      writer.println("package cn.taketoday.framework;");
      writer.println();
      writer.println("import java.io.IOException;");
      writer.println();
      writer.println("public class ApplicationAotProcessor {");
      writer.println();
      writer.println("    public static void main(String[] args) {");
      writer.println("    }");
      writer.println();
      writer.println("}");
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  protected List<String> getEntryNames(File file) throws IOException {
    List<String> entryNames = new ArrayList<>();
    try (JarFile jarFile = new JarFile(file)) {
      Enumeration<JarEntry> entries = jarFile.entries();
      while (entries.hasMoreElements()) {
        entryNames.add(entries.nextElement().getName());
      }
    }
    return entryNames;
  }

}
