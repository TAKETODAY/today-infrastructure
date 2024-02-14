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

package cn.taketoday.buildpack.platform.build;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DirectoryBuildpack}.
 *
 * @author Scott Frederick
 */
@DisabledOnOs(OS.WINDOWS)
class DirectoryBuildpackTests {

  @TempDir
  File temp;

  private File buildpackDir;

  private BuildpackResolverContext resolverContext;

  @BeforeEach
  void setUp() {
    this.buildpackDir = new File(this.temp, "buildpack");
    this.buildpackDir.mkdirs();
    this.resolverContext = mock(BuildpackResolverContext.class);
  }

  @Test
  void resolveWhenPath() throws Exception {
    writeBuildpackDescriptor();
    writeScripts();
    BuildpackReference reference = BuildpackReference.of(this.buildpackDir.toString());
    Buildpack buildpack = DirectoryBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNotNull();
    assertThat(buildpack.getCoordinates()).hasToString("example/buildpack1@0.0.1");
    assertHasExpectedLayers(buildpack);
  }

  @Test
  void resolveWhenFileUrl() throws Exception {
    writeBuildpackDescriptor();
    writeScripts();
    BuildpackReference reference = BuildpackReference.of("file://" + this.buildpackDir.toString());
    Buildpack buildpack = DirectoryBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNotNull();
    assertThat(buildpack.getCoordinates()).hasToString("example/buildpack1@0.0.1");
    assertHasExpectedLayers(buildpack);
  }

  @Test
  void resolveWhenDirectoryWithoutBuildpackTomlThrowsException() throws Exception {
    Files.createDirectories(this.buildpackDir.toPath());
    BuildpackReference reference = BuildpackReference.of(this.buildpackDir.toString());
    assertThatIllegalArgumentException()
            .isThrownBy(() -> DirectoryBuildpack.resolve(this.resolverContext, reference))
            .withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
            .withMessageContaining(this.buildpackDir.getAbsolutePath());
  }

  @Test
  void resolveWhenFileReturnsNull() throws Exception {
    Path file = Files.createFile(Paths.get(this.buildpackDir.toString(), "test"));
    BuildpackReference reference = BuildpackReference.of(file.toString());
    Buildpack buildpack = DirectoryBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNull();
  }

  @Test
  void resolveWhenDirectoryDoesNotExistReturnsNull() {
    BuildpackReference reference = BuildpackReference.of("/test/a/missing/buildpack");
    Buildpack buildpack = DirectoryBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNull();
  }

  @Test
  void locateDirectoryAsUrlThatDoesNotExistThrowsException() {
    BuildpackReference reference = BuildpackReference.of("file:///test/a/missing/buildpack");
    Buildpack buildpack = DirectoryBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNull();
  }

  private void assertHasExpectedLayers(Buildpack buildpack) throws IOException {
    List<ByteArrayOutputStream> layers = new ArrayList<>();
    buildpack.apply((layer) -> {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      layer.writeTo(out);
      layers.add(out);
    });
    assertThat(layers).hasSize(1);
    byte[] content = layers.get(0).toByteArray();
    try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(content))) {
      List<TarArchiveEntry> entries = new ArrayList<>();
      TarArchiveEntry entry = tar.getNextEntry();
      while (entry != null) {
        entries.add(entry);
        entry = tar.getNextEntry();
      }
      assertThat(entries).extracting("name", "mode")
              .containsExactlyInAnyOrder(tuple("/cnb/", 0755), tuple("/cnb/buildpacks/", 0755),
                      tuple("/cnb/buildpacks/example_buildpack1/", 0755),
                      tuple("/cnb/buildpacks/example_buildpack1/0.0.1/", 0755),
                      tuple("/cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml", 0644),
                      tuple("/cnb/buildpacks/example_buildpack1/0.0.1/bin/", 0755),
                      tuple("/cnb/buildpacks/example_buildpack1/0.0.1/bin/detect", 0744),
                      tuple("/cnb/buildpacks/example_buildpack1/0.0.1/bin/build", 0744));
    }
  }

  private void writeBuildpackDescriptor() throws IOException {
    Path descriptor = Files.createFile(Paths.get(this.buildpackDir.getAbsolutePath(), "buildpack.toml"),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")));
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(descriptor))) {
      writer.println("[buildpack]");
      writer.println("id = \"example/buildpack1\"");
      writer.println("version = \"0.0.1\"");
      writer.println("name = \"Example buildpack\"");
      writer.println("homepage = \"https://github.com/example/example-buildpack\"");
      writer.println("[[stacks]]");
      writer.println("id = \"io.buildpacks.stacks.bionic\"");
    }
  }

  private void writeScripts() throws IOException {
    Path binDirectory = Files.createDirectory(Paths.get(this.buildpackDir.getAbsolutePath(), "bin"),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x")));
    binDirectory.toFile().mkdirs();
    Path detect = Files.createFile(Paths.get(binDirectory.toString(), "detect"),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--")));
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(detect))) {
      writer.println("#!/usr/bin/env bash");
      writer.println("echo \"---> detect\"");
    }
    Path build = Files.createFile(Paths.get(binDirectory.toString(), "build"),
            PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr--r--")));
    try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(build))) {
      writer.println("#!/usr/bin/env bash");
      writer.println("echo \"---> build\"");
    }
  }

}
