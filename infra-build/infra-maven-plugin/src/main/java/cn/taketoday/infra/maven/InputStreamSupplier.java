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

package cn.taketoday.infra.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Supplier to provide an {@link InputStream}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
@FunctionalInterface
interface InputStreamSupplier {

  /**
   * Returns a new open {@link InputStream} at the beginning of the content.
   *
   * @return a new {@link InputStream}
   * @throws IOException on IO error
   */
  InputStream openStream() throws IOException;

  /**
   * Factory method to create an {@link InputStreamSupplier} for the given {@link File}.
   *
   * @param file the source file
   * @return a new {@link InputStreamSupplier} instance
   */
  static InputStreamSupplier forFile(File file) {
    return () -> new FileInputStream(file);
  }

}