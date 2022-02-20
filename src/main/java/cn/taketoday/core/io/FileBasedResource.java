/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 * <p>
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
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
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * {@link Resource} implementation for {@code java.io.File} and
 * {@code java.nio.file.Path} handles with a file system target.
 * Supports resolution as a {@code File} and also as a {@code URL}.
 * Implements the extended {@link WritableResource} interface.
 *
 * <p>Note: this {@link Resource} implementation uses
 * NIO.2 API for read/write interactions. it may be constructed with a
 * {@link java.nio.file.Path} handle in which case it will perform all file system
 * interactions via NIO.2, only resorting to {@link File} on {@link #getFile()}.
 *
 * @author TODAY 2019-05-14 22:50
 * @see java.io.File
 * @see java.nio.file.Files
 * @since 2.1.6
 */
public class FileBasedResource extends AbstractResource implements WritableResource {

  @Nullable
  private final File file;

  private final String path;
  private final Path filePath;

  public FileBasedResource(String path) {
    this.file = new File(this.path = StringUtils.cleanPath(path));
    this.filePath = file.toPath();
  }

  public FileBasedResource(File file) {
    this.file = file;
    this.filePath = file.toPath();
    this.path = StringUtils.cleanPath(file.getPath());
  }

  public FileBasedResource(Path filePath) {
    this.file = null;
    this.filePath = filePath;
    this.path = StringUtils.cleanPath(filePath.toString());
  }

  /**
   * Create a new {@code FileBasedResource} from a {@link FileSystem} handle,
   * locating the specified path.
   * <p>This is an alternative to {@link #FileBasedResource(String)},
   * performing all file system interactions via NIO.2 instead of {@link File}.
   *
   * @param fileSystem the FileSystem to locate the path within
   * @param path a file path
   * @see #FileBasedResource(File)
   * @since 4.0
   */
  public FileBasedResource(FileSystem fileSystem, String path) {
    Assert.notNull(path, "Path must not be null");
    Assert.notNull(fileSystem, "FileSystem must not be null");
    this.file = null;
    this.path = StringUtils.cleanPath(path);
    this.filePath = fileSystem.getPath(this.path).normalize();
  }

  /**
   * Return the file path for this resource.
   */
  public final String getPath() {
    return path;
  }

  /**
   * @since 4.0
   */
  public Path getFilePath() {
    return filePath;
  }

  /**
   * This implementation returns whether the underlying file exists.
   */
  @Override
  public boolean exists() {
    return file != null ? file.exists() : Files.exists(filePath);
  }

  /**
   * This implementation opens a NIO file stream for the underlying file.
   */
  @Override
  public InputStream getInputStream() throws IOException {
    try {
      return Files.newInputStream(filePath);
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  /**
   * This implementation opens a FileOutputStream for the underlying file.
   *
   * @see java.io.FileOutputStream
   */
  @Override
  public OutputStream getOutputStream() throws IOException {
    return Files.newOutputStream(filePath);
  }

  /**
   * This implementation returns a URL for the underlying file.
   */
  @Override
  public URL getLocation() throws IOException {
    return file != null ? file.toURI().toURL() : filePath.toUri().toURL();
  }

  /**
   * Return a URI handle for this resource.
   */
  @Override
  public URI getURI() {
    return file != null ? file.toURI() : filePath.toUri();
  }

  /**
   * This implementation returns the underlying File reference.
   */
  @Override
  public File getFile() {
    return file != null ? file : filePath.toFile();
  }

  /**
   * This implementation always indicates a file.
   */
  @Override
  public boolean isFile() {
    return true;
  }

  /**
   * @throws SecurityException If a security manager exists and its <code>{@link
   * java.lang.SecurityManager#checkRead(java.lang.String)}</code>
   * method denies read access to the file
   */
  @Override
  public boolean isDirectory() throws IOException {
    if (file != null) {
      return file.isDirectory();
    }
    return Files.isDirectory(filePath);
  }

  /**
   * This implementation returns the underlying File/Path length.
   */
  @Override
  public long contentLength() throws IOException {
    if (file != null) {
      long length = file.length();
      if (length == 0L && !file.exists()) {
        throw new FileNotFoundException(getName() + " cannot be resolved its content length");
      }
      return length;
    }
    try {
      return Files.size(filePath);
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  /**
   * This implementation returns the underlying File/Path last-modified time.
   */
  @Override
  public long lastModified() throws IOException {
    if (file != null) {
      return super.lastModified();
    }
    try {
      return Files.getLastModifiedTime(filePath).toMillis();
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    String pathToUse = ResourceUtils.getRelativePath(path, relativePath);
    return file != null
           ? new FileBasedResource(pathToUse)
           : new FileBasedResource(this.filePath.getFileSystem(), pathToUse);
  }

  @Override
  public String[] list() throws IOException {
    if (file != null) {
      return file.list();
    }
    return filePath.toFile().list();
  }

  @Override
  public Resource[] list(ResourceFilter filter) throws IOException {
    String[] names = list();
    if (ObjectUtils.isEmpty(names)) {
      return EMPTY_ARRAY;
    }

    File parent = getFile();
    ArrayList<Resource> resources = new ArrayList<>(names.length);
    for (String name : names) { // this resource is a directory
      FileBasedResource resource = new FileBasedResource(new File(parent, name));
      if (filter == null || filter.accept(resource)) {
        resources.add(resource);
      }
    }
    if (resources.isEmpty()) {
      return EMPTY_ARRAY;
    }
    return resources.toArray(Resource.EMPTY_ARRAY);
  }

  @Override
  public String getName() {
    if (file != null) {
      return file.getName();
    }
    if (filePath != null) {
      Path fileName = filePath.getFileName();
      if (fileName != null) {
        return fileName.toString();
      }
    }
    return new File(path).getName();
  }

  /**
   * This implementation opens a FileChannel for the underlying file.
   *
   * @see java.nio.channels.FileChannel
   */
  @Override
  public ReadableByteChannel readableChannel() throws IOException {
    try {
      return FileChannel.open(filePath, StandardOpenOption.READ);
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  /**
   * This implementation opens a FileChannel for the underlying file.
   *
   * @see java.nio.channels.FileChannel
   */
  @Override
  public WritableByteChannel writableChannel() throws IOException {
    return FileChannel.open(filePath, StandardOpenOption.WRITE);
  }

  /**
   * This implementation checks whether the underlying file is marked as readable
   * (and corresponds to an actual file with content, not to a directory).
   *
   * @see java.io.File#canRead()
   * @see java.io.File#isDirectory()
   */
  @Override
  public boolean isReadable() {
    return (file != null ? file.canRead() && !file.isDirectory() :
            Files.isReadable(filePath) && !Files.isDirectory(filePath));
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof FileBasedResource && path.equals(((FileBasedResource) other).path)));
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return "FileBasedResource: ".concat(path);
  }

}
