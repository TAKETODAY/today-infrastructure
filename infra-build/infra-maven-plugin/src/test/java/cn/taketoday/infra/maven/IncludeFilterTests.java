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
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link IncludeFilter}.
 *
 * @author David Turanski
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class IncludeFilterTests {

  @Test
  void includeSimple() throws ArtifactFilterException {
    IncludeFilter filter = new IncludeFilter(Arrays.asList(createInclude("com.foo", "bar")));
    Artifact artifact = createArtifact("com.foo", "bar");
    Set result = filter.filter(Collections.singleton(artifact));
    assertThat(result).hasSize(1);
    assertThat(result.iterator().next()).isSameAs(artifact);
  }

  @Test
  void includeGroupIdNoMatch() throws ArtifactFilterException {
    IncludeFilter filter = new IncludeFilter(Arrays.asList(createInclude("com.foo", "bar")));
    Artifact artifact = createArtifact("com.baz", "bar");
    Set result = filter.filter(Collections.singleton(artifact));
    assertThat(result).isEmpty();
  }

  @Test
  void includeArtifactIdNoMatch() throws ArtifactFilterException {
    IncludeFilter filter = new IncludeFilter(Arrays.asList(createInclude("com.foo", "bar")));
    Artifact artifact = createArtifact("com.foo", "biz");
    Set result = filter.filter(Collections.singleton(artifact));
    assertThat(result).isEmpty();
  }

  @Test
  void includeClassifier() throws ArtifactFilterException {
    IncludeFilter filter = new IncludeFilter(Arrays.asList(createInclude("com.foo", "bar", "jdk5")));
    Artifact artifact = createArtifact("com.foo", "bar", "jdk5");
    Set result = filter.filter(Collections.singleton(artifact));
    assertThat(result).hasSize(1);
    assertThat(result.iterator().next()).isSameAs(artifact);
  }

  @Test
  void includeClassifierNoTargetClassifier() throws ArtifactFilterException {
    IncludeFilter filter = new IncludeFilter(Arrays.asList(createInclude("com.foo", "bar", "jdk5")));
    Artifact artifact = createArtifact("com.foo", "bar");
    Set result = filter.filter(Collections.singleton(artifact));
    assertThat(result).isEmpty();
  }

  @Test
  void includeClassifierNoMatch() throws ArtifactFilterException {
    IncludeFilter filter = new IncludeFilter(Arrays.asList(createInclude("com.foo", "bar", "jdk5")));
    Artifact artifact = createArtifact("com.foo", "bar", "jdk6");
    Set result = filter.filter(Collections.singleton(artifact));
    assertThat(result).isEmpty();
  }

  @Test
  void includeMulti() throws ArtifactFilterException {
    IncludeFilter filter = new IncludeFilter(Arrays.asList(createInclude("com.foo", "bar"),
            createInclude("com.foo", "bar2"), createInclude("org.acme", "app")));
    Set<Artifact> artifacts = new HashSet<>();
    artifacts.add(createArtifact("com.foo", "bar"));
    artifacts.add(createArtifact("com.foo", "bar"));
    Artifact anotherAcme = createArtifact("org.acme", "another-app");
    artifacts.add(anotherAcme);
    Set result = filter.filter(artifacts);
    assertThat(result).hasSize(2);
  }

  private Include createInclude(String groupId, String artifactId) {
    return createInclude(groupId, artifactId, null);
  }

  private Include createInclude(String groupId, String artifactId, String classifier) {
    Include include = new Include();
    include.setGroupId(groupId);
    include.setArtifactId(artifactId);
    if (classifier != null) {
      include.setClassifier(classifier);
    }
    return include;
  }

  private Artifact createArtifact(String groupId, String artifactId, String classifier) {
    Artifact a = mock(Artifact.class);
    given(a.getGroupId()).willReturn(groupId);
    given(a.getArtifactId()).willReturn(artifactId);
    given(a.getClassifier()).willReturn(classifier);
    return a;
  }

  private Artifact createArtifact(String groupId, String artifactId) {
    return createArtifact(groupId, artifactId, null);
  }

}
