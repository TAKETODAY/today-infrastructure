/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework;

import java.io.File;

import cn.taketoday.lang.Nullable;
import cn.taketoday.util.StringUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/1/27 15:51
 */
public class ApplicationUtils {

  public static File getTemporalDirectory(@Nullable Class<?> startupClass, @Nullable String subDir) {
    if (StringUtils.isEmpty(subDir)) {
      return getBaseTemporalDirectory(startupClass);
    }
    File dir = new File(getBaseTemporalDirectory(startupClass), subDir);
    dir.mkdirs();
    return dir;
  }

  /**
   * Return the directory to be used for application specific temp files.
   *
   * @return the application temp directory
   */
  public static File getBaseTemporalDirectory(@Nullable Class<?> startupClass) {
    String property = System.getProperty("java.io.tmpdir");
    if (StringUtils.isEmpty(property)) {
      throw new IllegalStateException("There is no 'java.io.tmpdir' property set");
    }

    File baseTempDir = new File(property);
    if (!baseTempDir.exists()) {
      throw new IllegalStateException("Temp directory " + baseTempDir + " does not exist");
    }
    if (!baseTempDir.isDirectory()) {
      throw new IllegalStateException("Temp location " + baseTempDir + " is not a directory");
    }
    String tempSubDir;
    if (startupClass != null) {
      tempSubDir = startupClass.getName();
    }
    else {
      tempSubDir = "today-" + System.currentTimeMillis();
    }

    File directory = new File(baseTempDir, tempSubDir);
    if (!directory.exists()) {
      directory.mkdirs();
      if (!directory.exists()) {
        throw new IllegalStateException("Unable to create temp directory " + directory);
      }
    }
    return directory;
  }

}
