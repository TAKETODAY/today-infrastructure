/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.gradle.docs;

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

import infra.gradle.junit.GradleMultiDslExtension;
import infra.gradle.testkit.GradleBuild;
import infra.util.FileCopyUtils;

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
      assertThat(entry.getComment()).startsWith("UNPACK");
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
              .isEqualTo("infra.app.loader.PropertiesLauncher");
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

  protected void jarFile(File file) throws IOException {
    try (JarOutputStream jar = new JarOutputStream(new FileOutputStream(file))) {
      jar.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
      new Manifest().write(jar);
      jar.closeEntry();
    }
  }

}
