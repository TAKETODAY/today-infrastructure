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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import cn.taketoday.buildpack.platform.docker.type.Layer;
import cn.taketoday.buildpack.platform.io.Content;
import cn.taketoday.buildpack.platform.io.FilePermissions;
import cn.taketoday.buildpack.platform.io.IOConsumer;
import cn.taketoday.buildpack.platform.io.Layout;
import cn.taketoday.buildpack.platform.io.Owner;

import cn.taketoday.lang.Assert;

/**
 * A {@link Buildpack} that references a buildpack in a directory on the local file
 * system.
 *
 * The file system must contain a buildpack descriptor named {@code buildpack.toml} in the
 * root of the directory. The contents of the directory tree will be provided as a single
 * layer to be included in the builder image.
 *
 * @author Scott Frederick
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
final class DirectoryBuildpack implements Buildpack {

  private final Path path;

  private final BuildpackCoordinates coordinates;

  private DirectoryBuildpack(Path path) {
    this.path = path;
    this.coordinates = findBuildpackCoordinates(path);
  }

  private BuildpackCoordinates findBuildpackCoordinates(Path path) {
    Path buildpackToml = path.resolve("buildpack.toml");
    Assert.isTrue(Files.exists(buildpackToml),
            () -> "Buildpack descriptor 'buildpack.toml' is required in buildpack '" + path + "'");
    try {
      try (InputStream inputStream = Files.newInputStream(buildpackToml)) {
        return BuildpackCoordinates.fromToml(inputStream, path);
      }
    }
    catch (IOException ex) {
      throw new IllegalArgumentException("Error parsing descriptor for buildpack '" + path + "'", ex);
    }
  }

  @Override
  public BuildpackCoordinates getCoordinates() {
    return this.coordinates;
  }

  @Override
  public void apply(IOConsumer<Layer> layers) throws IOException {
    layers.accept(Layer.of(this::addLayerContent));
  }

  private void addLayerContent(Layout layout) throws IOException {
    String id = this.coordinates.getSanitizedId();
    Path cnbPath = Paths.get("/cnb/buildpacks/", id, this.coordinates.getVersion());
    writeBasePathEntries(layout, cnbPath);
    Files.walkFileTree(this.path, new LayoutFileVisitor(this.path, cnbPath, layout));
  }

  private void writeBasePathEntries(Layout layout, Path basePath) throws IOException {
    int pathCount = basePath.getNameCount();
    for (int pathIndex = 1; pathIndex < pathCount + 1; pathIndex++) {
      String name = "/" + basePath.subpath(0, pathIndex) + "/";
      layout.directory(name, Owner.ROOT);
    }
  }

  /**
   * A {@link BuildpackResolver} compatible method to resolve directory buildpacks.
   *
   * @param context the resolver context
   * @param reference the buildpack reference
   * @return the resolved {@link Buildpack} or {@code null}
   */
  static Buildpack resolve(BuildpackResolverContext context, BuildpackReference reference) {
    Path path = reference.asPath();
    if (path != null && Files.exists(path) && Files.isDirectory(path)) {
      return new DirectoryBuildpack(path);
    }
    return null;
  }

  /**
   * {@link SimpleFileVisitor} to used to create the {@link Layout}.
   */
  private static class LayoutFileVisitor extends SimpleFileVisitor<Path> {

    private final Path basePath;

    private final Path layerPath;

    private final Layout layout;

    LayoutFileVisitor(Path basePath, Path layerPath, Layout layout) {
      this.basePath = basePath;
      this.layerPath = layerPath;
      this.layout = layout;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
      if (!dir.equals(this.basePath)) {
        this.layout.directory(relocate(dir), Owner.ROOT, getMode(dir));
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
      this.layout.file(relocate(file), Owner.ROOT, getMode(file), Content.of(file.toFile()));
      return FileVisitResult.CONTINUE;
    }

    private int getMode(Path path) throws IOException {
      try {
        return FilePermissions.umaskForPath(path);
      }
      catch (IllegalStateException ex) {
        throw new IllegalStateException(
                "Buildpack content in a directory is not supported on this operating system");
      }
    }

    private String relocate(Path path) {
      Path node = path.subpath(this.basePath.getNameCount(), path.getNameCount());
      return Paths.get(this.layerPath.toString(), node.toString()).toString();
    }

  }

}
