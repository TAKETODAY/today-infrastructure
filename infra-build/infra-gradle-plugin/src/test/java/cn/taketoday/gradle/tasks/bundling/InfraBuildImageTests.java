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

import org.gradle.api.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import cn.taketoday.buildpack.platform.build.BuildRequest;
import cn.taketoday.buildpack.platform.build.BuildpackReference;
import cn.taketoday.buildpack.platform.build.PullPolicy;
import cn.taketoday.buildpack.platform.docker.type.Binding;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.gradle.junit.GradleProjectBuilder;
import cn.taketoday.lang.Version;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfraBuildImage}.
 *
 * @author Andy Wilkinson
 * @author Scott Frederick
 * @author Andrey Shlykov
 * @author Jeroen Meijer
 * @author Rafael Ceccone
 */
class InfraBuildImageTests {

  Project project;

  private InfraBuildImage buildImage;

  @BeforeEach
  void setUp(@TempDir File temp) {
    File projectDir = new File(temp, "project");
    projectDir.mkdirs();
    this.project = GradleProjectBuilder.builder().withProjectDir(projectDir).withName("build-image-test").build();
    this.project.setDescription("Test project for InfraBuildImage");
    this.buildImage = this.project.getTasks().create("buildImage", InfraBuildImage.class);
  }

  @Test
  void whenProjectVersionIsUnspecifiedThenItIsIgnoredWhenDerivingImageName() {
    assertThat(this.buildImage.getImageName().get()).isEqualTo("docker.io/library/build-image-test");
    BuildRequest request = this.buildImage.createRequest();
    assertThat(request.getName().getDomain()).isEqualTo("docker.io");
    assertThat(request.getName().getName()).isEqualTo("library/build-image-test");
    assertThat(request.getName().getTag()).isEqualTo("latest");
    assertThat(request.getName().getDigest()).isNull();
  }

  @Test
  void whenProjectVersionIsSpecifiedThenItIsUsedInTagOfImageName() {
    this.project.setVersion("1.2.3");
    assertThat(this.buildImage.getImageName().get()).isEqualTo("docker.io/library/build-image-test:1.2.3");
    BuildRequest request = this.buildImage.createRequest();
    assertThat(request.getName().getDomain()).isEqualTo("docker.io");
    assertThat(request.getName().getName()).isEqualTo("library/build-image-test");
    assertThat(request.getName().getTag()).isEqualTo("1.2.3");
    assertThat(request.getName().getDigest()).isNull();
  }

  @Test
  void whenImageNameIsSpecifiedThenItIsUsedInRequest() {
    this.project.setVersion("1.2.3");
    this.buildImage.getImageName().set("example.com/test/build-image:1.0");
    assertThat(this.buildImage.getImageName().get()).isEqualTo("example.com/test/build-image:1.0");
    BuildRequest request = this.buildImage.createRequest();
    assertThat(request.getName().getDomain()).isEqualTo("example.com");
    assertThat(request.getName().getName()).isEqualTo("test/build-image");
    assertThat(request.getName().getTag()).isEqualTo("1.0");
    assertThat(request.getName().getDigest()).isNull();
  }

  @Test
  void infraVersionDefaultValueIsUsed() {
    BuildRequest request = this.buildImage.createRequest();
    assertThat(request.getCreator().getName()).isEqualTo("Infra Application");
    assertThat(request.getCreator().getVersion()).isEqualTo(Version.get().implementationVersion());
  }

  @Test
  void whenIndividualEntriesAreAddedToTheEnvironmentThenTheyAreIncludedInTheRequest() {
    this.buildImage.getEnvironment().put("ALPHA", "a");
    this.buildImage.getEnvironment().put("BRAVO", "b");
    assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a")
            .containsEntry("BRAVO", "b")
            .hasSize(2);
  }

  @Test
  void whenEntriesAreAddedToTheEnvironmentThenTheyAreIncludedInTheRequest() {
    Map<String, String> environment = new HashMap<>();
    environment.put("ALPHA", "a");
    environment.put("BRAVO", "b");
    this.buildImage.getEnvironment().putAll(environment);
    assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a")
            .containsEntry("BRAVO", "b")
            .hasSize(2);
  }

