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

package cn.taketoday.maven;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.Random;
import java.util.stream.IntStream;

import cn.taketoday.buildpack.platform.docker.DockerApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.VolumeApi;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageName;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.test.junit.DisabledOnOs;
import cn.taketoday.test.testcontainers.DisabledIfDockerUnavailable;
import cn.taketoday.util.FileSystemUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's image support.
 *
 * @author Stephane Nicoll
 * @author Scott Frederick
 * @author Rafael Ceccone
 */
@ExtendWith(MavenBuildExtension.class)
@DisabledIfDockerUnavailable
@DisabledOnOs(os = { OS.LINUX, OS.MAC }, architecture = "aarch64",
              disabledReason = "The builder image has no ARM support")
class BuildImageTests extends AbstractArchiveIntegrationTests {

  @TestTemplate
  void whenBuildImageIsInvokedWithoutRepackageTheArchiveIsRepackagedOnTheFly(MavenBuild mavenBuild) {
    mavenBuild.project("build-image")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File jar = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar).isFile();
              File original = new File(project, "target/build-image-0.0.1.BUILD-SNAPSHOT.jar.original");
              assertThat(original).doesNotExist();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedOnTheCommandLineWithoutRepackageTheArchiveIsRepackagedOnTheFly(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-cmd-line")
            .goals("infra:build-image")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File jar = new File(project, "target/build-image-cmd-line-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar).isFile();
              File original = new File(project, "target/build-image-cmd-line-0.0.1.BUILD-SNAPSHOT.jar.original");
              assertThat(original).doesNotExist();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-cmd-line:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-cmd-line", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithClassifierWithoutRepackageTheArchiveIsRepackagedOnTheFly(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-classifier")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File jar = new File(project, "target/build-image-classifier-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar).isFile();
              File classifier = new File(project, "target/build-image-classifier-0.0.1.BUILD-SNAPSHOT-test.jar");
              assertThat(classifier).doesNotExist();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-classifier:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-classifier", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithClassifierSourceWithoutRepackageTheArchiveIsRepackagedOnTheFly(
          MavenBuild mavenBuild) {
    mavenBuild.project("build-image-classifier-source")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File jar = new File(project, "target/build-image-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar");
              assertThat(jar).isFile();
              File original = new File(project,
                      "target/build-image-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original");
              assertThat(original).doesNotExist();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-classifier-source:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-classifier-source", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-with-repackage")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File jar = new File(project, "target/build-image-with-repackage-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar).isFile();
              File original = new File(project,
                      "target/build-image-with-repackage-0.0.1.BUILD-SNAPSHOT.jar.original");
              assertThat(original).isFile();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-with-repackage:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-with-repackage", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithClassifierAndRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-classifier-with-repackage")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File jar = new File(project, "target/build-image-classifier-with-repackage-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar).isFile();
              File original = new File(project,
                      "target/build-image-classifier-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar");
              assertThat(original).isFile();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-classifier-with-repackage:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-classifier-with-repackage", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithClassifierSourceAndRepackageTheExistingArchiveIsUsed(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-classifier-source-with-repackage")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File jar = new File(project,
                      "target/build-image-classifier-source-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar");
              assertThat(jar).isFile();
              File original = new File(project,
                      "target/build-image-classifier-source-with-repackage-0.0.1.BUILD-SNAPSHOT-test.jar.original");
              assertThat(original).isFile();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-classifier-source-with-repackage:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-classifier-source-with-repackage", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithWarPackaging(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-war-packaging")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              File war = new File(project, "target/build-image-war-packaging-0.0.1.BUILD-SNAPSHOT.war");
              assertThat(war).isFile();
              File original = new File(project, "target/build-image-war-packaging-0.0.1.BUILD-SNAPSHOT.war.original");
              assertThat(original).doesNotExist();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-war-packaging:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-war-packaging", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithCustomImageName(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-custom-name")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .systemProperty("infra.build-image.imageName", "example.com/test/property-ignored:pom-preferred")
            .execute((project) -> {
              File jar = new File(project, "target/build-image-custom-name-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar).isFile();
              File original = new File(project, "target/build-image-custom-name-0.0.1.BUILD-SNAPSHOT.jar.original");
              assertThat(original).doesNotExist();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("example.com/test/build-image:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("example.com/test/build-image", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithCommandLineParameters(MavenBuild mavenBuild) {
    mavenBuild.project("build-image")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .systemProperty("infra.build-image.imageName", "example.com/test/cmd-property-name:v1")
            .systemProperty("infra.build-image.builder",
                    "projects.registry.vmware.com/springboot/spring-boot-cnb-builder:0.0.2")
            .systemProperty("infra.build-image.runImage", "projects.registry.vmware.com/springboot/run:tiny-cnb")
            .systemProperty("infra.build-image.createdDate", "2020-07-01T12:34:56Z")
            .systemProperty("infra.build-image.applicationDirectory", "/application")
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("example.com/test/cmd-property-name:v1")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              Image image = new DockerApi().image()
                      .inspect(ImageReference.of("example.com/test/cmd-property-name:v1"));
              assertThat(image.getCreated()).isEqualTo("2020-07-01T12:34:56Z");
              removeImage("example.com/test/cmd-property-name", "v1");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithCustomBuilderImageAndRunImage(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-custom-builder")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-v2-builder:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("docker.io/library/build-image-v2-builder", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithEmptyEnvEntry(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-empty-env-entry")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .prepare(this::writeLongNameResource)
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-empty-env-entry:0.0.1.BUILD-SNAPSHOT")
                      .contains("---> Test Info buildpack building")
                      .contains("---> Test Info buildpack done")
                      .contains("Successfully built image");
              removeImage("build-image-empty-env-entry", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithZipPackaging(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-zip-packaging")
            .goals("package")
            .prepare(this::writeLongNameResource)
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .execute((project) -> {
              File jar = new File(project, "target/build-image-zip-packaging-0.0.1.BUILD-SNAPSHOT.jar");
              assertThat(jar).isFile();
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-zip-packaging:0.0.1.BUILD-SNAPSHOT")
                      .contains("Main-Class: cn.taketoday.app.loader.PropertiesLauncher")
                      .contains("Successfully built image");
              removeImage("build-image-zip-packaging", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithBuildpacks(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-custom-buildpacks")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-custom-buildpacks:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image");
              removeImage("build-image-custom-buildpacks", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithBinding(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-bindings")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-bindings:0.0.1.BUILD-SNAPSHOT")
                      .contains("binding: ca-certificates/type=ca-certificates")
                      .contains("binding: ca-certificates/test.crt=---certificate one---")
                      .contains("Successfully built image");
              removeImage("build-image-bindings", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithNetworkModeNone(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-network")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-network:0.0.1.BUILD-SNAPSHOT")
                      .contains("Network status: curl failed")
                      .contains("Successfully built image");
              removeImage("build-image-network", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedOnMultiModuleProjectWithPackageGoal(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-multi-module")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-multi-module-app:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image");
              removeImage("build-image-multi-module-app", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithTags(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-tags")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-tags:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image")
                      .contains("docker.io/library/build-image-tags:latest")
                      .contains("Successfully created image tag");
              removeImage("build-image-tags", "0.0.1.BUILD-SNAPSHOT");
              removeImage("build-image-tags", "latest");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithVolumeCaches(MavenBuild mavenBuild) {
    String testBuildId = randomString();
    mavenBuild.project("build-image-volume-caches")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .systemProperty("test-build-id", testBuildId)
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-volume-caches:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image");
              removeImage("build-image-volume-caches", "0.0.1.BUILD-SNAPSHOT");
              deleteVolumes("cache-" + testBuildId + ".build", "cache-" + testBuildId + ".launch");
            });
  }

  @TestTemplate
  @EnabledOnOs(value = OS.LINUX, disabledReason =
          "Works with Docker Engine on Linux but is not reliable with Docker Desktop on other OSs")
  void whenBuildImageIsInvokedWithBindCaches(MavenBuild mavenBuild) {
    String testBuildId = randomString();
    mavenBuild.project("build-image-bind-caches")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .systemProperty("test-build-id", testBuildId)
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-bind-caches:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image");
              removeImage("build-image-bind-caches", "0.0.1.BUILD-SNAPSHOT");
              Path buildCachePath = ApplicationTemp.createDirectory("junit-image-cache-" + testBuildId + "-build");
              Path launchCachePath = ApplicationTemp.createDirectory("junit-image-cache-" + testBuildId + "-launch");
              assertThat(buildCachePath).exists().isDirectory();
              assertThat(launchCachePath).exists().isDirectory();
              FileSystemUtils.deleteRecursively(buildCachePath);
              FileSystemUtils.deleteRecursively(launchCachePath);
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithCreatedDate(MavenBuild mavenBuild) {
    String testBuildId = randomString();
    mavenBuild.project("build-image-created-date")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .systemProperty("test-build-id", testBuildId)
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-created-date:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image");
              Image image = new DockerApi().image()
                      .inspect(ImageReference.of("docker.io/library/build-image-created-date:0.0.1.BUILD-SNAPSHOT"));
              assertThat(image.getCreated()).isEqualTo("2020-07-01T12:34:56Z");
              removeImage("build-image-created-date", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithCurrentCreatedDate(MavenBuild mavenBuild) {
    String testBuildId = randomString();
    mavenBuild.project("build-image-current-created-date")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .systemProperty("test-build-id", testBuildId)
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-current-created-date:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image");
              Image image = new DockerApi().image()
                      .inspect(ImageReference
                              .of("docker.io/library/build-image-current-created-date:0.0.1.BUILD-SNAPSHOT"));
              OffsetDateTime createdDateTime = OffsetDateTime.parse(image.getCreated());
              OffsetDateTime current = OffsetDateTime.now().withOffsetSameInstant(createdDateTime.getOffset());
              assertThat(createdDateTime.getYear()).isEqualTo(current.getYear());
              assertThat(createdDateTime.getMonth()).isEqualTo(current.getMonth());
              assertThat(createdDateTime.getDayOfMonth()).isEqualTo(current.getDayOfMonth());
              removeImage("build-image-current-created-date", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void whenBuildImageIsInvokedWithApplicationDirectory(MavenBuild mavenBuild) {
    String testBuildId = randomString();
    mavenBuild.project("build-image-app-dir")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .systemProperty("test-build-id", testBuildId)
            .execute((project) -> {
              assertThat(buildLog(project)).contains("Building image")
                      .contains("docker.io/library/build-image-app-dir:0.0.1.BUILD-SNAPSHOT")
                      .contains("Successfully built image");
              removeImage("build-image-app-dir", "0.0.1.BUILD-SNAPSHOT");
            });
  }

  @TestTemplate
  void failsWhenBuildImageIsInvokedOnMultiModuleProjectWithBuildImageGoal(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-multi-module")
            .goals("infra:build-image")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .executeAndFail((project) -> assertThat(buildLog(project)).contains("Error packaging archive for image"));
  }

  @TestTemplate
  void failsWhenBuilderFails(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-builder-error")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .executeAndFail((project) -> assertThat(buildLog(project)).contains("Building image")
                    .contains("---> Test Info buildpack building")
                    .contains("Forced builder failure")
                    .containsPattern("Builder lifecycle '.*' failed with status code"));
  }

  @TestTemplate
  void failsWithBuildpackNotInBuilder(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-bad-buildpack")
            .goals("package")
            .systemProperty("infra.build-image.pullPolicy", "IF_NOT_PRESENT")
            .executeAndFail((project) -> assertThat(buildLog(project))
                    .contains("'urn:cnb:builder:example/does-not-exist:0.0.1' not found in builder"));
  }

  @TestTemplate
  void failsWhenFinalNameIsMisconfigured(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-final-name")
            .goals("package")
            .executeAndFail((project) -> assertThat(buildLog(project)).contains("final-name.jar.original")
                    .contains("is required for building an image"));
  }

  @TestTemplate
  void failsWhenCachesAreConfiguredTwice(MavenBuild mavenBuild) {
    mavenBuild.project("build-image-caches-multiple")
            .goals("package")
            .executeAndFail((project) -> assertThat(buildLog(project))
                    .contains("Each image building cache can be configured only once"));
  }

  private void writeLongNameResource(File project) {
    StringBuilder name = new StringBuilder();
    new Random().ints('a', 'z' + 1).limit(128).forEach((i) -> name.append((char) i));
    try {
      Path path = project.toPath().resolve(Paths.get("src", "main", "resources", name.toString()));
      Files.createDirectories(path.getParent());
      Files.createFile(path);
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void removeImage(String name, String version) {
    ImageReference imageReference = ImageReference.of(ImageName.of(name), version);
    try {
      new DockerApi().image().remove(imageReference, false);
    }
    catch (IOException ex) {
      throw new IllegalStateException("Failed to remove docker image " + imageReference, ex);
    }
  }

  private void deleteVolumes(String... names) throws IOException {
    VolumeApi volumeApi = new DockerApi().volume();
    for (String name : names) {
      volumeApi.delete(VolumeName.of(name), false);
    }
  }

  private String randomString() {
    IntStream chars = new Random().ints('a', 'z' + 1).limit(10);
    return chars.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
  }

}
