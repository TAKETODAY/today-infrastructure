/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.core.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * Interface for a resource descriptor that abstracts from the actual type of
 * underlying resource, such as a file or class path resource.
 *
 * <p>
 * An InputStream can be opened for every resource if it exists in physical
 * form, but a URL or File handle can just be returned for certain resources.
 * The actual behavior is implementation-specific.
 *
 * @author TODAY <br>
 * 2019-05-14 19:55
 * @since 2.1.6
 */
public interface Resource extends Readable {
  Resource[] EMPTY_ARRAY = {};

  /**
   * Get the name of the resource.
   *
   * @return name
   */
  String getName();

  /**
   * Get content length
   *
   * @return content length
   */
  long contentLength() throws IOException;

  /**
   * Get last modified
   *
   * @return last modified
   */
  long lastModified() throws IOException;

  /**
   * Get location of this resource.
   *
   * @throws IOException
   *         if the resource is not available
   */
  URL getLocation() throws IOException;

  /**
   * Return a URI handle for this resource.
   *
   * @throws IOException
   *         if the resource cannot be resolved as URI, i.e. if the resource
   *         is not available as descriptor
   * @since 2.1.7
   */
  URI getURI() throws IOException;

  /**
   * Return a File handle for this resource.
   *
   * @throws IOException
   *         in case of general resolution/reading failures
   */
  File getFile() throws IOException;

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
   * since it strictly implies {@link #exists()} semantics as of 5.1.
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
   * Is a directory?
   *
   * @throws IOException
   */
  boolean isDirectory() throws IOException;

  /**
   * list {@link Resource} under the directory
   *
   * @return {@link Resource} names
   *
   * @throws IOException
   *         if the resource is not available
   */
  String[] list() throws IOException;

  /**
   * list {@link Resource} under the directory
   *
   * @param filter
   *         filter {@link Resource}
   *
   * @return {@link Resource} names
   *
   * @throws IOException
   *         if the resource is not available
   */
  Resource[] list(ResourceFilter filter) throws IOException;

  /**
   * Create a resource relative to this resource.
   *
   * @param relativePath
   *         the relative path (relative to this resource)
   *
   * @return the resource handle for the relative resource
   *
   * @throws IOException
   *         if the relative resource cannot be determined
   */
  Resource createRelative(String relativePath) throws IOException;

}
