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

package cn.taketoday.buildpack.platform.io;

import java.io.IOException;

/**
 * Interface that can be used to write a file/directory layout.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 4.0
 */
public interface Layout {

  /**
   * Add a directory to the content.
   *
   * @param name the full name of the directory to add
   * @param owner the owner of the directory
   * @throws IOException on IO error
   */
  default void directory(String name, Owner owner) throws IOException {
    directory(name, owner, 0755);
  }

  /**
   * Add a directory to the content.
   *
   * @param name the full name of the directory to add
   * @param owner the owner of the directory
   * @param mode the permissions for the file
   * @throws IOException on IO error
   */
  void directory(String name, Owner owner, int mode) throws IOException;

  /**
   * Write a file to the content.
   *
   * @param name the full name of the file to add
   * @param owner the owner of the file
   * @param content the content to add
   * @throws IOException on IO error
   */
  default void file(String name, Owner owner, Content content) throws IOException {
    file(name, owner, 0644, content);
  }

  /**
   * Write a file to the content.
   *
   * @param name the full name of the file to add
   * @param owner the owner of the file
   * @param mode the permissions for the file
   * @param content the content to add
   * @throws IOException on IO error
   */
  void file(String name, Owner owner, int mode, Content content) throws IOException;

}
