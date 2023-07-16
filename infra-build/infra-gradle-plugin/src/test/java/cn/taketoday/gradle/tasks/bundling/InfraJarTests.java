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

package cn.taketoday.gradle.tasks.bundling;

import org.gradle.api.Action;
import org.gradle.api.JavaVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraJar}.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @author Paddy Drury
 */
class InfraJarTests extends AbstractInfraArchiveTests<InfraJar> {

  InfraJarTests() {
    super(InfraJar.class, "cn.taketoday.app.loader.JarLauncher", "APP-INF/lib/", "APP-INF/classes/",
            "APP-INF/");
  }

  @BeforeEach
  void setUp() {
    this.getTask().getTargetJavaVersion().set(JavaVersion.VERSION_17);
  }

  @Test
  void contentCanBeAddedToBootInfUsingCopySpecFromGetter() throws IOException {
    InfraJar infraJar = getTask();
    infraJar.getMainClass().set("com.example.Application");
    infraJar.getAppInf().into("test").from(new File("build.gradle").getAbsolutePath());
    infraJar.copy();
    try (JarFile jarFile = new JarFile(infraJar.getArchiveFile().get().getAsFile())) {
      assertThat(jarFile.getJarEntry("APP-INF/test/build.gradle")).isNotNull();
    }
  }

  @Test
  void contentCanBeAddedToBootInfUsingCopySpecAction() throws IOException {
    InfraJar infraJar = getTask();
    infraJar.getMainClass().set("com.example.Application");
    infraJar.appInf((copySpec) -> copySpec.into("test").from(new File("build.gradle").getAbsolutePath()));
    infraJar.copy();
    try (JarFile jarFile = new JarFile(infraJar.getArchiveFile().get().getAsFile())) {
      assertThat(jarFile.getJarEntry("APP-INF/test/build.gradle")).isNotNull();
    }
  }

  @Test
  void jarsInLibAreStored() throws IOException {
    try (JarFile jarFile = new JarFile(createLayeredJar())) {
      assertThat(jarFile.getEntry("APP-INF/lib/first-library.jar").getMethod()).isZero();
      assertThat(jarFile.getEntry("APP-INF/lib/second-library.jar").getMethod()).isZero();
      assertThat(jarFile.getEntry("APP-INF/lib/third-library-SNAPSHOT.jar").getMethod()).isZero();
      assertThat(jarFile.getEntry("APP-INF/lib/first-project-library.jar").getMethod()).isZero();
      assertThat(jarFile.getEntry("APP-INF/lib/second-project-library-SNAPSHOT.jar").getMethod()).isZero();
    }
  }

  @Test
  void whenJarIsLayeredClasspathIndexPointsToLayeredLibs() throws IOException {
    try (JarFile jarFile = new JarFile(createLayeredJar())) {
      assertThat(entryLines(jarFile, "APP-INF/classpath.idx")).containsExactly(
              "- \"APP-INF/lib/first-library.jar\"", "- \"APP-INF/lib/second-library.jar\"",
              "- \"APP-INF/lib/third-library-SNAPSHOT.jar\"", "- \"APP-INF/lib/fourth-library.jar\"",
              "- \"APP-INF/lib/first-project-library.jar\"",
              "- \"APP-INF/lib/second-project-library-SNAPSHOT.jar\"");
    }
  }

  @Test
  void classpathIndexPointsToBootInfLibs() throws IOException {
    try (JarFile jarFile = new JarFile(createPopulatedJar())) {
      assertThat(jarFile.getManifest().getMainAttributes().getValue("Infra-App-Classpath-Index"))
              .isEqualTo("APP-INF/classpath.idx");
      assertThat(entryLines(jarFile, "APP-INF/classpath.idx")).containsExactly(
              "- \"APP-INF/lib/first-library.jar\"", "- \"APP-INF/lib/second-library.jar\"",
              "- \"APP-INF/lib/third-library-SNAPSHOT.jar\"", "- \"APP-INF/lib/fourth-library.jar\"",
              "- \"APP-INF/lib/first-project-library.jar\"",
              "- \"APP-INF/lib/second-project-library-SNAPSHOT.jar\"");
    }
  }

