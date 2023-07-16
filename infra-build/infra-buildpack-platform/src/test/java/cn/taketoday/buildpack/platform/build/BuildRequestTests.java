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

package cn.taketoday.buildpack.platform.build;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.buildpack.platform.docker.type.Binding;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.io.Owner;
import cn.taketoday.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link BuildRequest}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 */
class BuildRequestTests {

  @TempDir
  File tempDir;

  @Test
  void forJarFileReturnsRequest() throws IOException {
    File jarFile = new File(this.tempDir, "my-app-0.0.1.jar");
    writeTestJarFile(jarFile);
    BuildRequest request = BuildRequest.forJarFile(jarFile);
    assertThat(request.getName()).hasToString("docker.io/library/my-app:0.0.1");
    assertThat(request.getBuilder()).hasToString("docker.io/" + BuildRequest.DEFAULT_BUILDER_IMAGE_NAME);
    assertThat(request.getApplicationContent(Owner.ROOT)).satisfies(this::hasExpectedJarContent);
    assertThat(request.getEnv()).isEmpty();
  }

  @Test
  void forJarFileWithNameReturnsRequest() throws IOException {
    File jarFile = new File(this.tempDir, "my-app-0.0.1.jar");
    writeTestJarFile(jarFile);
    BuildRequest request = BuildRequest.forJarFile(ImageReference.of("test-app"), jarFile);
    assertThat(request.getName()).hasToString("docker.io/library/test-app:latest");
    assertThat(request.getBuilder()).hasToString("docker.io/" + BuildRequest.DEFAULT_BUILDER_IMAGE_NAME);
    assertThat(request.getApplicationContent(Owner.ROOT)).satisfies(this::hasExpectedJarContent);
    assertThat(request.getEnv()).isEmpty();
  }

