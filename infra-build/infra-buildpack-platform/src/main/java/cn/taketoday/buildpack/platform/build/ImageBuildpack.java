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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import cn.taketoday.buildpack.platform.build.BuildpackLayersMetadata.BuildpackLayerDetails;
import cn.taketoday.buildpack.platform.docker.transport.DockerEngineException;
import cn.taketoday.buildpack.platform.docker.type.Image;
import cn.taketoday.buildpack.platform.docker.type.ImageReference;
import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.docker.type.LayerId;
import cn.taketoday.buildpack.platform.io.IOConsumer;

import cn.taketoday.util.StreamUtils;

/**
 * A {@link Buildpack} that references a buildpack contained in an OCI image.
 *
 * The reference must be an OCI image reference. The reference can optionally contain a
 * prefix {@code docker://} to unambiguously identify it as an image buildpack reference.
 *
 * @author Scott Frederick
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class ImageBuildpack implements Buildpack {

  private static final String PREFIX = "docker://";

  private final BuildpackCoordinates coordinates;

  private final ExportedLayers exportedLayers;

  private ImageBuildpack(BuildpackResolverContext context, ImageReference imageReference) {
    ImageReference reference = imageReference.inTaggedOrDigestForm();
    try {
      Image image = context.fetchImage(reference, ImageType.BUILDPACK);
      BuildpackMetadata buildpackMetadata = BuildpackMetadata.fromImage(image);
      this.coordinates = BuildpackCoordinates.fromBuildpackMetadata(buildpackMetadata);
      this.exportedLayers = (!buildpackExistsInBuilder(context, image.getLayers()))
                            ? new ExportedLayers(context, reference) : null;
    }
    catch (IOException | DockerEngineException ex) {
      throw new IllegalArgumentException("Error pulling buildpack image '" + reference + "'", ex);
    }
  }

  private boolean buildpackExistsInBuilder(BuildpackResolverContext context, List<LayerId> imageLayers) {
    BuildpackLayerDetails buildpackLayerDetails = context.getBuildpackLayersMetadata()
            .getBuildpack(this.coordinates.getId(), this.coordinates.getVersion());
    String layerDiffId = (buildpackLayerDetails != null) ? buildpackLayerDetails.getLayerDiffId() : null;
    return (layerDiffId != null) && imageLayers.stream().map(LayerId::toString).anyMatch(layerDiffId::equals);
  }

  @Override
  public BuildpackCoordinates getCoordinates() {
    return this.coordinates;
  }

  @Override
  public void apply(IOConsumer<Layer> layers) throws IOException {
    if (this.exportedLayers != null) {
      this.exportedLayers.apply(layers);
    }
  }

  /**
   * A {@link BuildpackResolver} compatible method to resolve image buildpacks.
   *
   * @param context the resolver context
   * @param reference the buildpack reference
   * @return the resolved {@link Buildpack} or {@code null}
   */
  static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
    boolean unambiguous = reference.hasPrefix(PREFIX);
    try {
      ImageReference imageReference = ImageReference
              .of((unambiguous) ? reference.getSubReference(PREFIX) : reference.toString());
      return new ImageBuildpack(context, imageReference);
    }
    catch (IllegalArgumentException ex) {
      if (unambiguous) {
        throw ex;
      }
      return null;
    }
  }

  private static class ExportedLayers {

    private final List<Path> layerFiles;

    ExportedLayers(BuildpackResolverContext context, ImageReference imageReference) throws IOException {
      List<Path> layerFiles = new ArrayList<>();
      context.exportImageLayers(imageReference, (name, path) -> layerFiles.add(copyToTemp(path)));
      this.layerFiles = Collections.unmodifiableList(layerFiles);
    }

    private Path copyToTemp(Path path) throws IOException {
      Path outputPath = Files.createTempFile("create-builder-scratch-", null);
      try (OutputStream out = Files.newOutputStream(outputPath)) {
        copyLayerTar(path, out);
      }
      return outputPath;
    }

    private void copyLayerTar(Path path, OutputStream out) throws IOException {
      try (TarArchiveInputStream tarIn = new TarArchiveInputStream(Files.newInputStream(path));
              TarArchiveOutputStream tarOut = new TarArchiveOutputStream(out)) {
        tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
        TarArchiveEntry entry = tarIn.getNextTarEntry();
        while (entry != null) {
          tarOut.putArchiveEntry(entry);
          StreamUtils.copy(tarIn, tarOut);
          tarOut.closeArchiveEntry();
          entry = tarIn.getNextTarEntry();
        }
        tarOut.finish();
      }
    }

    void apply(IOConsumer<Layer> layers) throws IOException {
      for (Path path : this.layerFiles) {
        layers.accept(Layer.fromTarArchive((out) -> {
          InputStream in = Files.newInputStream(path);
          StreamUtils.copy(in, out);
        }));
        Files.delete(path);
      }
    }

  }

}
