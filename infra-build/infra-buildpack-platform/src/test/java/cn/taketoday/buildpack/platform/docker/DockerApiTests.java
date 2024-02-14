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

package cn.taketoday.buildpack.platform.docker;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.buildpack.platform.docker.DockerApi.ContainerApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.ImageApi;
import cn.taketoday.buildpack.platform.docker.DockerApi.VolumeApi;
import cn.taketoday.buildpack.platform.docker.transport.HttpTransport;
import cn.taketoday.buildpack.platform.docker.transport.HttpTransport.Response;
import cn.taketoday.buildpack.platform.docker.type.ContainerConfig;
import cn.taketoday.buildpack.platform.docker.type.ContainerContent;
import cn.taketoday.buildpack.platform.docker.type.ContainerReference;
import cn.taketoday.buildpack.platform.docker.type.ContainerStatus;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageArchive;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;
import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.io.Owner;
import cn.taketoday.buildpack.platform.io.TarArchive;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link DockerApi}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Rafael Ceccone
 * @author Moritz Halbritter
 */
@ExtendWith(MockitoExtension.class)
class DockerApiTests {

  private static final String API_URL = "/" + DockerApi.API_VERSION;

  private static final String IMAGES_URL = API_URL + "/images";

  private static final String CONTAINERS_URL = API_URL + "/containers";

  private static final String VOLUMES_URL = API_URL + "/volumes";

  @Mock
  private HttpTransport http;

  private DockerApi dockerApi;

  @BeforeEach
  void setup() {
    this.dockerApi = new DockerApi(this.http);
  }

  private HttpTransport http() {
    return this.http;
  }

  private Response emptyResponse() {
    return responseOf(null);
  }

  private Response responseOf(String name) {
    return new Response() {

      @Override
      public void close() {
      }

      @Override
      public InputStream getContent() {
        if (name == null) {
          return null;
        }
        return getClass().getResourceAsStream(name);
      }

    };
  }

  @Test
  void createDockerApi() {
    DockerApi api = new DockerApi();
    assertThat(api).isNotNull();
  }

  @Nested
  class ImageDockerApiTests {

    private ImageApi api;

    @Mock
    private UpdateListener<PullImageUpdateEvent> pullListener;

    @Mock
    private UpdateListener<PushImageUpdateEvent> pushListener;

    @Mock
    private UpdateListener<LoadImageUpdateEvent> loadListener;

    @Captor
    private ArgumentCaptor<IOConsumer<OutputStream>> writer;

    @BeforeEach
    void setup() {
      this.api = DockerApiTests.this.dockerApi.image();
    }