  @Test
  void forJarFileWhenJarFileIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildRequest.forJarFile(null))
            .withMessage("JarFile must not be null");
  }

  @Test
  void forJarFileWhenJarFileIsMissingThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> BuildRequest.forJarFile(new File(this.tempDir, "missing.jar")))
            .withMessage("JarFile must exist");
  }

  @Test
  void forJarFileWhenJarFileIsDirectoryThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> BuildRequest.forJarFile(this.tempDir))
            .withMessage("JarFile must be a file");
  }

  @Test
  void withBuilderUpdatesBuilder() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"))
            .withBuilder(ImageReference.of("spring/builder"));
    assertThat(request.getBuilder()).hasToString("docker.io/spring/builder:latest");
  }

  @Test
  void withBuilderWhenHasDigestUpdatesBuilder() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"))
            .withBuilder(ImageReference
                    .of("spring/builder@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d"));
    assertThat(request.getBuilder()).hasToString(
            "docker.io/spring/builder@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
  }

  @Test
  void withRunImageUpdatesRunImage() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"))
            .withRunImage(ImageReference.of("example.com/custom/run-image:latest"));
    assertThat(request.getRunImage()).hasToString("example.com/custom/run-image:latest");
  }

  @Test
  void withRunImageWhenHasDigestUpdatesRunImage() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"))
            .withRunImage(ImageReference
                    .of("example.com/custom/run-image@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d"));
    assertThat(request.getRunImage()).hasToString(
            "example.com/custom/run-image@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
  }

  @Test
  void withCreatorUpdatesCreator() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withCreator = request.withCreator(Creator.withVersion("1.0.0"));
    assertThat(request.getCreator().getName()).isEqualTo("Spring Boot");
    assertThat(request.getCreator().getVersion()).isEmpty();
    assertThat(withCreator.getCreator().getName()).isEqualTo("Spring Boot");
    assertThat(withCreator.getCreator().getVersion()).isEqualTo("1.0.0");
  }

  @Test
  void withEnvAddsEnvEntry() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withEnv = request.withEnv("spring", "boot");
    assertThat(request.getEnv()).isEmpty();
    assertThat(withEnv.getEnv()).containsExactly(entry("spring", "boot"));
  }

  @Test
  void withEnvMapAddsEnvEntries() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    Map<String, String> env = new LinkedHashMap<>();
    env.put("spring", "boot");
    env.put("test", "test");
    BuildRequest withEnv = request.withEnv(env);
    assertThat(request.getEnv()).isEmpty();
    assertThat(withEnv.getEnv()).containsExactly(entry("spring", "boot"), entry("test", "test"));
  }

  @Test
  void withEnvWhenKeyIsNullThrowsException() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withEnv(null, "test"))
            .withMessage("Name must not be empty");
  }

  @Test
  void withEnvWhenValueIsNullThrowsException() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withEnv("test", null))
            .withMessage("Value must not be empty");
  }

  @Test
  void withBuildpacksAddsBuildpacks() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildpackReference buildpackReference1 = BuildpackReference.of("example/buildpack1");
    BuildpackReference buildpackReference2 = BuildpackReference.of("example/buildpack2");
    BuildRequest withBuildpacks = request.withBuildpacks(buildpackReference1, buildpackReference2);
    assertThat(request.getBuildpacks()).isEmpty();
    assertThat(withBuildpacks.getBuildpacks()).containsExactly(buildpackReference1, buildpackReference2);
  }

  @Test
  void withBuildpacksWhenBuildpacksIsNullThrowsException() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withBuildpacks((List<BuildpackReference>) null))
            .withMessage("Buildpacks must not be null");
  }

  @Test
  void withBindingsAddsBindings() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withBindings = request.withBindings(Binding.of("/host/path:/container/path:ro"),
            Binding.of("volume-name:/container/path:rw"));
    assertThat(request.getBindings()).isEmpty();
    assertThat(withBindings.getBindings()).containsExactly(Binding.of("/host/path:/container/path:ro"),
            Binding.of("volume-name:/container/path:rw"));
  }

  @Test
  void withBindingsWhenBindingsIsNullThrowsException() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withBindings((List<Binding>) null))
            .withMessage("Bindings must not be null");
  }

  @Test
  void withNetworkUpdatesNetwork() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar")).withNetwork("test");
    assertThat(request.getNetwork()).isEqualTo("test");
  }

  @Test
  void withTagsAddsTags() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withTags = request.withTags(ImageReference.of("docker.io/library/my-app:latest"),
            ImageReference.of("example.com/custom/my-app:0.0.1"),
            ImageReference.of("example.com/custom/my-app:latest"));
    assertThat(request.getTags()).isEmpty();
    assertThat(withTags.getTags()).containsExactly(ImageReference.of("docker.io/library/my-app:latest"),
            ImageReference.of("example.com/custom/my-app:0.0.1"),
            ImageReference.of("example.com/custom/my-app:latest"));
  }

  @Test
  void withTagsWhenTagsIsNullThrowsException() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withTags((List<ImageReference>) null))
            .withMessage("Tags must not be null");
  }

  @Test
  void withBuildVolumeCacheAddsCache() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withCache = request.withBuildCache(Cache.volume("build-volume"));
    assertThat(request.getBuildCache()).isNull();
    assertThat(withCache.getBuildCache()).isEqualTo(Cache.volume("build-volume"));
  }

  @Test
  void withBuildVolumeCacheWhenCacheIsNullThrowsException() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withBuildCache(null))
            .withMessage("BuildCache must not be null");
  }

  @Test
  void withLaunchVolumeCacheAddsCache() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withCache = request.withLaunchCache(Cache.volume("launch-volume"));
    assertThat(request.getLaunchCache()).isNull();
    assertThat(withCache.getLaunchCache()).isEqualTo(Cache.volume("launch-volume"));
  }

  @Test
  void withLaunchVolumeCacheWhenCacheIsNullThrowsException() throws IOException {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withLaunchCache(null))
            .withMessage("LaunchCache must not be null");
  }

  @Test
  void withCreatedDateSetsCreatedDate() throws Exception {
    Instant createDate = Instant.now();
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withCreatedDate = request.withCreatedDate(createDate.toString());
    assertThat(withCreatedDate.getCreatedDate()).isEqualTo(createDate);
  }

  @Test
  void withCreatedDateNowSetsCreatedDate() throws Exception {
    OffsetDateTime now = OffsetDateTime.now();
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withCreatedDate = request.withCreatedDate("now");
    OffsetDateTime createdDate = OffsetDateTime.ofInstant(withCreatedDate.getCreatedDate(), ZoneId.of("UTC"));
    assertThat(createdDate.getYear()).isEqualTo(now.getYear());
    assertThat(createdDate.getMonth()).isEqualTo(now.getMonth());
    assertThat(createdDate.getDayOfMonth()).isEqualTo(now.getDayOfMonth());
    withCreatedDate = request.withCreatedDate("NOW");
    createdDate = OffsetDateTime.ofInstant(withCreatedDate.getCreatedDate(), ZoneId.of("UTC"));
    assertThat(createdDate.getYear()).isEqualTo(now.getYear());
    assertThat(createdDate.getMonth()).isEqualTo(now.getMonth());
    assertThat(createdDate.getDayOfMonth()).isEqualTo(now.getDayOfMonth());
  }

  @Test
  void withCreatedDateAndInvalidDateThrowsException() throws Exception {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    assertThatIllegalArgumentException().isThrownBy(() -> request.withCreatedDate("not a date"))
            .withMessageContaining("'not a date'");
  }

  @Test
  void withApplicationDirectorySetsApplicationDirectory() throws Exception {
    BuildRequest request = BuildRequest.forJarFile(writeTestJarFile("my-app-0.0.1.jar"));
    BuildRequest withAppDir = request.withApplicationDirectory("/application");
    assertThat(withAppDir.getApplicationDirectory()).isEqualTo("/application");
  }

  private void hasExpectedJarContent(TarArchive archive) {
    try {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      archive.writeTo(outputStream);
      try (TarArchiveInputStream tar = new TarArchiveInputStream(
              new ByteArrayInputStream(outputStream.toByteArray()))) {
        assertThat(tar.getNextEntry().getName()).isEqualTo("spring/");
        assertThat(tar.getNextEntry().getName()).isEqualTo("spring/boot");
        assertThat(tar.getNextEntry()).isNull();
      }
    }
    catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  private File writeTestJarFile(String name) throws IOException {
    File file = new File(this.tempDir, name);
    writeTestJarFile(file);
    return file;
  }

  private void writeTestJarFile(File file) throws IOException {
    try (ZipArchiveOutputStream zip = new ZipArchiveOutputStream(file)) {
      ZipArchiveEntry dirEntry = new ZipArchiveEntry("spring/");
      zip.putArchiveEntry(dirEntry);
      zip.closeArchiveEntry();
      ZipArchiveEntry fileEntry = new ZipArchiveEntry("spring/boot");
      zip.putArchiveEntry(fileEntry);
      zip.write("test".getBytes(StandardCharsets.UTF_8));
      zip.closeArchiveEntry();
    }
  }

}
