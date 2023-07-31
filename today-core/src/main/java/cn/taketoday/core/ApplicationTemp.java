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

package cn.taketoday.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.TodayStrategies;
import cn.taketoday.util.StringUtils;

/**
 * Provides access to an application specific temporary directory. Generally speaking
 * different Framework applications will get different locations, however, simply
 * restarting an application will give the same location.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/20 23:28
 */
public class ApplicationTemp {
  private static final String TEMP_SUB_DIR = TodayStrategies.getProperty(
          "app.temp-prefix", ApplicationTemp.class.getName());

  public static final ApplicationTemp instance = new ApplicationTemp();

  @Nullable
  private final Class<?> sourceClass;

  @Nullable
  private volatile Path path;

  /**
   * Create a new {@link ApplicationTemp} instance.
   */
  public ApplicationTemp() {
    this(null);
  }

  /**
   * Create a new {@link ApplicationTemp} instance for the specified source class.
   *
   * @param sourceClass the source class or {@code null}
   */
  public ApplicationTemp(@Nullable Class<?> sourceClass) {
    this.sourceClass = sourceClass;
  }

  @Override
  public String toString() {
    return getDir().getAbsolutePath();
  }

  /**
   * Return the directory to be used for application specific temp files.
   *
   * @return the application temp directory
   */
  public File getDir() {
    return getPath().toFile();
  }

  /**
   * Return a sub-directory of the application temp.
   *
   * @param subDir the sub-directory name
   * @return a sub-directory
   */
  public File getDir(String subDir) {
    return createDirectory(getPath().resolve(subDir)).toFile();
  }

  private Path getPath() {
    Path path = this.path;
    if (path == null) {
      synchronized(this) {
        path = this.path;
        if (path == null) {
          String tempSubDir = getTempSubDir(sourceClass);
          path = createDirectory(getTempDirectory().resolve(tempSubDir));
          this.path = path;
        }
      }
    }
    return path;
  }

  private static String getTempSubDir(@Nullable Class<?> sourceClass) {
    if (sourceClass != null) {
      return sourceClass.getName();
    }
    return TEMP_SUB_DIR;
  }

  private Path createDirectory(Path path) {
    try {
      if (!Files.exists(path)) {
        Files.createDirectory(path, getFileAttributes(path.getFileSystem()));
      }
      return path;
    }
    catch (IOException ex) {
      throw new IllegalStateException("Unable to create application temp directory " + path, ex);
    }
  }

  private FileAttribute<?>[] getFileAttributes(FileSystem fileSystem) {
    if (!fileSystem.supportedFileAttributeViews().contains("posix")) {
      return new FileAttribute<?>[0];
    }
    return new FileAttribute<?>[] {
            PosixFilePermissions.asFileAttribute(Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ))
    };
  }

  private Path getTempDirectory() {
    String property = System.getProperty("java.io.tmpdir");
    Assert.state(StringUtils.isNotEmpty(property), "No 'java.io.tmpdir' property set");
    Path tempDirectory = Paths.get(property);

    if (!Files.exists(tempDirectory)) {
      throw new IllegalStateException("Temp directory '" + tempDirectory + "' does not exist");
    }

    if (!Files.isDirectory(tempDirectory)) {
      throw new IllegalStateException("Temp location '" + tempDirectory + "' is not a directory");
    }
    return tempDirectory;
  }

  // Static

  /**
   * Using default instance to create temp directory
   */
  public static File createDirectory(String subDir) {
    return instance.getDir(subDir);
  }

}