    @Test
    void pullWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.pull(null, this.pullListener))
              .withMessage("Reference is required");
    }

    @Test
    void pullWhenListenerIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.pull(ImageReference.of("ubuntu"), null))
              .withMessage("Listener is required");
    }

    @Test
    void pullPullsImageAndProducesEvents() throws Exception {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      URI createUri = new URI(IMAGES_URL + "/create?fromImage=gcr.io%2Fpaketo-buildpacks%2Fbuilder%3Abase");
      URI imageUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/json");
      given(http().post(eq(createUri), isNull())).willReturn(responseOf("pull-stream.json"));
      given(http().get(imageUri)).willReturn(responseOf("type/image.json"));
      Image image = this.api.pull(reference, this.pullListener);
      assertThat(image.getLayers()).hasSize(46);
      InOrder ordered = inOrder(this.pullListener);
      ordered.verify(this.pullListener).onStart();
      ordered.verify(this.pullListener, times(595)).onUpdate(any());
      ordered.verify(this.pullListener).onFinish();
    }

    @Test
    void pullWithRegistryAuthPullsImageAndProducesEvents() throws Exception {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      URI createUri = new URI(IMAGES_URL + "/create?fromImage=gcr.io%2Fpaketo-buildpacks%2Fbuilder%3Abase");
      URI imageUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/json");
      given(http().post(eq(createUri), eq("auth token"))).willReturn(responseOf("pull-stream.json"));
      given(http().get(imageUri)).willReturn(responseOf("type/image.json"));
      Image image = this.api.pull(reference, this.pullListener, "auth token");
      assertThat(image.getLayers()).hasSize(46);
      InOrder ordered = inOrder(this.pullListener);
      ordered.verify(this.pullListener).onStart();
      ordered.verify(this.pullListener, times(595)).onUpdate(any());
      ordered.verify(this.pullListener).onFinish();
    }

    @Test
    void pushWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.push(null, this.pushListener, null))
              .withMessage("Reference is required");
    }

    @Test
    void pushWhenListenerIsNullThrowsException() {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> this.api.push(ImageReference.of("ubuntu"), null, null))
              .withMessage("Listener is required");
    }

    @Test
    void pushPushesImageAndProducesEvents() throws Exception {
      ImageReference reference = ImageReference.of("localhost:5000/ubuntu");
      URI pushUri = new URI(IMAGES_URL + "/localhost:5000/ubuntu/push");
      given(http().post(pushUri, "auth token")).willReturn(responseOf("push-stream.json"));
      this.api.push(reference, this.pushListener, "auth token");
      InOrder ordered = inOrder(this.pushListener);
      ordered.verify(this.pushListener).onStart();
      ordered.verify(this.pushListener, times(44)).onUpdate(any());
      ordered.verify(this.pushListener).onFinish();
    }

    @Test
    void pushWithErrorInStreamThrowsException() throws Exception {
      ImageReference reference = ImageReference.of("localhost:5000/ubuntu");
      URI pushUri = new URI(IMAGES_URL + "/localhost:5000/ubuntu/push");
      given(http().post(pushUri, "auth token")).willReturn(responseOf("push-stream-with-error.json"));
      assertThatIllegalStateException()
              .isThrownBy(() -> this.api.push(reference, this.pushListener, "auth token"))
              .withMessageContaining("test message");
    }

    @Test
    void loadWhenArchiveIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.load(null, UpdateListener.none()))
              .withMessage("Archive is required");
    }

    @Test
    void loadWhenListenerIsNullThrowsException() {
      ImageArchive archive = mock(ImageArchive.class);
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.load(archive, null))
              .withMessage("Listener is required");
    }

    @Test
      // gh-23130
    void loadWithEmptyResponseThrowsException() throws Exception {
      Image image = Image.of(getClass().getResourceAsStream("type/image.json"));
      ImageArchive archive = ImageArchive.from(image);
      URI loadUri = new URI(IMAGES_URL + "/load");
      given(http().post(eq(loadUri), eq("application/x-tar"), any())).willReturn(emptyResponse());
      assertThatIllegalStateException().isThrownBy(() -> this.api.load(archive, this.loadListener))
              .withMessageContaining("Invalid response received");
    }

    @Test
    void loadLoadsImage() throws Exception {
      Image image = Image.of(getClass().getResourceAsStream("type/image.json"));
      ImageArchive archive = ImageArchive.from(image);
      URI loadUri = new URI(IMAGES_URL + "/load");
      given(http().post(eq(loadUri), eq("application/x-tar"), any())).willReturn(responseOf("load-stream.json"));
      this.api.load(archive, this.loadListener);
      InOrder ordered = inOrder(this.loadListener);
      ordered.verify(this.loadListener).onStart();
      ordered.verify(this.loadListener).onUpdate(any());
      ordered.verify(this.loadListener).onFinish();
      then(http()).should().post(any(), any(), this.writer.capture());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      this.writer.getValue().accept(out);
      assertThat(out.toByteArray()).hasSizeGreaterThan(21000);
    }

    @Test
    void removeWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.remove(null, true))
              .withMessage("Reference is required");
    }

    @Test
    void removeRemovesContainer() throws Exception {
      ImageReference reference = ImageReference
              .of("ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
      URI removeUri = new URI(IMAGES_URL
              + "/docker.io/library/ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
      given(http().delete(removeUri)).willReturn(emptyResponse());
      this.api.remove(reference, false);
      then(http()).should().delete(removeUri);
    }

    @Test
    void removeWhenForceIsTrueRemovesContainer() throws Exception {
      ImageReference reference = ImageReference
              .of("ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d");
      URI removeUri = new URI(IMAGES_URL
              + "/docker.io/library/ubuntu@sha256:6e9f67fa63b0323e9a1e587fd71c561ba48a034504fb804fd26fd8800039835d?force=1");
      given(http().delete(removeUri)).willReturn(emptyResponse());
      this.api.remove(reference, true);
      then(http()).should().delete(removeUri);
    }

    @Test
    void inspectWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.inspect(null))
              .withMessage("Reference is required");
    }

    @Test
    void inspectInspectImage() throws Exception {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      URI imageUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/json");
      given(http().get(imageUri)).willReturn(responseOf("type/image.json"));
      Image image = this.api.inspect(reference);
      assertThat(image.getLayers()).hasSize(46);
    }

    @Test
    void exportLayersWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.exportLayerFiles(null, (name, archive) -> {
      })).withMessage("Reference is required");
    }

    @Test
    void exportLayersWhenExportsIsNullThrowsException() {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.exportLayerFiles(reference, null))
              .withMessage("Exports is required");
    }

    @Test
    void exportLayersExportsLayerTars() throws Exception {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      URI exportUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/get");
      given(DockerApiTests.this.http.get(exportUri)).willReturn(responseOf("export.tar"));
      MultiValueMap<String, String> contents = new LinkedMultiValueMap<>();
      this.api.exportLayers(reference, (name, archive) -> {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        archive.writeTo(out);
        try (TarArchiveInputStream in = new TarArchiveInputStream(
                new ByteArrayInputStream(out.toByteArray()))) {
          TarArchiveEntry entry = in.getNextEntry();
          while (entry != null) {
            contents.add(name, entry.getName());
            entry = in.getNextEntry();
          }
        }
      });
      assertThat(contents).hasSize(3)
              .containsKeys("70bb7a3115f3d5c01099852112c7e05bf593789e510468edb06b6a9a11fa3b73/layer.tar",
                      "74a9a50ece13c025cf10e9110d9ddc86c995079c34e2a22a28d1a3d523222c6e/layer.tar",
                      "a69532b5b92bb891fbd9fa1a6b3af9087ea7050255f59ba61a796f8555ecd783/layer.tar");
      assertThat(contents.get("70bb7a3115f3d5c01099852112c7e05bf593789e510468edb06b6a9a11fa3b73/layer.tar"))
              .containsExactly("/cnb/order.toml");
      assertThat(contents.get("74a9a50ece13c025cf10e9110d9ddc86c995079c34e2a22a28d1a3d523222c6e/layer.tar"))
              .containsExactly("/cnb/stack.toml");
    }

    @Test
    void exportLayersWithSymlinksExportsLayerTars() throws Exception {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      URI exportUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/get");
      given(DockerApiTests.this.http.get(exportUri)).willReturn(responseOf("export-symlinks.tar"));
      MultiValueMap<String, String> contents = new LinkedMultiValueMap<>();
      this.api.exportLayers(reference, (name, archive) -> {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        archive.writeTo(out);
        try (TarArchiveInputStream in = new TarArchiveInputStream(
                new ByteArrayInputStream(out.toByteArray()))) {
          TarArchiveEntry entry = in.getNextEntry();
          while (entry != null) {
            contents.add(name, entry.getName());
            entry = in.getNextEntry();
          }
        }
      });
      assertThat(contents).hasSize(3)
              .containsKeys("6aa3691a73805f608e5fce69fb6bc89aec8362f58a6b4be2682515e9cfa3cc1a.tar",
                      "762e198f655bc2580ef3e56b538810fd2b9981bd707f8a44c70344b58f9aee68.tar",
                      "d3cc975ad97fdfbb73d9daf157e7f658d6117249fd9c237e3856ad173c87e1d2.tar");
      assertThat(contents.get("d3cc975ad97fdfbb73d9daf157e7f658d6117249fd9c237e3856ad173c87e1d2.tar"))
              .containsExactly("/cnb/order.toml");
      assertThat(contents.get("762e198f655bc2580ef3e56b538810fd2b9981bd707f8a44c70344b58f9aee68.tar"))
              .containsExactly("/cnb/stack.toml");
    }

    @Test
    void exportLayerFilesDeletesTempFiles() throws Exception {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      URI exportUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/get");
      given(DockerApiTests.this.http.get(exportUri)).willReturn(responseOf("export.tar"));
      List<Path> layerFilePaths = new ArrayList<>();
      this.api.exportLayerFiles(reference, (name, path) -> layerFilePaths.add(path));
      layerFilePaths.forEach((path) -> assertThat(path.toFile()).doesNotExist());
    }

    @Test
    void exportLayersWithNoManifestThrowsException() throws Exception {
      ImageReference reference = ImageReference.of("gcr.io/paketo-buildpacks/builder:base");
      URI exportUri = new URI(IMAGES_URL + "/gcr.io/paketo-buildpacks/builder:base/get");
      given(DockerApiTests.this.http.get(exportUri)).willReturn(responseOf("export-no-manifest.tar"));
      assertThatIllegalArgumentException()
              .isThrownBy(() -> this.api.exportLayerFiles(reference, (name, archive) -> {
              }))
              .withMessageContaining("Manifest not found in image " + reference);
    }

    @Test
    void tagWhenReferenceIsNullThrowsException() {
      ImageReference tag = ImageReference.of("localhost:5000/ubuntu");
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.tag(null, tag))
              .withMessage("SourceReference is required");
    }

    @Test
    void tagWhenTargetIsNullThrowsException() {
      ImageReference reference = ImageReference.of("localhost:5000/ubuntu");
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.tag(reference, null))
              .withMessage("TargetReference is required");
    }

    @Test
    void tagTagsImage() throws Exception {
      ImageReference sourceReference = ImageReference.of("localhost:5000/ubuntu");
      ImageReference targetReference = ImageReference.of("localhost:5000/ubuntu:tagged");
      URI tagURI = new URI(IMAGES_URL + "/localhost:5000/ubuntu/tag?repo=localhost%3A5000%2Fubuntu&tag=tagged");
      given(http().post(tagURI)).willReturn(emptyResponse());
      this.api.tag(sourceReference, targetReference);
      then(http()).should().post(tagURI);
    }

    @Test
    void tagRenamesImage() throws Exception {
      ImageReference sourceReference = ImageReference.of("localhost:5000/ubuntu");
      ImageReference targetReference = ImageReference.of("localhost:5000/ubuntu-2");
      URI tagURI = new URI(IMAGES_URL + "/localhost:5000/ubuntu/tag?repo=localhost%3A5000%2Fubuntu-2");
      given(http().post(tagURI)).willReturn(emptyResponse());
      this.api.tag(sourceReference, targetReference);
      then(http()).should().post(tagURI);
    }

  }

  @Nested
  class ContainerDockerApiTests {

    private ContainerApi api;

    @Captor
    private ArgumentCaptor<IOConsumer<OutputStream>> writer;

    @Mock
    private UpdateListener<LogUpdateEvent> logListener;

    @BeforeEach
    void setup() {
      this.api = DockerApiTests.this.dockerApi.container();
    }

    @Test
    void createWhenConfigIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.create(null))
              .withMessage("Config is required");
    }

    @Test
    void createCreatesContainer() throws Exception {
      ImageReference imageReference = ImageReference.of("ubuntu:bionic");
      ContainerConfig config = ContainerConfig.of(imageReference, (update) -> update.withCommand("/bin/bash"));
      URI createUri = new URI(CONTAINERS_URL + "/create");
      given(http().post(eq(createUri), eq("application/json"), any()))
              .willReturn(responseOf("create-container-response.json"));
      ContainerReference containerReference = this.api.create(config);
      assertThat(containerReference).hasToString("e90e34656806");
      then(http()).should().post(any(), any(), this.writer.capture());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      this.writer.getValue().accept(out);
      assertThat(out.toByteArray()).hasSize(config.toString().length());
    }

    @Test
    void createWhenHasContentContainerWithContent() throws Exception {
      ImageReference imageReference = ImageReference.of("ubuntu:bionic");
      ContainerConfig config = ContainerConfig.of(imageReference, (update) -> update.withCommand("/bin/bash"));
      TarArchive archive = TarArchive.of((layout) -> {
        layout.directory("/test", Owner.ROOT);
        layout.file("/test/file", Owner.ROOT, Content.of("test"));
      });
      ContainerContent content = ContainerContent.of(archive);
      URI createUri = new URI(CONTAINERS_URL + "/create");
      given(http().post(eq(createUri), eq("application/json"), any()))
              .willReturn(responseOf("create-container-response.json"));
      URI uploadUri = new URI(CONTAINERS_URL + "/e90e34656806/archive?path=%2F");
      given(http().put(eq(uploadUri), eq("application/x-tar"), any())).willReturn(emptyResponse());
      ContainerReference containerReference = this.api.create(config, content);
      assertThat(containerReference).hasToString("e90e34656806");
      then(http()).should().post(any(), any(), this.writer.capture());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      this.writer.getValue().accept(out);
      assertThat(out.toByteArray()).hasSize(config.toString().length());
      then(http()).should().put(any(), any(), this.writer.capture());
      this.writer.getValue().accept(out);
      assertThat(out.toByteArray()).hasSizeGreaterThan(2000);
    }

    @Test
    void startWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.start(null))
              .withMessage("Reference is required");
    }

    @Test
    void startStartsContainer() throws Exception {
      ContainerReference reference = ContainerReference.of("e90e34656806");
      URI startContainerUri = new URI(CONTAINERS_URL + "/e90e34656806/start");
      given(http().post(startContainerUri)).willReturn(emptyResponse());
      this.api.start(reference);
      then(http()).should().post(startContainerUri);
    }

    @Test
    void logsWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.logs(null, UpdateListener.none()))
              .withMessage("Reference is required");
    }

    @Test
    void logsWhenListenerIsNullThrowsException() {
      assertThatIllegalArgumentException()
              .isThrownBy(() -> this.api.logs(ContainerReference.of("e90e34656806"), null))
              .withMessage("Listener is required");
    }

    @Test
    void logsProducesEvents() throws Exception {
      ContainerReference reference = ContainerReference.of("e90e34656806");
      URI logsUri = new URI(CONTAINERS_URL + "/e90e34656806/logs?stdout=1&stderr=1&follow=1");
      given(http().get(logsUri)).willReturn(responseOf("log-update-event.stream"));
      this.api.logs(reference, this.logListener);
      InOrder ordered = inOrder(this.logListener);
      ordered.verify(this.logListener).onStart();
      ordered.verify(this.logListener, times(7)).onUpdate(any());
      ordered.verify(this.logListener).onFinish();
    }

    @Test
    void waitWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.wait(null))
              .withMessage("Reference is required");
    }

    @Test
    void waitReturnsStatus() throws Exception {
      ContainerReference reference = ContainerReference.of("e90e34656806");
      URI waitUri = new URI(CONTAINERS_URL + "/e90e34656806/wait");
      given(http().post(waitUri)).willReturn(responseOf("container-wait-response.json"));
      ContainerStatus status = this.api.wait(reference);
      assertThat(status.getStatusCode()).isOne();
    }

    @Test
    void removeWhenReferenceIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.remove(null, true))
              .withMessage("Reference is required");
    }

    @Test
    void removeRemovesContainer() throws Exception {
      ContainerReference reference = ContainerReference.of("e90e34656806");
      URI removeUri = new URI(CONTAINERS_URL + "/e90e34656806");
      given(http().delete(removeUri)).willReturn(emptyResponse());
      this.api.remove(reference, false);
      then(http()).should().delete(removeUri);
    }

    @Test
    void removeWhenForceIsTrueRemovesContainer() throws Exception {
      ContainerReference reference = ContainerReference.of("e90e34656806");
      URI removeUri = new URI(CONTAINERS_URL + "/e90e34656806?force=1");
      given(http().delete(removeUri)).willReturn(emptyResponse());
      this.api.remove(reference, true);
      then(http()).should().delete(removeUri);
    }

  }

  @Nested
  class VolumeDockerApiTests {

    private VolumeApi api;

    @BeforeEach
    void setup() {
      this.api = DockerApiTests.this.dockerApi.volume();
    }

    @Test
    void deleteWhenNameIsNullThrowsException() {
      assertThatIllegalArgumentException().isThrownBy(() -> this.api.delete(null, false))
              .withMessage("Name is required");
    }

    @Test
    void deleteDeletesContainer() throws Exception {
      VolumeName name = VolumeName.of("test");
      URI removeUri = new URI(VOLUMES_URL + "/test");
      given(http().delete(removeUri)).willReturn(emptyResponse());
      this.api.delete(name, false);
      then(http()).should().delete(removeUri);
    }

    @Test
    void deleteWhenForceIsTrueDeletesContainer() throws Exception {
      VolumeName name = VolumeName.of("test");
      URI removeUri = new URI(VOLUMES_URL + "/test?force=1");
      given(http().delete(removeUri)).willReturn(emptyResponse());
      this.api.delete(name, true);
      then(http()).should().delete(removeUri);
    }

  }

}
