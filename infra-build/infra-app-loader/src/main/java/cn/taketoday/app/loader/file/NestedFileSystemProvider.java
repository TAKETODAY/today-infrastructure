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

package cn.taketoday.app.loader.file;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ReadOnlyFileSystemException;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.taketoday.app.loader.net.protocol.nested.NestedLocation;

/**
 * {@link FileSystemProvider} implementation for {@link NestedLocation nested} jar files.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 5.0
 */
public class NestedFileSystemProvider extends FileSystemProvider {

  private final Map<Path, NestedFileSystem> fileSystems = new HashMap<>();

  @Override
  public String getScheme() {
    return "nested";
  }

  @Override
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    NestedLocation location = NestedLocation.fromUri(uri);
    Path jarPath = location.path();
    synchronized(this.fileSystems) {
      if (this.fileSystems.containsKey(jarPath)) {
        throw new FileSystemAlreadyExistsException();
      }
      NestedFileSystem fileSystem = new NestedFileSystem(this, location.path());
      this.fileSystems.put(location.path(), fileSystem);
      return fileSystem;
    }
  }

  @Override
  public FileSystem getFileSystem(URI uri) {
    NestedLocation location = NestedLocation.fromUri(uri);
    synchronized(this.fileSystems) {
      NestedFileSystem fileSystem = this.fileSystems.get(location.path());
      if (fileSystem == null) {
        throw new FileSystemNotFoundException();
      }
      return fileSystem;
    }
  }

  @Override
  public Path getPath(URI uri) {
    NestedLocation location = NestedLocation.fromUri(uri);
    synchronized(this.fileSystems) {
      NestedFileSystem fileSystem = this.fileSystems.computeIfAbsent(location.path(),
              (path) -> new NestedFileSystem(this, path));
      fileSystem.installZipFileSystemIfNecessary(location.nestedEntryName());
      return fileSystem.getPath(location.nestedEntryName());
    }
  }

  void removeFileSystem(NestedFileSystem fileSystem) {
    synchronized(this.fileSystems) {
      this.fileSystems.remove(fileSystem.getJarPath());
    }
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs)
          throws IOException {
    NestedPath nestedPath = NestedPath.cast(path);
    return new NestedByteChannel(nestedPath.getJarPath(), nestedPath.getNestedEntryName());
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
    throw new NotDirectoryException(NestedPath.cast(dir).toString());
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public void delete(Path path) throws IOException {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    throw new ReadOnlyFileSystemException();
  }

  @Override
  public boolean isSameFile(Path path, Path path2) throws IOException {
    return path.equals(path2);
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    return false;
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    NestedPath nestedPath = NestedPath.cast(path);
    nestedPath.assertExists();
    return new NestedFileStore(nestedPath.getFileSystem());
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    Path jarPath = getJarPath(path);
    jarPath.getFileSystem().provider().checkAccess(jarPath, modes);
  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
    Path jarPath = getJarPath(path);
    return jarPath.getFileSystem().provider().getFileAttributeView(jarPath, type, options);
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
          throws IOException {
    Path jarPath = getJarPath(path);
    return jarPath.getFileSystem().provider().readAttributes(jarPath, type, options);
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
    Path jarPath = getJarPath(path);
    return jarPath.getFileSystem().provider().readAttributes(jarPath, attributes, options);
  }

  protected Path getJarPath(Path path) {
    return NestedPath.cast(path).getJarPath();
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
    throw new ReadOnlyFileSystemException();
  }

}
