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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;

import cn.taketoday.buildpack.platform.docker.DockerApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.ContainerApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.ImageApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.VolumeApi;
import cn.taketoday.buildpack.platform.docker.TotalProgressPullListener;
import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration;
import cn.taketoday.buildpack.platform.docker.transport.DockerEngineException;
import cn.taketoday.buildpack.platform.docker.type.ContainerReference;
import cn.taketoday.buildpack.platform.docker.type.ContainerStatus;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageArchive;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.io.TarArchive;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link Builder}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Rafael Ceccone
 */
class BuilderTests {

  @Test
  void createWhenLogIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() -> new Builder((BuildLog) null))
            .withMessage("Log must not be null");
  }

  @Test
  void createWithDockerConfiguration() {
    Builder builder = new Builder(BuildLog.toSystemOut());
    assertThat(builder).isNotNull();
  }

  @Test
  void buildWhenRequestIsNullThrowsException() {
    Builder builder = new Builder();
    assertThatIllegalArgumentException().isThrownBy(() -> builder.build(null))
            .withMessage("Request must not be null");
  }

  @Test
  void buildInvokesBuilder() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(ArgumentMatchers.eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest();
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should()
            .pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull());
    then(docker.image()).should()
            .pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull());
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
    then(docker.image()).shouldHaveNoMoreInteractions();
  }

  @Test
  void buildInvokesBuilderAndPublishesImage() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    DockerConfiguration dockerConfiguration = new DockerConfiguration()
            .withBuilderRegistryTokenAuthentication("builder token")
            .withPublishRegistryTokenAuthentication("publish token");
    given(docker.image()
            .pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image()
            .pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
    BuildRequest request = getTestRequest().withPublish(true);
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should()
            .pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
    then(docker.image()).should()
            .pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
    then(docker.image()).should()
            .push(eq(request.getName()), any(),
                    eq(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()));
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
    then(docker.image()).shouldHaveNoMoreInteractions();
  }

  @Test
  void buildInvokesBuilderWithDefaultImageTags() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image-with-no-run-image-tag.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of("gcr.io/paketo-buildpacks/builder:latest")), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:latest")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest().withBuilder(ImageReference.of("gcr.io/paketo-buildpacks/builder"));
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
  }

  @Test
  void buildInvokesBuilderWithRunImageInDigestForm() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image-with-run-image-digest.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image()
            .pull(eq(ImageReference
                            .of("docker.io/cloudfoundry/run@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d")),
                    any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest();
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
  }

  @Test
  void buildInvokesBuilderWithRunImageFromRequest() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("example.com/custom/run:latest")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest().withRunImage(ImageReference.of("example.com/custom/run:latest"));
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
  }

  @Test
  void buildInvokesBuilderWithNeverPullPolicy() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    given(docker.image().inspect(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME))))
            .willReturn(builderImage);
    given(docker.image().inspect(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb"))))
            .willReturn(runImage);
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest().withPullPolicy(PullPolicy.NEVER);
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
    then(docker.image()).should(never()).pull(any(), any());
    then(docker.image()).should(times(2)).inspect(any());
  }

  @Test
  void buildInvokesBuilderWithAlwaysPullPolicy() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    given(docker.image().inspect(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME))))
            .willReturn(builderImage);
    given(docker.image().inspect(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb"))))
            .willReturn(runImage);
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest().withPullPolicy(PullPolicy.ALWAYS);
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
    then(docker.image()).should(times(2)).pull(any(), any(), isNull());
    then(docker.image()).should(never()).inspect(any());
  }

  @Test
  void buildInvokesBuilderWithIfNotPresentPullPolicy() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    given(docker.image().inspect(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME))))
            .willThrow(
                    new DockerEngineException("docker://localhost/", new URI("example"), 404, "NOT FOUND", null, null))
            .willReturn(builderImage);
    given(docker.image().inspect(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb"))))
            .willThrow(
                    new DockerEngineException("docker://localhost/", new URI("example"), 404, "NOT FOUND", null, null))
            .willReturn(runImage);
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest().withPullPolicy(PullPolicy.IF_NOT_PRESENT);
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
    then(docker.image()).should(times(2)).inspect(any());
    then(docker.image()).should(times(2)).pull(any(), any(), isNull());
  }

  @Test
  void buildInvokesBuilderWithTags() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest().withTags(ImageReference.of("my-application:1.2.3"));
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    assertThat(out.toString()).contains("Successfully created image tag 'docker.io/library/my-application:1.2.3'");
    then(docker.image()).should().tag(eq(request.getName()), eq(ImageReference.of("my-application:1.2.3")));
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
  }

  @Test
  void buildInvokesBuilderWithTagsAndPublishesImageAndTags() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    DockerConfiguration dockerConfiguration = new DockerConfiguration()
            .withBuilderRegistryTokenAuthentication("builder token")
            .withPublishRegistryTokenAuthentication("publish token");
    given(docker.image()
            .pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image()
            .pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
    BuildRequest request = getTestRequest().withPublish(true).withTags(ImageReference.of("my-application:1.2.3"));
    builder.build(request);
    assertThat(out.toString()).contains("Running creator");
    assertThat(out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
    assertThat(out.toString()).contains("Successfully created image tag 'docker.io/library/my-application:1.2.3'");

    then(docker.image()).should()
            .pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
    then(docker.image()).should()
            .pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader()));
    then(docker.image()).should()
            .push(eq(request.getName()), any(),
                    eq(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()));
    then(docker.image()).should().tag(eq(request.getName()), eq(ImageReference.of("my-application:1.2.3")));
    then(docker.image()).should()
            .push(eq(ImageReference.of("my-application:1.2.3")), any(),
                    eq(dockerConfiguration.getPublishRegistryAuthentication().getAuthHeader()));
    ArgumentCaptor<ImageArchive> archive = ArgumentCaptor.forClass(ImageArchive.class);
    then(docker.image()).should().load(archive.capture(), any());
    then(docker.image()).should().remove(archive.getValue().getTag(), true);
    then(docker.image()).shouldHaveNoMoreInteractions();
  }

  @Test
  void buildWhenStackIdDoesNotMatchThrowsException() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image-with-bad-stack.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest();
    assertThatIllegalStateException().isThrownBy(() -> builder.build(request))
            .withMessage(
                    "Run image stack 'org.cloudfoundry.stacks.cfwindowsfs3' does not match builder stack 'io.buildpacks.stacks.bionic'");
  }

  @Test
  void buildWhenBuilderReturnsErrorThrowsException() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApiLifecycleError();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildRequest request = getTestRequest();
    assertThatExceptionOfType(BuilderException.class).isThrownBy(() -> builder.build(request))
            .withMessage("Builder lifecycle 'creator' failed with status code 9");
  }

  @Test
  void buildWhenDetectedRunImageInDifferentAuthenticatedRegistryThrowsException() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image-with-run-image-different-registry.json");
    DockerConfiguration dockerConfiguration = new DockerConfiguration()
            .withBuilderRegistryTokenAuthentication("builder token");
    given(docker.image()
            .pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
            .willAnswer(withPulledImage(builderImage));
    Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
    BuildRequest request = getTestRequest();
    assertThatIllegalStateException().isThrownBy(() -> builder.build(request))
            .withMessage(
                    "Run image 'example.com/custom/run:latest' must be pulled from the 'docker.io' authenticated registry");
  }

  @Test
  void buildWhenRequestedRunImageInDifferentAuthenticatedRegistryThrowsException() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApi();
    Image builderImage = loadImage("image.json");
    DockerConfiguration dockerConfiguration = new DockerConfiguration()
            .withBuilderRegistryTokenAuthentication("builder token");
    given(docker.image()
            .pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(),
                    eq(dockerConfiguration.getBuilderRegistryAuthentication().getAuthHeader())))
            .willAnswer(withPulledImage(builderImage));
    Builder builder = new Builder(BuildLog.to(out), docker, dockerConfiguration);
    BuildRequest request = getTestRequest().withRunImage(ImageReference.of("example.com/custom/run:latest"));
    assertThatIllegalStateException().isThrownBy(() -> builder.build(request))
            .withMessage(
                    "Run image 'example.com/custom/run:latest' must be pulled from the 'docker.io' authenticated registry");
  }

  @Test
  void buildWhenRequestedBuildpackNotInBuilderThrowsException() throws Exception {
    TestPrintStream out = new TestPrintStream();
    DockerApi docker = mockDockerApiLifecycleError();
    Image builderImage = loadImage("image.json");
    Image runImage = loadImage("run-image.json");
    given(docker.image().pull(eq(ImageReference.of(BuildRequest.DEFAULT_BUILDER_IMAGE_NAME)), any(), isNull()))
            .willAnswer(withPulledImage(builderImage));
    given(docker.image().pull(eq(ImageReference.of("docker.io/cloudfoundry/run:base-cnb")), any(), isNull()))
            .willAnswer(withPulledImage(runImage));
    Builder builder = new Builder(BuildLog.to(out), docker, null);
    BuildpackReference reference = BuildpackReference.of("urn:cnb:builder:example/buildpack@1.2.3");
    BuildRequest request = getTestRequest().withBuildpacks(reference);
    assertThatIllegalArgumentException().isThrownBy(() -> builder.build(request))
            .withMessageContaining("'urn:cnb:builder:example/buildpack@1.2.3'")
            .withMessageContaining("not found in builder");
  }

  private DockerApi mockDockerApi() throws IOException {
    ContainerApi containerApi = mock(ContainerApi.class);
    ContainerReference reference = ContainerReference.of("container-ref");
    given(containerApi.create(any(), any())).willReturn(reference);
    given(containerApi.wait(eq(reference))).willReturn(ContainerStatus.of(0, null));
    ImageApi imageApi = mock(ImageApi.class);
    VolumeApi volumeApi = mock(VolumeApi.class);
    DockerApi docker = mock(DockerApi.class);
    given(docker.image()).willReturn(imageApi);
    given(docker.container()).willReturn(containerApi);
    given(docker.volume()).willReturn(volumeApi);
    return docker;
  }

  private DockerApi mockDockerApiLifecycleError() throws IOException {
    ContainerApi containerApi = mock(ContainerApi.class);
    ContainerReference reference = ContainerReference.of("container-ref");
    given(containerApi.create(any(), any())).willReturn(reference);
    given(containerApi.wait(eq(reference))).willReturn(ContainerStatus.of(9, null));
    ImageApi imageApi = mock(ImageApi.class);
    VolumeApi volumeApi = mock(VolumeApi.class);
    DockerApi docker = mock(DockerApi.class);
    given(docker.image()).willReturn(imageApi);
    given(docker.container()).willReturn(containerApi);
    given(docker.volume()).willReturn(volumeApi);
    return docker;
  }

  private BuildRequest getTestRequest() {
    TarArchive content = mock(TarArchive.class);
    ImageReference name = ImageReference.of("my-application");
    return BuildRequest.of(name, (owner) -> content);
  }

  private Image loadImage(String name) throws IOException {
    return Image.of(getClass().getResourceAsStream(name));
  }

  private Answer<Image> withPulledImage(Image image) {
    return (invocation) -> {
      TotalProgressPullListener listener = invocation.getArgument(1, TotalProgressPullListener.class);
      listener.onStart();
      listener.onFinish();
      return image;
    };

  }

  static class TestPrintStream extends PrintStream {

    TestPrintStream() {
      super(new ByteArrayOutputStream());
    }

    @Override
    public String toString() {
      return this.out.toString();
    }

  }

}
