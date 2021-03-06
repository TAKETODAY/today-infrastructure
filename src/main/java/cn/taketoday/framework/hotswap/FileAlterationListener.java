/*
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2020 All Rights Reserved.
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

package cn.taketoday.framework.hotswap;

import java.io.File;

/**
 * A listener that receives events of file system modifications.
 * <p>
 * Register {@link FileAlterationListener}s with a {@link FileAlterationObserver}.
 *
 * @author TODAY 2021/2/18 11:46
 * @see FileAlterationObserver
 */
public interface FileAlterationListener {

  /**
   * File system observer started checking event.
   *
   * @param observer
   *         The file system observer
   */
  default void onStart(final FileAlterationObserver observer) { }

  /**
   * Directory created Event.
   *
   * @param directory
   *         The directory created
   */
  default void onDirectoryCreate(final File directory) { }

  /**
   * Directory changed Event.
   *
   * @param directory
   *         The directory changed
   */
  default void onDirectoryChange(final File directory) { }

  /**
   * Directory deleted Event.
   *
   * @param directory
   *         The directory deleted
   */
  default void onDirectoryDelete(final File directory) { }

  /**
   * File created Event.
   *
   * @param file
   *         The file created
   */
  default void onFileCreate(final File file) { }

  /**
   * File changed Event.
   *
   * @param file
   *         The file changed
   */
  default void onFileChange(final File file) { }

  /**
   * File deleted Event.
   *
   * @param file
   *         The file deleted
   */
  default void onFileDelete(final File file) { }

  /**
   * File system observer finished checking event.
   *
   * @param observer
   *         The file system observer
   */
  default void onStop(final FileAlterationObserver observer) { }
}
