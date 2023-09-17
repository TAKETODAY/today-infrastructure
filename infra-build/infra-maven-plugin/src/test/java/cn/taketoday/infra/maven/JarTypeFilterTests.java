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

package cn.taketoday.infra.maven;

import org.apache.maven.artifact.Artifact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JarTypeFilter}.
 *
 * @author Andy Wilkinson
 */
class JarTypeFilterTests {

  @TempDir
  Path temp;

  @Test
  void whenArtifactHasNoJarTypeThenItIsIncluded() {
    assertThat(new JarTypeFilter().filter(createArtifact(null))).isFalse();
  }

  @Test
  void whenArtifactHasJarTypeThatIsNotExcludedThenItIsIncluded() {
    assertThat(new JarTypeFilter().filter(createArtifact("something-included"))).isFalse();
  }

  @Test
  void whenArtifactHasDependenciesStarterJarTypeThenItIsExcluded() {
    assertThat(new JarTypeFilter().filter(createArtifact("dependencies-starter"))).isTrue();
  }

  @Test
  void whenArtifactHasAnnotationProcessorJarTypeThenItIsExcluded() {
    assertThat(new JarTypeFilter().filter(createArtifact("annotation-processor"))).isTrue();
  }

  @Test
  void whenArtifactHasNoManifestFileThenItIsIncluded() {
    assertThat(new JarTypeFilter().filter(createArtifactWithNoManifest())).isFalse();
  }

  private Artifact createArtifact(String jarType) {
    Path jarPath = this.temp.resolve("test.jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
    if (jarType != null) {
      manifest.getMainAttributes().putValue("Infra-App-Jar-Type", jarType);
    }
    try {
      new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest).close();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return mockArtifact(jarPath);
  }

  private Artifact createArtifactWithNoManifest() {
    Path jarPath = this.temp.resolve("test.jar");
    try {
      new JarOutputStream(new FileOutputStream(jarPath.toFile())).close();
    }
    catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return mockArtifact(jarPath);
  }

  private Artifact mockArtifact(Path jarPath) {
    Artifact artifact = mock(Artifact.class);
    given(artifact.getFile()).willReturn(jarPath.toFile());
    return artifact;
  }

}
