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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import cn.taketoday.lang.Experimental;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.FileCopyUtils;

/**
 * Interface for a resource descriptor that abstracts from the actual
 * type of underlying resource, such as a file or class path resource.
 *
 * <p>An InputStream can be opened for every resource if it exists in
 * physical form, but a URL or File handle can just be returned for
 * certain resources. The actual behavior is implementation-specific.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #getInputStream()
 * @see #getURL()
 * @see #getURI()
 * @see #getFile()
 * @see WritableResource
 * @see ContextResource
 * @see UrlResource
 * @see FileUrlResource
 * @see FileSystemResource
 * @see ClassPathResource
 * @see ByteArrayResource
 * @see InputStreamResource
 * @since 2.1.6 2019-05-14 19:55
 */
public interface Resource extends InputStreamSource {
  Resource[] EMPTY_ARRAY = {};

  /**
   * Determine a name for this resource, i.e. typically the last
   * part of the path: for example, "myfile.txt".
   * <p>Returns {@code null} if this type of resource does not
   * have a filename.
   */
  @Nullable
  String getName();

  /**
   * Determine the content length for this resource.
   *
   * @throws IOException if the resource cannot be resolved
   * (in the file system or as some other known physical resource type)
   */
  long contentLength() throws IOException;

  /**
   * Determine the last-modified timestamp for this resource.
   *
   * @throws IOException if the resource cannot be resolved
   * (in the file system or as some other known physical resource type)
   */
  long lastModified() throws IOException;

  /**
   * Return a URL handle for this resource.
   *
   * @throws IOException if the resource cannot be resolved as URL,
   * i.e. if the resource is not available as a descriptor
   */
  URL getURL() throws IOException;

  /**
   * Return a URI handle for this resource.
   *
   * @throws IOException if the resource cannot be resolved as URI,
   * i.e. if the resource is not available as a descriptor
   * @since 2.1.7
   */
  URI getURI() throws IOException;

  /**
   * Return a File handle for this resource.
   *
   * @throws java.io.FileNotFoundException if the resource cannot be resolved as
   * absolute file path, i.e. if the resource is not available in a file system
   * @throws IOException in case of general resolution/reading failures
   * @see #getInputStream()
   */
  File getFile() throws IOException;

  /**
   * Return the contents of this resource as a byte array.
   *
   * @return the contents of this resource as byte array
   * @throws java.io.FileNotFoundException if the resource cannot be resolved as
   * absolute file path, i.e. if the resource is not available in a file system
   * @throws IOException in case of general resolution/reading failures
   * @since 4.0
   */
  default byte[] getContentAsByteArray() throws IOException {
    return FileCopyUtils.copyToByteArray(getInputStream());
  }

  /**
   * Returns the contents of this resource as a string, using the specified
   * charset.
   *
   * @param charset the charset to use for decoding
   * @return the contents of this resource as a {@code String}
   * @throws java.io.FileNotFoundException if the resource cannot be resolved as
   * absolute file path, i.e. if the resource is not available in a file system
   * @throws IOException in case of general resolution/reading failures
   * @since 4.0
   */
  default String getContentAsString(Charset charset) throws IOException {
    return FileCopyUtils.copyToString(new InputStreamReader(getInputStream(), charset));
  }

  /**
   * Determine whether this resource actually exists in physical form.
   * <p>
   * This method performs a definitive existence check, whereas the existence of a
   * {@code Resource} handle only guarantees a valid descriptor handle.
   */
  boolean exists();

  /**
   * Indicate whether non-empty contents of this resource can be read via
   * {@link #getInputStream()}.
   * <p>Will be {@code true} for typical resource descriptors that exist
   * since it strictly implies {@link #exists()} semantics.
   * Note that actual content reading may still fail when attempted.
   * However, a value of {@code false} is a definitive indication
   * that the resource content cannot be read.
   *
   * @see #getInputStream()
   * @see #exists()
   */
  default boolean isReadable() {
    return exists();
  }

  /**
   * Indicate whether this resource represents a handle with an open stream.
   * If {@code true}, the InputStream cannot be read multiple times,
   * and must be read and closed to avoid resource leaks.
   * <p>Will be {@code false} for typical resource descriptors.
   */
  default boolean isOpen() {
    return false;
  }

  /**
   * Determine whether this resource represents a file in a file system.
   * <p>A value of {@code true} strongly suggests (but does not guarantee)
   * that a {@link #getFile()} call will succeed.
   * <p>This is conservatively {@code false} by default.
   *
   * @see #getFile()
   * @since 4.0
   */
  default boolean isFile() {
    return false;
  }

  /**
   * Tests whether the resource denoted by this abstract pathname is a
   * directory.
   *
   * <p> Where it is required to distinguish an I/O exception from the case
   * that the file is not a directory, or where several attributes of the
   * same file are required at the same time, then the {@link
   * java.nio.file.Files#readAttributes(Path, Class, LinkOption[])
   * Files.readAttributes} method may be used.
   *
   * @return <code>true</code> if and only if the file denoted by this
   * abstract pathname exists <em>and</em> is a directory;
   * <code>false</code> otherwise
   * @throws IOException cannot determine resource
   */
  boolean isDirectory() throws IOException;

  /**
   * list {@link Resource} under the directory
   *
   * @return {@link Resource} names
   * @throws IOException if the resource is not available
   */
  @Experimental
  String[] list() throws IOException;

  /**
   * list {@link Resource} under the directory
   *
   * @param filter filter {@link Resource}
   * @return {@link Resource} names
   * @throws IOException if the resource is not available
   */
  @Experimental
  Resource[] list(@Nullable ResourceFilter filter) throws IOException;

  /**
   * Create a resource relative to this resource.
   *
   * @param relativePath the relative path (relative to this resource)
   * @return the resource handle for the relative resource
   * @throws IOException if the relative resource cannot be determined
   */
  Resource createRelative(String relativePath) throws IOException;

  /**
   * Return a description for this resource,
   * to be used for error output when working with the resource.
   * <p>Implementations are also encouraged to return this value
   * from their {@code toString} method.
   *
   * @see Object#toString()
   */
  @Override
  String toString();

}
