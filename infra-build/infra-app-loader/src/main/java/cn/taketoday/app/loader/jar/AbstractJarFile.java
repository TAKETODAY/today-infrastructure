/*
 * Copyright 2012 - 2023 the original author or authors.
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

package cn.taketoday.app.loader.jar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;

/**
 * Base class for extended variants of {@link java.util.jar.JarFile}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
abstract class AbstractJarFile extends java.util.jar.JarFile {

  /**
   * Create a new {@link AbstractJarFile}.
   *
   * @param file the root jar file.
   * @throws IOException on IO error
   */
  AbstractJarFile(File file) throws IOException {
    super(file);
  }

  /**
   * Return a URL that can be used to access this JAR file. NOTE: the specified URL
   * cannot be serialized and or cloned.
   *
   * @return the URL
   * @throws MalformedURLException if the URL is malformed
   */
  abstract URL getUrl() throws MalformedURLException;

  /**
   * Return the {@link JarFileType} of this instance.
   *
   * @return the jar file type
   */
  abstract JarFileType getType();

  /**
   * Return the security permission for this JAR.
   *
   * @return the security permission.
   */
  abstract Permission getPermission();

  /**
   * Return an {@link InputStream} for the entire jar contents.
   *
   * @return the contents input stream
   * @throws IOException on IO error
   */
  abstract InputStream getInputStream() throws IOException;

  /**
   * The type of a {@link JarFile}.
   */
  enum JarFileType {

    DIRECT, NESTED_DIRECTORY, NESTED_JAR

  }

}
