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

package cn.taketoday.buildpack.platform.docker;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hc.core5.net.URIBuilder;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import cn.taketoday.buildpack.platform.docker.configuration.DockerConfiguration.DockerHostConfiguration;
import cn.taketoday.buildpack.platform.docker.transport.HttpTransport;
import cn.taketoday.buildpack.platform.docker.transport.HttpTransport.Response;
import cn.taketoday.buildpack.platform.docker.type.ContainerConfig;
import cn.taketoday.buildpack.platform.docker.type.ContainerContent;
import cn.taketoday.buildpack.platform.docker.type.ContainerReference;
import cn.taketoday.buildpack.platform.docker.type.ContainerStatus;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageArchive;
import cn.taketoday.buildpack.platform.docker.type.ImageArchiveManifest;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.VolumeName;
import cn.taketoday.buildpack.platform.io.IOBiConsumer;
import cn.taketoday.buildpack.platform.io.TarArchive;
import cn.taketoday.buildpack.platform.json.JsonStream;
import cn.taketoday.buildpack.platform.json.SharedObjectMapper;
import cn.taketoday.core.ApplicationTemp;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StreamUtils;
import cn.taketoday.util.StringUtils;

/**
 * Provides access to the limited set of Docker APIs needed by pack.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Rafael Ceccone
 * @author Moritz Halbritter
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class DockerApi {

  private static final List<String> FORCE_PARAMS = List.of("force", "1");

  static final String API_VERSION = "v1.24";

  private final HttpTransport http;

  private final JsonStream jsonStream;

  private final ImageApi image;

  private final ContainerApi container;

  private final VolumeApi volume;

  /**
   * Create a new {@link DockerApi} instance.
   */
  public DockerApi() {
    this(HttpTransport.create(null));
  }

  /**
   * Create a new {@link DockerApi} instance.
   *
   * @param dockerHost the Docker daemon host information
   */
  public DockerApi(DockerHostConfiguration dockerHost) {
    this(HttpTransport.create(dockerHost));
  }

  /**
   * Create a new {@link DockerApi} instance backed by a specific {@link HttpTransport}
   * implementation.
   *
   * @param http the http implementation
   */
  DockerApi(HttpTransport http) {
    this.http = http;
    this.jsonStream = new JsonStream(SharedObjectMapper.get());
    this.image = new ImageApi();
    this.container = new ContainerApi();
    this.volume = new VolumeApi();
  }

  private HttpTransport http() {
    return this.http;
  }

  private JsonStream jsonStream() {
    return this.jsonStream;
  }

  private URI buildUrl(String path, Collection<?> params) {
    return buildUrl(path, (params != null) ? params.toArray() : null);
  }

  private URI buildUrl(String path, @Nullable Object... params) {
    try {
      URIBuilder builder = new URIBuilder("/" + API_VERSION + path);
      if (params != null) {
        int param = 0;
        while (param < params.length) {
          builder.addParameter(Objects.toString(params[param++]), Objects.toString(params[param++]));
        }
      }
      return builder.build();
    }
    catch (URISyntaxException ex) {
      throw new IllegalStateException(ex);
    }
  }

  /**
   * Return the Docker API for image operations.
   *
   * @return the image API
   */
  public ImageApi image() {
    return this.image;
  }

  /**
   * Return the Docker API for container operations.
   *
   * @return the container API
   */
  public ContainerApi container() {
    return this.container;
  }

  public VolumeApi volume() {
    return this.volume;
  }

  /**
   * Docker API for image operations.
   */
  public class ImageApi {

    ImageApi() {
    }

    /**
     * Pull an image from a registry.
     *
     * @param reference the image reference to pull
     * @param listener a pull listener to receive update events
     * @return the {@link ImageApi pulled image} instance
     * @throws IOException on IO error
     */
    public Image pull(ImageReference reference, UpdateListener<PullImageUpdateEvent> listener) throws IOException {
      return pull(reference, listener, null);
    }

    /**
     * Pull an image from a registry.
     *
     * @param reference the image reference to pull
     * @param listener a pull listener to receive update events
     * @param registryAuth registry authentication credentials
     * @return the {@link ImageApi pulled image} instance
     * @throws IOException on IO error
     */
    public Image pull(ImageReference reference, UpdateListener<PullImageUpdateEvent> listener, String registryAuth)
            throws IOException {
      Assert.notNull(reference, "Reference is required");
      Assert.notNull(listener, "Listener is required");
      URI createUri = buildUrl("/images/create", "fromImage", reference);
      DigestCaptureUpdateListener digestCapture = new DigestCaptureUpdateListener();
      listener.onStart();
      try {
        try (Response response = http().post(createUri, registryAuth)) {
          jsonStream().get(response.getContent(), PullImageUpdateEvent.class, (event) -> {
            digestCapture.onUpdate(event);
            listener.onUpdate(event);
          });
        }
        return inspect(reference);
      }
      finally {
        listener.onFinish();
      }
    }

    /**
     * Push an image to a registry.
     *
     * @param reference the image reference to push
     * @param listener a push listener to receive update events
     * @param registryAuth registry authentication credentials
     * @throws IOException on IO error
     */
    public void push(ImageReference reference, UpdateListener<PushImageUpdateEvent> listener, String registryAuth)
            throws IOException {
      Assert.notNull(reference, "Reference is required");
      Assert.notNull(listener, "Listener is required");
      URI pushUri = buildUrl("/images/" + reference + "/push");
      ErrorCaptureUpdateListener errorListener = new ErrorCaptureUpdateListener();
      listener.onStart();
      try {
        try (Response response = http().post(pushUri, registryAuth)) {
          jsonStream().get(response.getContent(), PushImageUpdateEvent.class, (event) -> {
            errorListener.onUpdate(event);
            listener.onUpdate(event);
          });
        }
      }
      finally {
        listener.onFinish();
      }
    }

    /**
     * Load an {@link ImageArchive} into Docker.
     *
     * @param archive the archive to load
     * @param listener a pull listener to receive update events
     * @throws IOException on IO error
     */
    public void load(ImageArchive archive, UpdateListener<LoadImageUpdateEvent> listener) throws IOException {
      Assert.notNull(archive, "Archive is required");
      Assert.notNull(listener, "Listener is required");
      URI loadUri = buildUrl("/images/load");
      StreamCaptureUpdateListener streamListener = new StreamCaptureUpdateListener();
      listener.onStart();
      try {
        try (Response response = http().post(loadUri, "application/x-tar", archive::writeTo)) {
          jsonStream().get(response.getContent(), LoadImageUpdateEvent.class, (event) -> {
            streamListener.onUpdate(event);
            listener.onUpdate(event);
          });
        }
        Assert.state(StringUtils.hasText(streamListener.getCapturedStream()),
                "Invalid response received when loading image "
                        + ((archive.getTag() != null) ? "\"" + archive.getTag() + "\"" : ""));
      }
      finally {
        listener.onFinish();
      }
    }

    /**
     * Export the layers of an image as {@link TarArchive}s.
     *
     * @param reference the reference to export
     * @param exports a consumer to receive the layers (contents can only be accessed
     * during the callback)
     * @throws IOException on IO error
     */
    public void exportLayers(ImageReference reference, IOBiConsumer<String, TarArchive> exports)
            throws IOException {
      exportLayerFiles(reference, (name, path) -> {
        try (InputStream in = Files.newInputStream(path)) {
          TarArchive archive = (out) -> StreamUtils.copy(in, out);
          exports.accept(name, archive);
        }
      });
    }

    /**
     * Export the layers of an image as paths to layer tar files.
     *
     * @param reference the reference to export
     * @param exports a consumer to receive the layer tar file paths (file can only be
     * accessed during the callback)
     * @throws IOException on IO error
     */
    public void exportLayerFiles(ImageReference reference, IOBiConsumer<String, Path> exports) throws IOException {
      Assert.notNull(reference, "Reference is required");
      Assert.notNull(exports, "Exports is required");
      URI saveUri = buildUrl("/images/" + reference + "/get");
      Response response = http().get(saveUri);
      ImageArchiveManifest manifest = null;
      Map<String, Path> layerFiles = new HashMap<>();
      try (TarArchiveInputStream tar = new TarArchiveInputStream(response.getContent())) {
        TarArchiveEntry entry = tar.getNextTarEntry();
        while (entry != null) {
          if (entry.getName().equals("manifest.json")) {
            manifest = readManifest(tar);
          }
          if (entry.getName().endsWith(".tar")) {
            layerFiles.put(entry.getName(), copyToTemp(tar));
          }
          entry = tar.getNextTarEntry();
        }
      }
      Assert.notNull(manifest, "Manifest not found in image " + reference);
      for (Map.Entry<String, Path> entry : layerFiles.entrySet()) {
        String name = entry.getKey();
        Path path = entry.getValue();
        if (manifestContainsLayerEntry(manifest, name)) {
          exports.accept(name, path);
        }
        Files.delete(path);
      }
    }

    /**
     * Remove a specific image.
     *
     * @param reference the reference the remove
     * @param force if removal should be forced
     * @throws IOException on IO error
     */
    public void remove(ImageReference reference, boolean force) throws IOException {
      Assert.notNull(reference, "Reference is required");
      Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
      URI uri = buildUrl("/images/" + reference, params);
      http().delete(uri).close();
    }

    /**
     * Inspect an image.
     *
     * @param reference the image reference
     * @return the image from the local repository
     * @throws IOException on IO error
     */
    public Image inspect(ImageReference reference) throws IOException {
      Assert.notNull(reference, "Reference is required");
      URI imageUri = buildUrl("/images/" + reference + "/json");
      try (Response response = http().get(imageUri)) {
        return Image.of(response.getContent());
      }
    }

    public void tag(ImageReference sourceReference, ImageReference targetReference) throws IOException {
      Assert.notNull(sourceReference, "SourceReference is required");
      Assert.notNull(targetReference, "TargetReference is required");
      String tag = targetReference.getTag();
      String path = "/images/" + sourceReference + "/tag";
      URI uri = (tag != null) ? buildUrl(path, "repo", targetReference.inTaglessForm(), "tag", tag)
                              : buildUrl(path, "repo", targetReference);
      http().post(uri).close();
    }

    private ImageArchiveManifest readManifest(TarArchiveInputStream tar) throws IOException {
      String manifestContent = new BufferedReader(new InputStreamReader(tar, StandardCharsets.UTF_8)).lines()
              .collect(Collectors.joining());
      return ImageArchiveManifest.of(new ByteArrayInputStream(manifestContent.getBytes(StandardCharsets.UTF_8)));
    }

    private Path copyToTemp(TarArchiveInputStream in) throws IOException {
      Path path = ApplicationTemp.createFile("create-builder-scratch");
      try (OutputStream out = Files.newOutputStream(path)) {
        StreamUtils.copy(in, out);
      }
      return path;
    }

    private boolean manifestContainsLayerEntry(ImageArchiveManifest manifest, String layerId) {
      return manifest.getEntries().stream().anyMatch((content) -> content.getLayers().contains(layerId));
    }

  }

  /**
   * Docker API for container operations.
   */
  public class ContainerApi {

    ContainerApi() { }

    /**
     * Create a new container a {@link ContainerConfig}.
     *
     * @param config the container config
     * @param contents additional contents to include
     * @return a {@link ContainerReference} for the newly created container
     * @throws IOException on IO error
     */
    public ContainerReference create(ContainerConfig config, ContainerContent... contents) throws IOException {
      Assert.notNull(config, "Config is required");
      Assert.noNullElements(contents, "Contents must not contain null elements");
      ContainerReference containerReference = createContainer(config);
      for (ContainerContent content : contents) {
        uploadContainerContent(containerReference, content);
      }
      return containerReference;
    }

    private ContainerReference createContainer(ContainerConfig config) throws IOException {
      URI createUri = buildUrl("/containers/create");
      try (Response response = http().post(createUri, "application/json", config::writeTo)) {
        return ContainerReference
                .of(SharedObjectMapper.get().readTree(response.getContent()).at("/Id").asText());
      }
    }

    private void uploadContainerContent(ContainerReference reference, ContainerContent content) throws IOException {
      URI uri = buildUrl("/containers/" + reference + "/archive", "path", content.getDestinationPath());
      http().put(uri, "application/x-tar", content.getArchive()::writeTo).close();
    }

    /**
     * Start a specific container.
     *
     * @param reference the container reference to start
     * @throws IOException on IO error
     */
    public void start(ContainerReference reference) throws IOException {
      Assert.notNull(reference, "Reference is required");
      URI uri = buildUrl("/containers/" + reference + "/start");
      http().post(uri).close();
    }

    /**
     * Return and follow logs for a specific container.
     *
     * @param reference the container reference
     * @param listener a listener to receive log update events
     * @throws IOException on IO error
     */
    public void logs(ContainerReference reference, UpdateListener<LogUpdateEvent> listener) throws IOException {
      Assert.notNull(reference, "Reference is required");
      Assert.notNull(listener, "Listener is required");
      Object[] params = { "stdout", "1", "stderr", "1", "follow", "1" };
      URI uri = buildUrl("/containers/" + reference + "/logs", params);
      listener.onStart();
      try {
        try (Response response = http().get(uri)) {
          LogUpdateEvent.readAll(response.getContent(), listener::onUpdate);
        }
      }
      finally {
        listener.onFinish();
      }
    }

    /**
     * Wait for a container to stop and retrieve the status.
     *
     * @param reference the container reference
     * @return a {@link ContainerStatus} indicating the exit status of the container
     * @throws IOException on IO error
     */
    public ContainerStatus wait(ContainerReference reference) throws IOException {
      Assert.notNull(reference, "Reference is required");
      URI uri = buildUrl("/containers/" + reference + "/wait");
      Response response = http().post(uri);
      return ContainerStatus.of(response.getContent());
    }

    /**
     * Remove a specific container.
     *
     * @param reference the container to remove
     * @param force if removal should be forced
     * @throws IOException on IO error
     */
    public void remove(ContainerReference reference, boolean force) throws IOException {
      Assert.notNull(reference, "Reference is required");
      Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
      URI uri = buildUrl("/containers/" + reference, params);
      http().delete(uri).close();
    }

  }

  /**
   * Docker API for volume operations.
   */
  public class VolumeApi {

    VolumeApi() {
    }

    /**
     * Delete a volume.
     *
     * @param name the name of the volume to delete
     * @param force if the deletion should be forced
     * @throws IOException on IO error
     */
    public void delete(VolumeName name, boolean force) throws IOException {
      Assert.notNull(name, "Name is required");
      Collection<String> params = force ? FORCE_PARAMS : Collections.emptySet();
      URI uri = buildUrl("/volumes/" + name, params);
      http().delete(uri).close();
    }

  }

  /**
   * {@link UpdateListener} used to capture the image digest.
   */
  private static class DigestCaptureUpdateListener implements UpdateListener<ProgressUpdateEvent> {

    private static final String PREFIX = "Digest:";

    private String digest;

    @Override
    public void onUpdate(ProgressUpdateEvent event) {
      String status = event.getStatus();
      if (status != null && status.startsWith(PREFIX)) {
        String digest = status.substring(PREFIX.length()).trim();
        Assert.state(this.digest == null || this.digest.equals(digest), "Different digests IDs provided");
        this.digest = digest;
      }
    }

  }

  /**
   * {@link UpdateListener} used to ensure an image load response stream.
   */
  private static class StreamCaptureUpdateListener implements UpdateListener<LoadImageUpdateEvent> {

    private String stream;

    @Override
    public void onUpdate(LoadImageUpdateEvent event) {
      this.stream = event.getStream();
    }

    String getCapturedStream() {
      return this.stream;
    }

  }

  /**
   * {@link UpdateListener} used to capture the details of an error in a response
   * stream.
   */
  private static class ErrorCaptureUpdateListener implements UpdateListener<PushImageUpdateEvent> {

    @Override
    public void onUpdate(PushImageUpdateEvent event) {
      Assert.state(event.getErrorDetail() == null,
              () -> "Error response received when pushing image: " + event.getErrorDetail().getMessage());
    }

  }

}
