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

package infra.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import infra.app.loader.tools.Library;
import infra.app.loader.tools.LibraryCallback;
import infra.app.loader.tools.LibraryScope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link ArtifactsLibraries}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class ArtifactsLibrariesTests {

  @Mock
  private Artifact artifact;

  @Mock
  private ArtifactHandler artifactHandler;

  private Set<Artifact> artifacts;

  private final File file = new File(".");

  private ArtifactsLibraries libs;

  @Mock
  private LibraryCallback callback;

  @Captor
  private ArgumentCaptor<Library> libraryCaptor;

  @BeforeEach
  void setup() {
    this.artifacts = Collections.singleton(this.artifact);
    this.libs = new ArtifactsLibraries(this.artifacts, Collections.emptyList(), null, mock(Log.class));
    given(this.artifactHandler.getExtension()).willReturn("jar");
  }

  @Test
  void callbackForJars() throws Exception {
    given(this.artifact.getFile()).willReturn(this.file);
    given(this.artifact.getArtifactHandler()).willReturn(this.artifactHandler);
    given(this.artifact.getScope()).willReturn("compile");
    this.libs.doWithLibraries(this.callback);
    then(this.callback).should().library(assertArg((library) -> {
      assertThat(library.getFile()).isEqualTo(this.file);
      assertThat(library.getScope()).isEqualTo(LibraryScope.COMPILE);
      assertThat(library.isUnpackRequired()).isFalse();
    }));
  }

  @Test
  void callbackWithUnpack() throws Exception {
    given(this.artifact.getFile()).willReturn(this.file);
    given(this.artifact.getArtifactHandler()).willReturn(this.artifactHandler);
    given(this.artifact.getGroupId()).willReturn("gid");
    given(this.artifact.getArtifactId()).willReturn("aid");
    given(this.artifact.getScope()).willReturn("compile");
    Dependency unpack = new Dependency();
    unpack.setGroupId("gid");
    unpack.setArtifactId("aid");
    this.libs = new ArtifactsLibraries(this.artifacts, Collections.emptyList(), Collections.singleton(unpack),
            mock(Log.class));
    this.libs.doWithLibraries(this.callback);
    then(this.callback).should().library(assertArg((library) -> assertThat(library.isUnpackRequired()).isTrue()));
  }

  @Test
  void renamesDuplicates() throws Exception {
    Artifact artifact1 = mock(Artifact.class);
    Artifact artifact2 = mock(Artifact.class);
    given(artifact1.getScope()).willReturn("compile");
    given(artifact1.getGroupId()).willReturn("g1");
    given(artifact1.getArtifactId()).willReturn("artifact");
    given(artifact1.getBaseVersion()).willReturn("1.0");
    given(artifact1.getFile()).willReturn(new File("a"));
    given(artifact1.getArtifactHandler()).willReturn(this.artifactHandler);
    given(artifact2.getScope()).willReturn("compile");
    given(artifact2.getGroupId()).willReturn("g2");
    given(artifact2.getArtifactId()).willReturn("artifact");
    given(artifact2.getBaseVersion()).willReturn("1.0");
    given(artifact2.getFile()).willReturn(new File("a"));
    given(artifact2.getArtifactHandler()).willReturn(this.artifactHandler);
    this.artifacts = new LinkedHashSet<>(Arrays.asList(artifact1, artifact2));
    this.libs = new ArtifactsLibraries(this.artifacts, Collections.emptyList(), null, mock(Log.class));
    this.libs.doWithLibraries(this.callback);
    then(this.callback).should(times(2)).library(this.libraryCaptor.capture());
    assertThat(this.libraryCaptor.getAllValues().get(0).getName()).isEqualTo("g1-artifact-1.0.jar");
    assertThat(this.libraryCaptor.getAllValues().get(1).getName()).isEqualTo("g2-artifact-1.0.jar");
  }

  @Test
  void libraryCoordinatesVersionUsesBaseVersionOfArtifact() throws IOException {
    Artifact snapshotArtifact = mock(Artifact.class);
    given(snapshotArtifact.getScope()).willReturn("compile");
    given(snapshotArtifact.getArtifactId()).willReturn("artifact");
    given(snapshotArtifact.getBaseVersion()).willReturn("1.0-SNAPSHOT");
    given(snapshotArtifact.getFile()).willReturn(new File("a"));
    given(snapshotArtifact.getArtifactHandler()).willReturn(this.artifactHandler);
    this.artifacts = Collections.singleton(snapshotArtifact);
    new ArtifactsLibraries(this.artifacts, Collections.emptyList(), null, mock(Log.class))
            .doWithLibraries((library) -> {
              assertThat(library.isIncluded()).isTrue();
              assertThat(library.isLocal()).isFalse();
              assertThat(library.getCoordinates().getVersion()).isEqualTo("1.0-SNAPSHOT");
            });
  }

  @Test
  void artifactForLocalProjectProducesLocalLibrary() throws IOException {
    Artifact artifact = mock(Artifact.class);
    given(artifact.getScope()).willReturn("compile");
    given(artifact.getArtifactId()).willReturn("artifact");
    given(artifact.getBaseVersion()).willReturn("1.0-SNAPSHOT");
    given(artifact.getFile()).willReturn(new File("a"));
    given(artifact.getArtifactHandler()).willReturn(this.artifactHandler);
    MavenProject mavenProject = mock(MavenProject.class);
    given(mavenProject.getArtifact()).willReturn(artifact);
    this.artifacts = Collections.singleton(artifact);
    new ArtifactsLibraries(this.artifacts, Collections.singleton(mavenProject), null, mock(Log.class))
            .doWithLibraries((library) -> assertThat(library.isLocal()).isTrue());
  }

  @Test
  void attachedArtifactForLocalProjectProducesLocalLibrary() throws IOException {
    MavenProject mavenProject = mock(MavenProject.class);
    Artifact artifact = mock(Artifact.class);
    given(mavenProject.getArtifact()).willReturn(artifact);
    Artifact attachedArtifact = mock(Artifact.class);
    given(attachedArtifact.getScope()).willReturn("compile");
    given(attachedArtifact.getArtifactId()).willReturn("attached-artifact");
    given(attachedArtifact.getBaseVersion()).willReturn("1.0-SNAPSHOT");
    given(attachedArtifact.getFile()).willReturn(new File("a"));
    given(attachedArtifact.getArtifactHandler()).willReturn(this.artifactHandler);
    given(mavenProject.getAttachedArtifacts()).willReturn(Collections.singletonList(attachedArtifact));
    this.artifacts = Collections.singleton(attachedArtifact);
    new ArtifactsLibraries(this.artifacts, Collections.singleton(mavenProject), null, mock(Log.class))
            .doWithLibraries((library) -> assertThat(library.isLocal()).isTrue());
  }

  @Test
  void nonIncludedArtifact() throws IOException {
    Artifact artifact = mock(Artifact.class);
    given(artifact.getScope()).willReturn("compile");
    given(artifact.getArtifactId()).willReturn("artifact");
    given(artifact.getBaseVersion()).willReturn("1.0-SNAPSHOT");
    given(artifact.getFile()).willReturn(new File("a"));
    given(artifact.getArtifactHandler()).willReturn(this.artifactHandler);
    MavenProject mavenProject = mock(MavenProject.class);
    given(mavenProject.getArtifact()).willReturn(artifact);
    this.artifacts = Collections.singleton(artifact);
    new ArtifactsLibraries(this.artifacts, Collections.emptySet(), Collections.singleton(mavenProject), null,
            mock(Log.class))
            .doWithLibraries((library) -> assertThat(library.isIncluded()).isFalse());
  }

}
