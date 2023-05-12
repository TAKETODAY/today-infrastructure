/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.shared.artifact.filter.collection.ArtifactsFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import cn.taketoday.util.ExceptionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link AbstractDependencyFilterMojo}.
 *
 * @author Stephane Nicoll
 */
class DependencyFilterMojoTests {

  @TempDir
  static Path temp;

  @Test
  void filterDependencies() throws MojoExecutionException {
    TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "com.foo");

    Artifact artifact = createArtifact("com.bar", "one");
    Set<Artifact> artifacts = mojo.filterDependencies(createArtifact("com.foo", "one"),
            createArtifact("com.foo", "two"), artifact);
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.iterator().next()).isSameAs(artifact);
  }

  @Test
  void filterGroupIdExactMatch() throws MojoExecutionException {
    TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "com.foo");

    Artifact artifact = createArtifact("com.foo.bar", "one");
    Set<Artifact> artifacts = mojo.filterDependencies(createArtifact("com.foo", "one"),
            createArtifact("com.foo", "two"), artifact);
    assertThat(artifacts).hasSize(1);
    assertThat(artifacts.iterator().next()).isSameAs(artifact);
  }

  @Test
  void filterScopeKeepOrder() throws MojoExecutionException {
    TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "",
            new ScopeFilter(null, Artifact.SCOPE_SYSTEM));
    Artifact one = createArtifact("com.foo", "one");
    Artifact two = createArtifact("com.foo", "two", Artifact.SCOPE_SYSTEM);
    Artifact three = createArtifact("com.foo", "three", Artifact.SCOPE_RUNTIME);
    Set<Artifact> artifacts = mojo.filterDependencies(one, two, three);
    assertThat(artifacts).containsExactly(one, three);
  }

  @Test
  void filterGroupIdKeepOrder() throws MojoExecutionException {
    TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "com.foo");
    Artifact one = createArtifact("com.foo", "one");
    Artifact two = createArtifact("com.bar", "two");
    Artifact three = createArtifact("com.bar", "three");
    Artifact four = createArtifact("com.foo", "four");
    Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
    assertThat(artifacts).containsExactly(two, three);
  }

  @Test
  void filterExcludeKeepOrder() throws MojoExecutionException {
    Exclude exclude = new Exclude();
    exclude.setGroupId("com.bar");
    exclude.setArtifactId("two");
    TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.singletonList(exclude), "");
    Artifact one = createArtifact("com.foo", "one");
    Artifact two = createArtifact("com.bar", "two");
    Artifact three = createArtifact("com.bar", "three");
    Artifact four = createArtifact("com.foo", "four");
    Set<Artifact> artifacts = mojo.filterDependencies(one, two, three, four);
    assertThat(artifacts).containsExactly(one, three, four);
  }

  @Test
  @Disabled
  void excludeByJarType() throws MojoExecutionException {
    TestableDependencyFilterMojo mojo = new TestableDependencyFilterMojo(Collections.emptyList(), "");
    Artifact one = createArtifact("com.foo", "one", null, "dependencies-starter");
    Artifact two = createArtifact("com.bar", "two");
    Set<Artifact> artifacts = mojo.filterDependencies(one, two);
    assertThat(artifacts).containsExactly(two);
  }

  private static Artifact createArtifact(String groupId, String artifactId) {
    return createArtifact(groupId, artifactId, null);
  }

  private static Artifact createArtifact(String groupId, String artifactId, String scope) {
    return createArtifact(groupId, artifactId, scope, null);
  }

  private static Artifact createArtifact(String groupId, String artifactId, String scope, String jarType) {
    Artifact a = mock(Artifact.class);
    given(a.getGroupId()).willReturn(groupId);
    given(a.getArtifactId()).willReturn(artifactId);
    if (scope != null) {
      given(a.getScope()).willReturn(scope);
    }
    given(a.getFile()).willReturn(createArtifactFile(jarType));
    return a;
  }

  private static File createArtifactFile(String jarType) {
    Path jarPath = temp.resolve(UUID.randomUUID().toString() + ".jar");
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
    try {
      new JarOutputStream(new FileOutputStream(jarPath.toFile()), manifest).close();
    }
    catch (IOException ex) {
      throw ExceptionUtils.sneakyThrow(ex);
    }
    return jarPath.toFile();
  }

  private static final class TestableDependencyFilterMojo extends AbstractDependencyFilterMojo {

    private final ArtifactsFilter[] additionalFilters;

    private TestableDependencyFilterMojo(List<Exclude> excludes, String excludeGroupIds,
            ArtifactsFilter... additionalFilters) {
      setExcludes(excludes);
      setExcludeGroupIds(excludeGroupIds);
      this.additionalFilters = additionalFilters;
    }

    Set<Artifact> filterDependencies(Artifact... artifacts) throws MojoExecutionException {
      Set<Artifact> input = new LinkedHashSet<>(Arrays.asList(artifacts));
      return filterDependencies(input, this.additionalFilters);
    }

    @Override
    public void execute() {

    }

  }

}