  @Test
  void whenTheEnvironmentIsSetItIsIncludedInTheRequest() {
    Map<String, String> environment = new HashMap<>();
    environment.put("ALPHA", "a");
    environment.put("BRAVO", "b");
    this.buildImage.getEnvironment().set(environment);
    assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a")
            .containsEntry("BRAVO", "b")
            .hasSize(2);
  }

  @Test
  void whenTheEnvironmentIsSetItReplacesAnyExistingEntriesAndIsIncludedInTheRequest() {
    Map<String, String> environment = new HashMap<>();
    environment.put("ALPHA", "a");
    environment.put("BRAVO", "b");
    this.buildImage.getEnvironment().put("C", "Charlie");
    this.buildImage.getEnvironment().set(environment);
    assertThat(this.buildImage.createRequest().getEnv()).containsEntry("ALPHA", "a")
            .containsEntry("BRAVO", "b")
            .hasSize(2);
  }

  @Test
  void whenUsingDefaultConfigurationThenRequestHasVerboseLoggingDisabled() {
    assertThat(this.buildImage.createRequest().isVerboseLogging()).isFalse();
  }

  @Test
  void whenVerboseLoggingIsEnabledThenRequestHasVerboseLoggingEnabled() {
    this.buildImage.getVerboseLogging().set(true);
    assertThat(this.buildImage.createRequest().isVerboseLogging()).isTrue();
  }

  @Test
  void whenUsingDefaultConfigurationThenRequestHasCleanCacheDisabled() {
    assertThat(this.buildImage.createRequest().isCleanCache()).isFalse();
  }

  @Test
  void whenCleanCacheIsEnabledThenRequestHasCleanCacheEnabled() {
    this.buildImage.getCleanCache().set(true);
    assertThat(this.buildImage.createRequest().isCleanCache()).isTrue();
  }

  @Test
  void whenUsingDefaultConfigurationThenRequestHasPublishDisabled() {
    assertThat(this.buildImage.createRequest().isPublish()).isFalse();
  }

  @Test
  void whenNoBuilderIsConfiguredThenRequestHasDefaultBuilder() {
    assertThat(this.buildImage.createRequest().getBuilder().getName())
            .isEqualTo("paketobuildpacks/builder-jammy-base");
  }

  @Test
  void whenBuilderIsConfiguredThenRequestUsesSpecifiedBuilder() {
    this.buildImage.getBuilder().set("example.com/test/builder:1.2");
    assertThat(this.buildImage.createRequest().getBuilder().getName()).isEqualTo("test/builder");
  }

  @Test
  void whenNoRunImageIsConfiguredThenRequestUsesDefaultRunImage() {
    assertThat(this.buildImage.createRequest().getRunImage()).isNull();
  }

  @Test
  void whenRunImageIsConfiguredThenRequestUsesSpecifiedRunImage() {
    this.buildImage.getRunImage().set("example.com/test/run:1.0");
    assertThat(this.buildImage.createRequest().getRunImage().getName()).isEqualTo("test/run");
  }

  @Test
  void whenUsingDefaultConfigurationThenRequestHasAlwaysPullPolicy() {
    assertThat(this.buildImage.createRequest().getPullPolicy()).isEqualTo(PullPolicy.ALWAYS);
  }

  @Test
  void whenPullPolicyIsConfiguredThenRequestHasPullPolicy() {
    this.buildImage.getPullPolicy().set(PullPolicy.NEVER);
    assertThat(this.buildImage.createRequest().getPullPolicy()).isEqualTo(PullPolicy.NEVER);
  }

  @Test
  void whenNoBuildpacksAreConfiguredThenRequestUsesDefaultBuildpacks() {
    assertThat(this.buildImage.createRequest().getBuildpacks()).isEmpty();
  }

  @Test
  void whenBuildpacksAreConfiguredThenRequestHasBuildpacks() {
    this.buildImage.getBuildpacks().set(Arrays.asList("example/buildpack1", "example/buildpack2"));
    assertThat(this.buildImage.createRequest().getBuildpacks())
            .containsExactly(BuildpackReference.of("example/buildpack1"), BuildpackReference.of("example/buildpack2"));
  }

