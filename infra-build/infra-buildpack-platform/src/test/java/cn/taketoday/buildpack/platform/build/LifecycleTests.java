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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.jna.Platform;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.taketoday.buildpack.platform.docker.DockerApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.ContainerApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.ImageApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.VolumeApi;
import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import cn.taketoday.buildpack.platform.docker.configuration.ResolvedDockerHost;
import cn.taketoday.buildpack.platform.docker.type.Binding;
import cn.taketoday.buildpack.platform.docker.type.ContainerConfig;
import cn.taketoday.buildpack.platform.docker.type.ContainerContent;
import cn.taketoday.buildpack.platform.docker.type.ContainerReference;
import cn.taketoday.buildpack.platform.docker.type.ContainerStatus;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.io.TarArchive;
import cn.taketoday.buildpack.platform.json.SharedObjectMapper;
import cn.taketoday.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Lifecycle}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
 */
class LifecycleTests {

  private TestPrintStream out;

  private DockerApi docker;

  private final Map<String, ContainerConfig> configs = new LinkedHashMap<>();

  private final Map<String, ContainerContent> content = new LinkedHashMap<>();

  @BeforeEach
  void setup() {
    this.out = new TestPrintStream();
    this.docker = mockDockerApi();
  }

  @Test
  void executeExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    createLifecycle().execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeWithBindingsExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest().withBindings(Binding.of("/host/src/path:/container/dest/path:ro"),
            Binding.of("volume-name:/container/volume/path:rw"));
    createLifecycle(request).execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-bindings.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeExecutesPhasesWithPlatformApi03() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    createLifecycle("builder-metadata-platform-api-0.3.json").execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-platform-api-0.3.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeOnlyUploadsContentOnce() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    createLifecycle().execute();
    assertThat(this.content).hasSize(1);
  }

  @Test
  void executeWhenAlreadyRunThrowsException() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    Lifecycle lifecycle = createLifecycle();
    lifecycle.execute();
    assertThatIllegalStateException().isThrownBy(lifecycle::execute)
            .withMessage("Lifecycle has already been executed");
  }

  @Test
  void executeWhenBuilderReturnsErrorThrowsException() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(9, null));
    assertThatExceptionOfType(BuilderException.class).isThrownBy(() -> createLifecycle().execute())
            .withMessage("Builder lifecycle 'creator' failed with status code 9");
  }

  @Test
  void executeWhenCleanCacheClearsCache() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest().withCleanCache(true);
    createLifecycle(request).execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-clean-cache.json"));
    VolumeName name = VolumeName.of("pack-cache-b35197ac41ea.build");
    then(this.docker.volume()).should().delete(name, true);
  }

  @Test
  void executeWhenPlatformApiNotSupportedThrowsException() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    assertThatIllegalStateException()
            .isThrownBy(() -> createLifecycle("builder-metadata-unsupported-api.json").execute())
            .withMessageContaining("Detected platform API versions '0.2' are not included in supported versions");
  }

  @Test
  void executeWhenMultiplePlatformApisNotSupportedThrowsException() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    assertThatIllegalStateException()
            .isThrownBy(() -> createLifecycle("builder-metadata-unsupported-apis.json").execute())
            .withMessageContaining("Detected platform API versions '0.1,0.2' are not included in supported versions");
  }

  @Test
  void executeWhenMultiplePlatformApisSupportedExecutesPhase() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    createLifecycle("builder-metadata-supported-apis.json").execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator.json"));
  }

  @Test
  void closeClearsVolumes() throws Exception {
    createLifecycle().close();
    then(this.docker.volume()).should().delete(VolumeName.of("pack-layers-aaaaaaaaaa"), true);
    then(this.docker.volume()).should().delete(VolumeName.of("pack-app-aaaaaaaaaa"), true);
  }

  @Test
  void executeWithNetworkExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest().withNetwork("test");
    createLifecycle(request).execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-network.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeWithCacheVolumeNamesExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest().withBuildCache(Cache.volume("build-volume"))
            .withLaunchCache(Cache.volume("launch-volume"));
    createLifecycle(request).execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-cache-volumes.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeWithCreatedDateExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest().withCreatedDate("2020-07-01T12:34:56Z");
    createLifecycle(request).execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-created-date.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeWithApplicationDirectoryExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest().withApplicationDirectory("/application");
    createLifecycle(request).execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-app-dir.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeWithDockerHostAndRemoteAddressExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest();
    createLifecycle(request, ResolvedDockerHost.from(DockerHostConfiguration.forAddress("tcp://192.168.1.2:2376")))
            .execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-inherit-remote.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  @Test
  void executeWithDockerHostAndLocalAddressExecutesPhases() throws Exception {
    given(this.docker.container().create(any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().create(any(), any())).willAnswer(answerWithGeneratedContainerId());
    given(this.docker.container().wait(any())).willReturn(ContainerStatus.of(0, null));
    BuildRequest request = getTestRequest();
    createLifecycle(request, ResolvedDockerHost.from(DockerHostConfiguration.forAddress("/var/alt.sock")))
            .execute();
    assertPhaseWasRun("creator", withExpectedConfig("lifecycle-creator-inherit-local.json"));
    assertThat(this.out.toString()).contains("Successfully built image 'docker.io/library/my-application:latest'");
  }

  private DockerApi mockDockerApi() {
    DockerApi docker = mock(DockerApi.class);
    ImageApi imageApi = mock(ImageApi.class);
    ContainerApi containerApi = mock(ContainerApi.class);
    VolumeApi volumeApi = mock(VolumeApi.class);
    given(docker.image()).willReturn(imageApi);
    given(docker.container()).willReturn(containerApi);
    given(docker.volume()).willReturn(volumeApi);
    return docker;
  }

  private BuildRequest getTestRequest() {
    TarArchive content = mock(TarArchive.class);
    ImageReference name = ImageReference.of("my-application");
    return BuildRequest.of(name, (owner) -> content).withRunImage(ImageReference.of("cloudfoundry/run"));
  }

  private Lifecycle createLifecycle() throws IOException {
    return createLifecycle(getTestRequest());
  }

  private Lifecycle createLifecycle(BuildRequest request) throws IOException {
    EphemeralBuilder builder = mockEphemeralBuilder();
    return createLifecycle(request, builder);
  }

  private Lifecycle createLifecycle(String builderMetadata) throws IOException {
    EphemeralBuilder builder = mockEphemeralBuilder(builderMetadata);
    return createLifecycle(getTestRequest(), builder);
  }

  private Lifecycle createLifecycle(BuildRequest request, ResolvedDockerHost dockerHost) throws IOException {
    EphemeralBuilder builder = mockEphemeralBuilder();
    return new TestLifecycle(BuildLog.to(this.out), this.docker, dockerHost, request, builder);
  }

  private Lifecycle createLifecycle(BuildRequest request, EphemeralBuilder ephemeralBuilder) {
    return new TestLifecycle(BuildLog.to(this.out), this.docker, null, request, ephemeralBuilder);
  }

  private EphemeralBuilder mockEphemeralBuilder() throws IOException {
    return mockEphemeralBuilder("builder-metadata.json");
  }

  private EphemeralBuilder mockEphemeralBuilder(String builderMetadata) throws IOException {
    EphemeralBuilder builder = mock(EphemeralBuilder.class);
    byte[] metadataContent = FileCopyUtils.copyToByteArray(getClass().getResourceAsStream(builderMetadata));
    BuilderMetadata metadata = BuilderMetadata.fromJson(new String(metadataContent, StandardCharsets.UTF_8));
    given(builder.getName()).willReturn(ImageReference.of("pack.local/ephemeral-builder"));
    given(builder.getBuilderMetadata()).willReturn(metadata);
    return builder;
  }

  private Answer<ContainerReference> answerWithGeneratedContainerId() {
    return (invocation) -> {
      ContainerConfig config = invocation.getArgument(0, ContainerConfig.class);
      ArrayNode command = getCommand(config);
      String name = command.get(0).asText().substring(1).replaceAll("/", "-");
      this.configs.put(name, config);
      if (invocation.getArguments().length > 1) {
        this.content.put(name, invocation.getArgument(1, ContainerContent.class));
      }
      return ContainerReference.of(name);
    };
  }

  private ArrayNode getCommand(ContainerConfig config) throws JsonProcessingException {
    JsonNode node = SharedObjectMapper.get().readTree(config.toString());
    return (ArrayNode) node.at("/Cmd");
  }

  private void assertPhaseWasRun(String name, IOConsumer<ContainerConfig> configConsumer) throws IOException {
    ContainerReference containerReference = ContainerReference.of("cnb-lifecycle-" + name);
    then(this.docker.container()).should().start(containerReference);
    then(this.docker.container()).should().logs(eq(containerReference), any());
    then(this.docker.container()).should().remove(containerReference, true);
    configConsumer.accept(this.configs.get(containerReference.toString()));
  }

  private IOConsumer<ContainerConfig> withExpectedConfig(String name) {
    return (config) -> {
      try {
        InputStream in = getClass().getResourceAsStream(name);
        String jsonString = FileCopyUtils.copyToString(new InputStreamReader(in, StandardCharsets.UTF_8));
        JSONObject json = new JSONObject(jsonString);
        if (Platform.isWindows()) {
          JSONObject hostConfig = json.getJSONObject("HostConfig");
          hostConfig.remove("SecurityOpt");
        }
        JSONAssert.assertEquals(config.toString(), json, true);
      }
      catch (JSONException ex) {
        throw new IOException(ex);
      }
    };
  }

  static class TestLifecycle extends Lifecycle {

    TestLifecycle(BuildLog log, DockerApi docker, ResolvedDockerHost dockerHost, BuildRequest request,
            EphemeralBuilder builder) {
      super(log, docker, dockerHost, request, builder);
    }

    @Override
    protected VolumeName createRandomVolumeName(String prefix) {
      return VolumeName.of(prefix + "aaaaaaaaaa");
    }

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