  @Test
  void metaInfEntryIsPackagedInTheRootOfTheArchive() throws IOException {
    getTask().getMainClass().set("com.example.Main");
    File classpathDirectory = new File(this.temp, "classes");
    File metaInfEntry = new File(classpathDirectory, "META-INF/test");
    metaInfEntry.getParentFile().mkdirs();
    metaInfEntry.createNewFile();
    File applicationClass = new File(classpathDirectory, "com/example/Application.class");
    applicationClass.getParentFile().mkdirs();
    applicationClass.createNewFile();
    getTask().classpath(classpathDirectory);
    executeTask();
    try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
      assertThat(jarFile.getEntry("APP-INF/classes/com/example/Application.class")).isNotNull();
      assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
      assertThat(jarFile.getEntry("APP-INF/classes/META-INF/test")).isNull();
      assertThat(jarFile.getEntry("META-INF/test")).isNotNull();
    }
  }

  @Test
  void aopXmlIsPackagedBeneathClassesDirectory() throws IOException {
    getTask().getMainClass().set("com.example.Main");
    File classpathDirectory = new File(this.temp, "classes");
    File aopXml = new File(classpathDirectory, "META-INF/aop.xml");
    aopXml.getParentFile().mkdirs();
    aopXml.createNewFile();
    File applicationClass = new File(classpathDirectory, "com/example/Application.class");
    applicationClass.getParentFile().mkdirs();
    applicationClass.createNewFile();
    getTask().classpath(classpathDirectory);
    executeTask();
    try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
      assertThat(jarFile.getEntry("APP-INF/classes/com/example/Application.class")).isNotNull();
      assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
      assertThat(jarFile.getEntry("APP-INF/classes/META-INF/aop.xml")).isNotNull();
      assertThat(jarFile.getEntry("META-INF/aop.xml")).isNull();
    }
  }

  @Test
  void kotlinModuleIsPackagedBeneathClassesDirectory() throws IOException {
    getTask().getMainClass().set("com.example.Main");
    File classpathDirectory = new File(this.temp, "classes");
    File kotlinModule = new File(classpathDirectory, "META-INF/example.kotlin_module");
    kotlinModule.getParentFile().mkdirs();
    kotlinModule.createNewFile();
    File applicationClass = new File(classpathDirectory, "com/example/Application.class");
    applicationClass.getParentFile().mkdirs();
    applicationClass.createNewFile();
    getTask().classpath(classpathDirectory);
    executeTask();
    try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
      assertThat(jarFile.getEntry("APP-INF/classes/com/example/Application.class")).isNotNull();
      assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
      assertThat(jarFile.getEntry("APP-INF/classes/META-INF/example.kotlin_module")).isNotNull();
      assertThat(jarFile.getEntry("META-INF/example.kotlin_module")).isNull();
    }
  }

  @Test
  void metaInfServicesEntryIsPackagedBeneathClassesDirectory() throws IOException {
    getTask().getMainClass().set("com.example.Main");
    File classpathDirectory = new File(this.temp, "classes");
    File service = new File(classpathDirectory, "META-INF/services/com.example.Service");
    service.getParentFile().mkdirs();
    service.createNewFile();
    File applicationClass = new File(classpathDirectory, "com/example/Application.class");
    applicationClass.getParentFile().mkdirs();
    applicationClass.createNewFile();
    getTask().classpath(classpathDirectory);
    executeTask();
    try (JarFile jarFile = new JarFile(getTask().getArchiveFile().get().getAsFile())) {
      assertThat(jarFile.getEntry("APP-INF/classes/com/example/Application.class")).isNotNull();
      assertThat(jarFile.getEntry("com/example/Application.class")).isNull();
      assertThat(jarFile.getEntry("APP-INF/classes/META-INF/services/com.example.Service")).isNotNull();
      assertThat(jarFile.getEntry("META-INF/services/com.example.Service")).isNull();
    }
  }

  @Test
  void nativeImageArgFileWithExcludesIsWritten() throws IOException {
    try (JarFile jarFile = new JarFile(createLayeredJar(true))) {
      assertThat(entryLines(jarFile, "META-INF/native-image/argfile")).containsExactly("--exclude-config",
              "\\Qfirst-library.jar\\E", "^/META-INF/native-image/.*", "--exclude-config",
              "\\Qsecond-library.jar\\E", "^/META-INF/native-image/.*");
    }
  }

  @Test
  void nativeImageArgFileIsNotWrittenWhenExcludesAreEmpty() throws IOException {
    try (JarFile jarFile = new JarFile(createLayeredJar(false))) {
      assertThat(jarFile.getEntry("META-INF/native-image/argfile")).isNull();
    }
  }

  @Test
  void javaVersionIsWrittenToManifest() throws IOException {
    try (JarFile jarFile = new JarFile(createPopulatedJar())) {
      assertThat(jarFile.getManifest().getMainAttributes().getValue("Build-Jdk-Spec"))
              .isEqualTo(JavaVersion.VERSION_17.getMajorVersion());
    }
  }

  @Override
  void applyLayered(Action<LayeredSpec> action) {
    getTask().layered(action);
  }

  @Override
  protected void executeTask() {
    getTask().copy();
  }

}
