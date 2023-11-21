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

package cn.taketoday.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * {@link Resource} implementation for {@link java.nio.file.Path} handles,
 * performing all operations and transformations via the {@code Path} API.
 * Supports resolution as a {@link File} and also as a {@link URL}.
 * Implements the extended {@link WritableResource} interface.
 *
 * <p>{@link java.nio.file.Path} support is also available
 * in {@link FileSystemResource#FileSystemResource(Path) FileSystemResource},
 * applying Infra standard String-based path transformations but
 * performing all operations via the {@link java.nio.file.Files} API.
 * This {@code PathResource} is effectively a pure {@code java.nio.path.Path}
 * based alternative with different {@code createRelative} behavior.
 *
 * @author Philippe Marschall
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 * @see FileSystemResource
 * @since 4.0 2022/3/25 11:45
 */
public class PathResource extends AbstractResource implements WritableResource {

  private final Path path;

  /**
   * Create a new {@code PathResource} from a {@link Path} handle.
   * <p>Note: Unlike {@link FileSystemResource}, when building relative resources
   * via {@link #createRelative}, the relative path will be built <i>underneath</i>
   * the given root: e.g. Paths.get("C:/dir1/"), relative path "dir2" &rarr; "C:/dir1/dir2"!
   *
   * @param path a Path handle
   */
  public PathResource(Path path) {
    Assert.notNull(path, "Path is required");
    this.path = path.normalize();
  }

  /**
   * Create a new {@code PathResource} from a path string.
   * <p>Note: Unlike {@link FileSystemResource}, when building relative resources
   * via {@link #createRelative}, the relative path will be built <i>underneath</i>
   * the given root: e.g. Paths.get("C:/dir1/"), relative path "dir2" &rarr; "C:/dir1/dir2"!
   *
   * @param path a path
   * @see java.nio.file.Paths#get(String, String...)
   */
  public PathResource(String path) {
    Assert.notNull(path, "Path is required");
    this.path = Paths.get(path).normalize();
  }

  /**
   * Create a new {@code PathResource} from a {@link URI}.
   * <p>Note: Unlike {@link FileSystemResource}, when building relative resources
   * via {@link #createRelative}, the relative path will be built <i>underneath</i>
   * the given root: e.g. Paths.get("C:/dir1/"), relative path "dir2" &rarr; "C:/dir1/dir2"!
   *
   * @param uri a path URI
   * @see java.nio.file.Paths#get(URI)
   */
  public PathResource(URI uri) {
    Assert.notNull(uri, "URI is required");
    this.path = Paths.get(uri).normalize();
  }

  /**
   * Return the file path for this resource.
   */
  public final String getPath() {
    return this.path.toString();
  }

  /**
   * This implementation returns whether the underlying file exists.
   *
   * @see java.nio.file.Files#exists(Path, java.nio.file.LinkOption...)
   */
  @Override
  public boolean exists() {
    return Files.exists(this.path);
  }

  /**
   * This implementation checks whether the underlying file is marked as readable
   * (and corresponds to an actual file with content, not to a directory).
   *
   * @see java.nio.file.Files#isReadable(Path)
   * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
   */
  @Override
  public boolean isReadable() {
    return (Files.isReadable(this.path) && !Files.isDirectory(this.path));
  }

  /**
   * This implementation opens an {@link InputStream} for the underlying file.
   *
   * @see java.nio.file.spi.FileSystemProvider#newInputStream(Path, OpenOption...)
   */
  @Override
  public InputStream getInputStream() throws IOException {
    if (!exists()) {
      throw new FileNotFoundException(getPath() + " (no such file or directory)");
    }
    if (Files.isDirectory(this.path)) {
      throw new FileNotFoundException(getPath() + " (is a directory)");
    }
    return Files.newInputStream(this.path);
  }

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    try {
      return Files.readAllBytes(this.path);
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  @Override
  public String getContentAsString(Charset charset) throws IOException {
    try {
      return Files.readString(this.path, charset);
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  /**
   * This implementation checks whether the underlying file is marked as writable
   * (and corresponds to an actual file with content, not to a directory).
   *
   * @see java.nio.file.Files#isWritable(Path)
   * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
   */
  @Override
  public boolean isWritable() {
    return (Files.isWritable(this.path) && !Files.isDirectory(this.path));
  }

  /**
   * This implementation opens an {@link OutputStream} for the underlying file.
   *
   * @see java.nio.file.spi.FileSystemProvider#newOutputStream(Path, OpenOption...)
   */
  @Override
  public OutputStream getOutputStream() throws IOException {
    if (Files.isDirectory(this.path)) {
      throw new FileNotFoundException(getPath() + " (is a directory)");
    }
    return Files.newOutputStream(this.path);
  }

  /**
   * This implementation returns a {@link URL} for the underlying file.
   *
   * @see java.nio.file.Path#toUri()
   * @see java.net.URI#toURL()
   */
  @Override
  public URL getURL() throws IOException {
    return this.path.toUri().toURL();
  }

  /**
   * This implementation returns a {@link URI} for the underlying file.
   *
   * @see java.nio.file.Path#toUri()
   */
  @Override
  public URI getURI() throws IOException {
    return this.path.toUri();
  }

  /**
   * This implementation always indicates a file.
   */
  @Override
  public boolean isFile() {
    return true;
  }

  /**
   * This implementation returns the underlying {@link File} reference.
   */
  @Override
  public File getFile() throws IOException {
    try {
      return this.path.toFile();
    }
    catch (UnsupportedOperationException ex) {
      // Only paths on the default file system can be converted to a File:
      // Do exception translation for cases where conversion is not possible.
      throw new FileNotFoundException(this.path + " cannot be resolved to absolute file path");
    }
  }

  /**
   * This implementation opens a {@link ReadableByteChannel} for the underlying file.
   *
   * @see Files#newByteChannel(Path, OpenOption...)
   */
  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    try {
      return Files.newByteChannel(this.path, StandardOpenOption.READ);
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  /**
   * This implementation opens a {@link WritableByteChannel} for the underlying file.
   *
   * @see Files#newByteChannel(Path, OpenOption...)
   */
  @Override
  public WritableByteChannel writableChannel() throws IOException {
    return Files.newByteChannel(this.path, StandardOpenOption.WRITE);
  }

  /**
   * This implementation returns the underlying file's length.
   */
  @Override
  public long contentLength() throws IOException {
    return Files.size(this.path);
  }

  /**
   * This implementation returns the underlying file's timestamp.
   *
   * @see java.nio.file.Files#getLastModifiedTime(Path, java.nio.file.LinkOption...)
   */
  @Override
  public long lastModified() throws IOException {
    // We can not use the superclass method since it uses conversion to a File and
    // only a Path on the default file system can be converted to a File...
    return Files.getLastModifiedTime(this.path).toMillis();
  }

  /**
   * This implementation creates a {@link PathResource}, applying the given path
   * relative to the path of the underlying file of this resource descriptor.
   *
   * @see java.nio.file.Path#resolve(String)
   */
  @Override
  public Resource createRelative(String relativePath) {
    return new PathResource(this.path.resolve(relativePath));
  }

  /**
   * This implementation returns the name of the file.
   *
   * @see java.nio.file.Path#getFileName()
   */
  @Override
  public String getName() {
    return this.path.getFileName().toString();
  }

  @Override
  public String toString() {
    return "path [" + this.path.toAbsolutePath() + "]";
  }

  /**
   * This implementation compares the underlying {@link Path} references.
   */
  @Override
  public boolean equals(@Nullable Object obj) {
    return (this == obj || (obj instanceof PathResource that && this.path.equals(that.path)));
  }

  /**
   * This implementation returns the hash code of the underlying Path reference.
   */
  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

}
