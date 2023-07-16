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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility to create test tgz files.
 *
 * @author Scott Frederick
 */
class TestTarGzip {

  private final File buildpackDir;

  TestTarGzip(File buildpackDir) {
    this.buildpackDir = buildpackDir;
  }

  Path createArchive() throws Exception {
    return createArchive(true);
  }

  Path createEmptyArchive() throws Exception {
    return createArchive(false);
  }

  private Path createArchive(boolean addContent) throws Exception {
    Path path = Paths.get(this.buildpackDir.getAbsolutePath(), "buildpack.tar");
    Path archive = Files.createFile(path);
    if (addContent) {
      writeBuildpackContentToArchive(archive);
    }
    return compressBuildpackArchive(archive);
  }

  private Path compressBuildpackArchive(Path archive) throws Exception {
    Path tgzPath = Paths.get(this.buildpackDir.getAbsolutePath(), "buildpack.tgz");
    FileCopyUtils.copy(Files.newInputStream(archive),
            new GzipCompressorOutputStream(Files.newOutputStream(tgzPath)));
    return tgzPath;
  }

  private void writeBuildpackContentToArchive(Path archive) throws Exception {
    StringBuilder buildpackToml = new StringBuilder();
    buildpackToml.append("[buildpack]\n");
    buildpackToml.append("id = \"example/buildpack1\"\n");
    buildpackToml.append("version = \"0.0.1\"\n");
    buildpackToml.append("name = \"Example buildpack\"\n");
    buildpackToml.append("homepage = \"https://github.com/example/example-buildpack\"\n");
    buildpackToml.append("[[stacks]]\n");
    buildpackToml.append("id = \"io.buildpacks.stacks.bionic\"\n");
    String detectScript = """
            #!/usr/bin/env bash
            echo "---> detect"
            """;
    String buildScript = """
            #!/usr/bin/env bash
            echo "---> build"
            """;
    try (TarArchiveOutputStream tar = new TarArchiveOutputStream(Files.newOutputStream(archive))) {
      writeEntry(tar, "buildpack.toml", buildpackToml.toString());
      writeEntry(tar, "bin/");
      writeEntry(tar, "bin/detect", detectScript);
      writeEntry(tar, "bin/build", buildScript);
      tar.finish();
    }
  }

  private void writeEntry(TarArchiveOutputStream tar, String entryName) throws IOException {
    TarArchiveEntry entry = new TarArchiveEntry(entryName);
    tar.putArchiveEntry(entry);
    tar.closeArchiveEntry();
  }

  private void writeEntry(TarArchiveOutputStream tar, String entryName, String content) throws IOException {
    TarArchiveEntry entry = new TarArchiveEntry(entryName);
    entry.setSize(content.length());
    tar.putArchiveEntry(entry);
    IOUtils.copy(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), tar);
    tar.closeArchiveEntry();
  }

  void assertHasExpectedLayers(Buildpack buildpack) throws IOException {
    List<ByteArrayOutputStream> layers = new ArrayList<>();
    buildpack.apply((layer) -> {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      layer.writeTo(out);
      layers.add(out);
    });
    assertThat(layers).hasSize(1);
    byte[] content = layers.get(0).toByteArray();
    try (TarArchiveInputStream tar = new TarArchiveInputStream(new ByteArrayInputStream(content))) {
      assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/");
      assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/");
      assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/");
      assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/");
      assertThat(tar.getNextEntry().getName())
              .isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/buildpack.toml");
      assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/");
      assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/detect");
      assertThat(tar.getNextEntry().getName()).isEqualTo("cnb/buildpacks/example_buildpack1/0.0.1/bin/build");
      assertThat(tar.getNextEntry()).isNull();
    }
  }

}