  @Test
  void whenEntriesAreAddedToBuildpacksThenRequestHasBuildpacks() {
    this.buildImage.getBuildpacks().addAll(Arrays.asList("example/buildpack1", "example/buildpack2"));
    assertThat(this.buildImage.createRequest().getBuildpacks())
            .containsExactly(BuildpackReference.of("example/buildpack1"), BuildpackReference.of("example/buildpack2"));
  }

  @Test
  void whenIndividualEntriesAreAddedToBuildpacksThenRequestHasBuildpacks() {
    this.buildImage.getBuildpacks().add("example/buildpack1");
    this.buildImage.getBuildpacks().add("example/buildpack2");
    assertThat(this.buildImage.createRequest().getBuildpacks())
            .containsExactly(BuildpackReference.of("example/buildpack1"), BuildpackReference.of("example/buildpack2"));
  }

  @Test
  void whenNoBindingsAreConfiguredThenRequestHasNoBindings() {
    assertThat(this.buildImage.createRequest().getBindings()).isEmpty();
  }

  @Test
  void whenBindingsAreConfiguredThenRequestHasBindings() {
    this.buildImage.getBindings().set(Arrays.asList("host-src:container-dest:ro", "volume-name:container-dest:rw"));
    assertThat(this.buildImage.createRequest().getBindings())
            .containsExactly(Binding.of("host-src:container-dest:ro"), Binding.of("volume-name:container-dest:rw"));
  }

  @Test
  void whenEntriesAreAddedToBindingsThenRequestHasBindings() {
    this.buildImage.getBindings()
            .addAll(Arrays.asList("host-src:container-dest:ro", "volume-name:container-dest:rw"));
    assertThat(this.buildImage.createRequest().getBindings())
            .containsExactly(Binding.of("host-src:container-dest:ro"), Binding.of("volume-name:container-dest:rw"));
  }

  @Test
  void whenIndividualEntriesAreAddedToBindingsThenRequestHasBindings() {
    this.buildImage.getBindings().add("host-src:container-dest:ro");
    this.buildImage.getBindings().add("volume-name:container-dest:rw");
    assertThat(this.buildImage.createRequest().getBindings())
            .containsExactly(Binding.of("host-src:container-dest:ro"), Binding.of("volume-name:container-dest:rw"));
  }

  @Test
  void whenNetworkIsConfiguredThenRequestHasNetwork() {
    this.buildImage.getNetwork().set("test");
    assertThat(this.buildImage.createRequest().getNetwork()).isEqualTo("test");
  }

  @Test
  void whenNoTagsAreConfiguredThenRequestHasNoTags() {
    assertThat(this.buildImage.createRequest().getTags()).isEmpty();
  }

  @Test
  void whenTagsAreConfiguredThenRequestHasTags() {
    this.buildImage.getTags()
            .set(Arrays.asList("my-app:latest", "example.com/my-app:0.0.1-SNAPSHOT", "example.com/my-app:latest"));
    assertThat(this.buildImage.createRequest().getTags()).containsExactly(ImageReference.of("my-app:latest"),
            ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
  }

  @Test
  void whenEntriesAreAddedToTagsThenRequestHasTags() {
    this.buildImage.getTags()
            .addAll(Arrays.asList("my-app:latest", "example.com/my-app:0.0.1-SNAPSHOT", "example.com/my-app:latest"));
    assertThat(this.buildImage.createRequest().getTags()).containsExactly(ImageReference.of("my-app:latest"),
            ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
  }

  @Test
  void whenIndividualEntriesAreAddedToTagsThenRequestHasTags() {
    this.buildImage.getTags().add("my-app:latest");
    this.buildImage.getTags().add("example.com/my-app:0.0.1-SNAPSHOT");
    this.buildImage.getTags().add("example.com/my-app:latest");
    assertThat(this.buildImage.createRequest().getTags()).containsExactly(ImageReference.of("my-app:latest"),
            ImageReference.of("example.com/my-app:0.0.1-SNAPSHOT"), ImageReference.of("example.com/my-app:latest"));
  }

}
