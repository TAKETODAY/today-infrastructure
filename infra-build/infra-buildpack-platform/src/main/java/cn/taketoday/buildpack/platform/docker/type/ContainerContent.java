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

package cn.taketoday.buildpack.platform.docker.type;

import cn.taketoday.buildpack.platform.io.TarArchive;

import cn.taketoday.lang.Assert;

/**
 * Additional content that can be written to a created container.
 *
 * @author Phillip Webb
 * @since 4.0
 */
public interface ContainerContent {

  /**
   * Return the actual content to be added.
   *
   * @return the content
   */
  TarArchive getArchive();

  /**
   * Return the destination path where the content should be added.
   *
   * @return the destination path
   */
  String getDestinationPath();

  /**
   * Factory method to create a new {@link ContainerContent} instance written to the
   * root of the container.
   *
   * @param archive the archive to add
   * @return a new {@link ContainerContent} instance
   */
  static ContainerContent of(TarArchive archive) {
    return of(archive, "/");
  }

  /**
   * Factory method to create a new {@link ContainerContent} instance.
   *
   * @param archive the archive to add
   * @param destinationPath the destination path within the container
   * @return a new {@link ContainerContent} instance
   */
  static ContainerContent of(TarArchive archive, String destinationPath) {
    Assert.notNull(archive, "Archive must not be null");
    Assert.hasText(destinationPath, "DestinationPath must not be empty");
    return new ContainerContent() {

      @Override
      public TarArchive getArchive() {
        return archive;
      }

      @Override
      public String getDestinationPath() {
        return destinationPath;
      }

    };
  }

}
