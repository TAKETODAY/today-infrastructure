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

package cn.taketoday.gradle.docs;

import org.gradle.testkit.runner.BuildResult;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import cn.taketoday.gradle.junit.GradleMultiDslExtension;
import cn.taketoday.gradle.testkit.GradleBuild;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the packaging documentation.
 *
 * @author Andy Wilkinson
 * @author Jean-Baptiste Nizet
 * @author Scott Frederick
 */
@ExtendWith(GradleMultiDslExtension.class)
class PackagingDocumentationTests {

  GradleBuild gradleBuild;

  @TestTemplate
  void warContainerDependencyEvaluatesSuccessfully() {
    this.gradleBuild.script("src/docs/gradle/packaging/war-container-dependency").build();
  }

  @TestTemplate
  void infraJarMainClass() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-main-class").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
              .isEqualTo("com.example.ExampleApplication");
    }
  }

  @TestTemplate
  void infraJarManifestMainClass() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-manifest-main-class").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
              .isEqualTo("com.example.ExampleApplication");
    }
  }

  @TestTemplate
  void applicationPluginMainClass() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/application-plugin-main-class").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
              .isEqualTo("com.example.ExampleApplication");
    }
  }

  @TestTemplate
  void infraApplicationDslMainClass() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-application-dsl-main-class").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      assertThat(jar.getManifest().getMainAttributes().getValue("Start-Class"))
              .isEqualTo("com.example.ExampleApplication");
    }
  }

  @TestTemplate
  void infraWarIncludeDevtools() throws IOException {
    jarFile(new File(this.gradleBuild.getProjectDir(), "spring-boot-devtools-1.2.3.RELEASE.jar"));
    this.gradleBuild.script("src/docs/gradle/packaging/infra-war-include-devtools").build("infraWar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".war");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      assertThat(jar.getEntry("WEB-INF/lib/spring-boot-devtools-1.2.3.RELEASE.jar")).isNotNull();
    }
  }

  @TestTemplate
  void infraJarRequiresUnpack() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-requires-unpack").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      JarEntry entry = jar.getJarEntry("APP-INF/lib/jruby-complete-1.7.25.jar");
      assertThat(entry).isNotNull();
      assertThat(entry.getComment()).startsWith("UNPACK:");
    }
  }

  @TestTemplate
  void infraJarIncludeLaunchScript() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-include-launch-script").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    assertThat(FileCopyUtils.copyToString(new FileReader(file))).startsWith("#!/bin/bash");
  }

  @TestTemplate
  void infraJarLaunchScriptProperties() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-launch-script-properties").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    assertThat(FileCopyUtils.copyToString(new FileReader(file))).contains("example-app.log");
  }

  @TestTemplate
  void infraJarCustomLaunchScript() throws IOException {
    File customScriptFile = new File(this.gradleBuild.getProjectDir(), "src/custom.script");
    customScriptFile.getParentFile().mkdirs();
    FileCopyUtils.copy("custom", new FileWriter(customScriptFile));
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-custom-launch-script").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    assertThat(FileCopyUtils.copyToString(new FileReader(file))).startsWith("custom");
  }

  @TestTemplate
  void infraWarPropertiesLauncher() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-war-properties-launcher").build("infraWar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".war");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      assertThat(jar.getManifest().getMainAttributes().getValue("Main-Class"))
              .isEqualTo("cn.taketoday.app.loader.PropertiesLauncher");
    }
  }

  @TestTemplate
  void onlyInfraJar() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/only-infra-jar").build("assemble");
    File plainJar = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + "-plain.jar");
    assertThat(plainJar).doesNotExist();
    File infraJar = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(infraJar).isFile();
    try (JarFile jar = new JarFile(infraJar)) {
      assertThat(jar.getEntry("APP-INF/")).isNotNull();
    }
  }

  @TestTemplate
  void classifiedInfraJar() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-and-jar-classifiers").build("assemble");
    File plainJar = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(plainJar).isFile();
    try (JarFile jar = new JarFile(plainJar)) {
      assertThat(jar.getEntry("APP-INF/")).isNull();
    }
    File infraJar = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + "-boot.jar");
    assertThat(infraJar).isFile();
    try (JarFile jar = new JarFile(infraJar)) {
      assertThat(jar.getEntry("APP-INF/")).isNotNull();
    }
  }

  @TestTemplate
  void infraJarLayeredDisabled() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-layered-disabled").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      JarEntry entry = jar.getJarEntry("APP-INF/layers.idx");
      assertThat(entry).isNull();
    }
  }

  @TestTemplate
  void infraJarLayeredCustom() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-layered-custom").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      JarEntry entry = jar.getJarEntry("APP-INF/layers.idx");
      assertThat(entry).isNotNull();
      assertThat(Collections.list(jar.entries())
              .stream()
              .map(JarEntry::getName)
              .filter((name) -> name.startsWith("APP-INF/lib/infra"))).isNotEmpty();
    }
  }

  @TestTemplate
  void infraJarLayeredExcludeTools() throws IOException {
    this.gradleBuild.script("src/docs/gradle/packaging/infra-jar-layered-exclude-tools").build("infraJar");
    File file = new File(this.gradleBuild.getProjectDir(),
            "build/libs/" + this.gradleBuild.getProjectDir().getName() + ".jar");
    assertThat(file).isFile();
    try (JarFile jar = new JarFile(file)) {
      JarEntry entry = jar.getJarEntry("APP-INF/layers.idx");
      assertThat(entry).isNotNull();
      assertThat(Collections.list(jar.entries())
              .stream()
              .map(JarEntry::getName)
              .filter((name) -> name.startsWith("APP-INF/lib/spring-boot"))).isEmpty();
    }
  }

  @TestTemplate
  void infraBuildImageWithBuilder() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-builder")
            .build("infraBuildImageBuilder");
    assertThat(result.getOutput()).contains("builder=mine/java-cnb-builder").contains("runImage=mine/java-cnb-run");
  }

  @TestTemplate
  void infraBuildImageWithCustomBuildpackJvmVersion() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-env")
            .build("infraBuildImageEnvironment");
    assertThat(result.getOutput()).contains("BP_JVM_VERSION=17");
  }

  @TestTemplate
  void infraBuildImageWithCustomProxySettings() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-env-proxy")
            .build("infraBuildImageEnvironment");
    assertThat(result.getOutput()).contains("HTTP_PROXY=http://proxy.example.com")
            .contains("HTTPS_PROXY=https://proxy.example.com");
  }

  @TestTemplate
  void infraBuildImageWithCustomRuntimeConfiguration() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-env-runtime")
            .build("infraBuildImageEnvironment");
    assertThat(result.getOutput()).contains("BPE_DELIM_JAVA_TOOL_OPTIONS= ")
            .contains("BPE_APPEND_JAVA_TOOL_OPTIONS=-XX:+HeapDumpOnOutOfMemoryError");
  }

  @TestTemplate
  void infraBuildImageWithCustomImageName() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-name")
            .build("infraBuildImageName");
    assertThat(result.getOutput()).contains("example.com/library/" + this.gradleBuild.getProjectDir().getName());
  }

  @TestTemplate
  void infraBuildImageWithDockerHostMinikube() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-docker-host")
            .build("infraBuildImageDocker");
    assertThat(result.getOutput()).contains("host=tcp://192.168.99.100:2376")
            .contains("tlsVerify=true")
            .contains("certPath=/home/user/.minikube/certs");
  }

  @TestTemplate
  void infraBuildImageWithDockerHostPodman() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-docker-host-podman")
            .build("infraBuildImageDocker");
    assertThat(result.getOutput()).contains("host=unix:///run/user/1000/podman/podman.sock")
            .contains("bindHostToBuilder=true");
  }

  @TestTemplate
  void infraBuildImageWithDockerUserAuth() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-docker-auth-user")
            .build("infraBuildImageDocker");
    assertThat(result.getOutput()).contains("username=user")
            .contains("password=secret")
            .contains("url=https://docker.example.com/v1/")
            .contains("email=user@example.com");
  }

  @TestTemplate
  void infraBuildImageWithDockerTokenAuth() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-docker-auth-token")
            .build("infraBuildImageDocker");
    assertThat(result.getOutput()).contains("token=9cbaf023786cd7...");
  }

  @TestTemplate
  void infraBuildImagePublish() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-publish")
            .build("infraBuildImagePublish");
    assertThat(result.getOutput()).contains("true");
  }

  @TestTemplate
  void infraBuildImageWithBuildpacks() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-buildpacks")
            .build("infraBuildImageBuildpacks");
    assertThat(result.getOutput()).contains("file:///path/to/example-buildpack.tgz")
            .contains("urn:cnb:builder:paketo-buildpacks/java");
  }

  @TestTemplate
  void infraBuildImageWithCaches() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-caches")
            .build("infraBuildImageCaches");
    assertThat(result.getOutput()).containsPattern("buildCache=cache-gradle-[\\d]+.build")
            .containsPattern("launchCache=cache-gradle-[\\d]+.launch");
  }

  @TestTemplate
  void infraBuildImageWithBindCaches() {
    BuildResult result = this.gradleBuild.script("src/docs/gradle/packaging/infra-build-image-bind-caches")
            .build("infraBuildImageCaches");
    assertThat(result.getOutput()).containsPattern("buildWorkspace=/tmp/cache-gradle-[\\d]+.work")
            .containsPattern("buildCache=/tmp/cache-gradle-[\\d]+.build")
            .containsPattern("launchCache=/tmp/cache-gradle-[\\d]+.launch");
  }

  protected void jarFile(File file) throws IOException {
    try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file))) {
      jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
      new Manifest().write(jar);
      jar.closeEntry();
    }
  }

}
