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

package cn.taketoday.app.loader.tools;

import java.io.IOException;
import java.io.InputStream;

/**
 * Writer used by {@link CustomLoaderLayout CustomLoaderLayouts} to write classes into a
 * repackaged JAR.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public interface LoaderClassesWriter {

  /**
   * Write the default required infra-app-loader classes to the JAR.
   *
   * @throws IOException if the classes cannot be written
   */
  void writeLoaderClasses() throws IOException;

  /**
   * Write custom required infra-app-loader classes to the JAR.
   *
   * @param loaderJarResourceName the name of the resource containing the loader classes
   * to be written
   * @throws IOException if the classes cannot be written
   */
  void writeLoaderClasses(String loaderJarResourceName) throws IOException;

  /**
   * Write a single entry to the JAR.
   *
   * @param name the name of the entry
   * @param inputStream the input stream content
   * @throws IOException if the entry cannot be written
   */
  void writeEntry(String name, InputStream inputStream) throws IOException;

}
