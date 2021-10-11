/**
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
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.util.ObjectUtils;
import cn.taketoday.util.ResourceUtils;
import cn.taketoday.util.StringUtils;

/**
 * @author TODAY <br>
 * 2019-05-14 22:50
 * @since 2.1.6
 */
public class FileBasedResource extends AbstractResource implements WritableResource {

  private final File file;
  private final String path;
  private final Path filePath;

  public FileBasedResource(String path) {
    this.file = new File(this.path = StringUtils.cleanPath(path));
    this.filePath = this.file.toPath();
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
   * Return the file path for this resource.
   */
  public final String getPath() {
    return this.path;
  }

  /**
   * This implementation returns whether the underlying file exists.
   */
  @Override
  public boolean exists() {
    return (this.file != null ? this.file.exists() : Files.exists(this.filePath));
  }

  /**
   * This implementation opens a NIO file stream for the underlying file.
   */
  @Override
  public InputStream getInputStream() throws IOException {
    try {
      return Files.newInputStream(this.filePath);
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
    return Files.newOutputStream(this.filePath);
  }

  /**
   * This implementation returns a URL for the underlying file.
   */
  @Override
  public URL getLocation() throws IOException {
    return (this.file != null ? this.file.toURI().toURL() : this.filePath.toUri().toURL());
  }

  /**
   * This implementation returns the underlying File reference.
   */
  @Override
  public File getFile() {
    return (this.file != null ? this.file : this.filePath.toFile());
  }

  /**
   * This implementation returns the underlying File/Path length.
   */
  @Override
  public long contentLength() throws IOException {
    if (this.file != null) {
      long length = this.file.length();
      if (length == 0L && !this.file.exists()) {
        throw new FileNotFoundException(getName() + " cannot be resolved its content length");
      }
      return length;
    }
    try {
      return Files.size(this.filePath);
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
    if (this.file != null) {
      return super.lastModified();
    }
    try {
      return Files.getLastModifiedTime(this.filePath).toMillis();
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  @Override
  public Resource createRelative(String relativePath) throws IOException {
    final String pathToUse = ResourceUtils.getRelativePath(path, relativePath);
    return (this.file != null
            ? new FileBasedResource(pathToUse)
            : new FileBasedResource(this.filePath.getFileSystem().getPath(pathToUse).normalize()));
  }

  @Override
  public Resource[] list(ResourceFilter filter) throws IOException {

    final String[] names = list();

    if (ObjectUtils.isEmpty(names)) {
      return EMPTY_ARRAY;
    }

    final String path = this.path;
    final List<Resource> resources = new ArrayList<>();
    for (final String name : names) { // this resource is a directory
      final FileBasedResource resource = new FileBasedResource(new File(path, name));
      if (filter == null || filter.accept(resource)) {
        resources.add(resource);
      }
    }
    if (resources.isEmpty()) {
      return EMPTY_ARRAY;
    }
    return resources.toArray(new Resource[resources.size()]);
  }

  @Override
  public String getName() {
    if (file != null) {
      return file.getName();
    }
    if (filePath != null) {
      final Path fileName = filePath.getFileName();
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
      return FileChannel.open(this.filePath, StandardOpenOption.READ);
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
    return FileChannel.open(this.filePath, StandardOpenOption.WRITE);
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
    return (this.file != null ? this.file.canRead() && !this.file.isDirectory() :
            Files.isReadable(this.filePath) && !Files.isDirectory(this.filePath));
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof FileBasedResource && this.path.equals(((FileBasedResource) other).path)));
  }

  @Override
  public int hashCode() {
    return this.path.hashCode();
  }

  @Override
  public String toString() {
    return "FileBasedResource: ".concat(path);
  }

}
