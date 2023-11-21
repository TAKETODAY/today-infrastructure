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
package cn.taketoday.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
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
public class FileSystemResource extends AbstractResource implements WritableResource {

  @Nullable
  private final File file;

  private final String path;
  private final Path filePath;

  /**
   * Create a new {@code FileSystemResource} from a file path.
   * <p>Note: When building relative resources via {@link #createRelative},
   * it makes a difference whether the specified resource base path here
   * ends with a slash or not. In the case of "C:/dir1/", relative paths
   * will be built underneath that root: e.g. relative path "dir2" &rarr;
   * "C:/dir1/dir2". In the case of "C:/dir1", relative paths will apply
   * at the same directory level: relative path "dir2" &rarr; "C:/dir2".
   *
   * @param path a file path
   * @see #FileSystemResource(Path)
   */
  public FileSystemResource(String path) {
    Assert.notNull(path, "Path is required");
    this.path = StringUtils.cleanPath(path);
    this.file = new File(path);
    this.filePath = this.file.toPath();
  }

  /**
   * Create a new {@code FileSystemResource} from a {@link File} handle.
   * <p>Note: When building relative resources via {@link #createRelative},
   * the relative path will apply <i>at the same directory level</i>:
   * e.g. new File("C:/dir1"), relative path "dir2" &rarr; "C:/dir2"!
   * If you prefer to have relative paths built underneath the given root directory,
   * use the {@link #FileSystemResource(String) constructor with a file path}
   * to append a trailing slash to the root path: "C:/dir1/", which indicates
   * this directory as root for all relative paths.
   *
   * @param file a File handle
   * @see #FileSystemResource(Path)
   * @see #getFile()
   */
  public FileSystemResource(File file) {
    this.file = file;
    this.filePath = file.toPath();
    this.path = StringUtils.cleanPath(file.getPath());
  }

  /**
   * Create a new {@code FileSystemResource} from a {@link Path} handle,
   * performing all file system interactions via NIO.2 instead of {@link File}.
   * <p>In contrast to {@link PathResource}, this variant strictly follows the
   * general {@link FileSystemResource} conventions, in particular in terms of
   * path cleaning and {@link #createRelative(String)} handling.
   * <p>Note: When building relative resources via {@link #createRelative},
   * the relative path will apply <i>at the same directory level</i>:
   * e.g. Paths.get("C:/dir1"), relative path "dir2" &rarr; "C:/dir2"!
   * If you prefer to have relative paths built underneath the given root directory,
   * use the {@link #FileSystemResource(String) constructor with a file path}
   * to append a trailing slash to the root path: "C:/dir1/", which indicates
   * this directory as root for all relative paths. Alternatively, consider
   * using {@link PathResource#PathResource(Path)} for {@code java.nio.path.Path}
   * resolution in {@code createRelative}, always nesting relative paths.
   *
   * @param filePath a Path handle to a file
   * @see #FileSystemResource(File)
   */
  public FileSystemResource(Path filePath) {
    this.file = null;
    this.filePath = filePath;
    this.path = StringUtils.cleanPath(filePath.toString());
  }

  /**
   * Create a new {@code FileSystemResource} from a {@link FileSystem} handle,
   * locating the specified path.
   * <p>This is an alternative to {@link #FileSystemResource(String)},
   * performing all file system interactions via NIO.2 instead of {@link File}.
   *
   * @param fileSystem the FileSystem to locate the path within
   * @param path a file path
   * @see #FileSystemResource(File)
   * @since 4.0
   */
  public FileSystemResource(FileSystem fileSystem, String path) {
    Assert.notNull(path, "Path is required");
    Assert.notNull(fileSystem, "FileSystem is required");
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
   *
   * @see java.io.File#exists()
   * @see java.nio.file.Files#exists(Path, java.nio.file.LinkOption...)
   */
  @Override
  public boolean exists() {
    return file != null ? file.exists() : Files.exists(filePath);
  }

  /**
   * This implementation opens an NIO file stream for the underlying file.
   *
   * @see java.nio.file.Files#newInputStream(Path, java.nio.file.OpenOption...)
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

  @Override
  public byte[] getContentAsByteArray() throws IOException {
    try {
      return Files.readAllBytes(this.filePath);
    }
    catch (NoSuchFileException ex) {
      throw new FileNotFoundException(ex.getMessage());
    }
  }

  @Override
  public String getContentAsString(Charset charset) throws IOException {
    try {
      return Files.readString(this.filePath, charset);
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
  public URL getURL() throws IOException {
    return file != null ? file.toURI().toURL() : filePath.toUri().toURL();
  }

  /**
   * Return a URI handle for this resource.
   */
  @Override
  public URI getURI() throws IOException {
    if (this.file != null) {
      return this.file.toURI();
    }
    else {
      URI uri = this.filePath.toUri();
      // Normalize URI? See https://github.com/spring-projects/spring-framework/issues/29275
      String scheme = uri.getScheme();
      if (ResourceUtils.URL_PROTOCOL_FILE.equals(scheme)) {
        try {
          uri = new URI(scheme, uri.getPath(), null);
        }
        catch (URISyntaxException ex) {
          throw new IOException("Failed to normalize URI: " + uri, ex);
        }
      }
      return uri;
    }
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
    String pathToUse = StringUtils.applyRelativePath(path, relativePath);
    return file != null
           ? new FileSystemResource(pathToUse)
           : new FileSystemResource(this.filePath.getFileSystem(), pathToUse);
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
      FileSystemResource resource = new FileSystemResource(new File(parent, name));
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
   * @see java.nio.file.Files#isReadable(Path)
   * @see java.nio.file.Files#isDirectory(Path, java.nio.file.LinkOption...)
   */
  @Override
  public boolean isReadable() {
    return (file != null ? file.canRead() && !file.isDirectory() :
            Files.isReadable(filePath) && !Files.isDirectory(filePath));
  }

  @Override
  public boolean equals(Object other) {
    return (this == other || (other instanceof FileSystemResource && path.equals(((FileSystemResource) other).path)));
  }

  @Override
  public int hashCode() {
    return path.hashCode();
  }

  @Override
  public String toString() {
    return "FileSystemResource: ".concat(path);
  }

}
