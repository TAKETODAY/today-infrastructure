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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TarGzipBuildpack}.
 *
 * @author Scott Frederick
 */
class TarGzipBuildpackTests {

  private File buildpackDir;

  private TestTarGzip testTarGzip;

  private BuildpackResolverContext resolverContext;

  @BeforeEach
  void setUp(@TempDir File temp) {
    this.buildpackDir = new File(temp, "buildpack");
    this.buildpackDir.mkdirs();
    this.testTarGzip = new TestTarGzip(this.buildpackDir);
    this.resolverContext = mock(BuildpackResolverContext.class);
  }

  @Test
  void resolveWhenFilePathReturnsBuildpack() throws Exception {
    Path compressedArchive = this.testTarGzip.createArchive();
    BuildpackReference reference = BuildpackReference.of(compressedArchive.toString());
    Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNotNull();
    assertThat(buildpack.getCoordinates()).hasToString("example/buildpack1@0.0.1");
    this.testTarGzip.assertHasExpectedLayers(buildpack);
  }

  @Test
  void resolveWhenFileUrlReturnsBuildpack() throws Exception {
    Path compressedArchive = this.testTarGzip.createArchive();
    BuildpackReference reference = BuildpackReference.of("file://" + compressedArchive.toString());
    Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNotNull();
    assertThat(buildpack.getCoordinates()).hasToString("example/buildpack1@0.0.1");
    this.testTarGzip.assertHasExpectedLayers(buildpack);
  }

  @Test
  void resolveWhenArchiveWithoutDescriptorThrowsException() throws Exception {
    Path compressedArchive = this.testTarGzip.createEmptyArchive();
    BuildpackReference reference = BuildpackReference.of(compressedArchive.toString());
    assertThatIllegalArgumentException().isThrownBy(() -> TarGzipBuildpack.resolve(this.resolverContext, reference))
            .withMessageContaining("Buildpack descriptor 'buildpack.toml' is required")
            .withMessageContaining(compressedArchive.toString());
  }

  @Test
  void resolveWhenArchiveWithDirectoryReturnsNull() {
    BuildpackReference reference = BuildpackReference.of(this.buildpackDir.getAbsolutePath());
    Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNull();
  }

  @Test
  void resolveWhenArchiveThatDoesNotExistReturnsNull() {
    BuildpackReference reference = BuildpackReference.of("/test/i/am/missing/buildpack.tar");
    Buildpack buildpack = TarGzipBuildpack.resolve(this.resolverContext, reference);
    assertThat(buildpack).isNull();
  }

}
