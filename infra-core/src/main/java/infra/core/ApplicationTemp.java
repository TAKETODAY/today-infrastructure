/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.core;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import infra.lang.Assert;
import infra.lang.TodayStrategies;
import infra.util.StringUtils;

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
          "infra.app.sub-temp-dir", ApplicationTemp.class.getName());

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
    return getDir().toString();
  }

  /**
   * Return the directory to be used for application specific temp files.
   *
   * @return the application temp directory
   * @throws UncheckedIOException failed to create base temp dir
   */
  public Path getDir() throws UncheckedIOException {
    return getPath();
  }

  /**
   * Return a sub-directory of the application temp.
   *
   * @param subDir the sub-directory name
   * @return a sub-directory
   * @throws UncheckedIOException failed to create subdir
   */
  public Path getDir(@Nullable String subDir) throws UncheckedIOException {
    if (subDir != null) {
      return createDirectory(getPath().resolve(subDir));
    }
    return getPath();
  }

  /**
   * Creates a new empty file in the specified directory, using the given
   * prefix and suffix strings to generate its name. The resulting
   * {@code Path} is associated with the same {@code FileSystem} as the given
   * directory.
   *
   * <p> The details as to how the name of the file is constructed is
   * implementation dependent and therefore not specified. Where possible
   * the {@code prefix} and {@code suffix} are used to construct candidate
   * names in the same manner as the {@link
   * java.io.File#createTempFile(String, String, File)} method.
   *
   * <p> As with the {@code File.createTempFile} methods, this method is only
   * part of a temporary-file facility. Where used as a <em>work files</em>,
   * the resulting file may be opened using the {@link
   * StandardOpenOption#DELETE_ON_CLOSE DELETE_ON_CLOSE} option so that the
   * file is deleted when the appropriate {@code close} method is invoked.
   * Alternatively, a {@link Runtime#addShutdownHook shutdown-hook}, or the
   * {@link java.io.File#deleteOnExit} mechanism may be used to delete the
   * file automatically.
   *
   * <p> The {@code attrs} parameter is optional {@link FileAttribute
   * file-attributes} to set atomically when creating the file. Each attribute
   * is identified by its {@link FileAttribute#name name}. If more than one
   * attribute of the same name is included in the array then all but the last
   * occurrence is ignored. When no file attributes are specified, then the
   * resulting file may have more restrictive access permissions to files
   * created by the {@link java.io.File#createTempFile(String, String, File)}
   * method.
   *
   * @param subDir the path to directory in which to create the file
   * @param prefix the prefix string to be used in generating the file's name;
   * may be {@code null}
   * @return the path to the newly created file that did not exist before
   * this method was invoked
   * @throws IllegalArgumentException if the prefix or suffix parameters cannot be used to generate
   * a candidate file name
   * @throws UncheckedIOException if an I/O error occurs or {@code dir} does not exist
   */
  public Path createFile(@Nullable String subDir, @Nullable String prefix, FileAttribute<?>... attrs) {
    return createFile(subDir, prefix, null, attrs);
  }

  /**
   * Creates a new empty file in the specified directory, using the given
   * prefix and suffix strings to generate its name. The resulting
   * {@code Path} is associated with the same {@code FileSystem} as the given
   * directory.
   *
   * <p> The details as to how the name of the file is constructed is
   * implementation dependent and therefore not specified. Where possible
   * the {@code prefix} and {@code suffix} are used to construct candidate
   * names in the same manner as the {@link
   * java.io.File#createTempFile(String, String, File)} method.
   *
   * <p> As with the {@code File.createTempFile} methods, this method is only
   * part of a temporary-file facility. Where used as a <em>work files</em>,
   * the resulting file may be opened using the {@link
   * StandardOpenOption#DELETE_ON_CLOSE DELETE_ON_CLOSE} option so that the
   * file is deleted when the appropriate {@code close} method is invoked.
   * Alternatively, a {@link Runtime#addShutdownHook shutdown-hook}, or the
   * {@link java.io.File#deleteOnExit} mechanism may be used to delete the
   * file automatically.
   *
   * <p> The {@code attrs} parameter is optional {@link FileAttribute
   * file-attributes} to set atomically when creating the file. Each attribute
   * is identified by its {@link FileAttribute#name name}. If more than one
   * attribute of the same name is included in the array then all but the last
   * occurrence is ignored. When no file attributes are specified, then the
   * resulting file may have more restrictive access permissions to files
   * created by the {@link java.io.File#createTempFile(String, String, File)}
   * method.
   *
   * @param subDir the path to directory in which to create the file
   * @param prefix the prefix string to be used in generating the file's name;
   * may be {@code null}
   * @param suffix the suffix string to be used in generating the file's name;
   * may be {@code null}, in which case "{@code .tmp}" is used
   * @return the path to the newly created file that did not exist before
   * this method was invoked
   * @throws IllegalArgumentException if the prefix or suffix parameters cannot be used to generate
   * a candidate file name
   * @throws UncheckedIOException if an I/O error occurs or {@code dir} does not exist
   */
  public Path createFile(@Nullable String subDir, @Nullable String prefix, @Nullable String suffix, FileAttribute<?>... attrs) {
    try {
      return Files.createTempFile(getDir(subDir), prefix, suffix, attrs);
    }
    catch (IOException e) {
      throw new UncheckedIOException("Files.createTempFile IO error", e);
    }
  }

  private Path getPath() throws UncheckedIOException {
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

  /**
   * @throws UncheckedIOException failed to create temp dir
   */
  private Path createDirectory(Path path) throws UncheckedIOException {
    try {
      if (Files.notExists(path)) {
        Files.createDirectories(path, getFileAttributes(path.getFileSystem()));
      }
      return path;
    }
    catch (IOException ex) {
      throw new UncheckedIOException("Unable to create application temp directory " + path, ex);
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

    if (Files.notExists(tempDirectory)) {
      throw new IllegalStateException("Temp directory '%s' does not exist".formatted(tempDirectory));
    }

    if (!Files.isDirectory(tempDirectory)) {
      throw new IllegalStateException("Temp location '%s' is not a directory".formatted(tempDirectory));
    }
    return tempDirectory;
  }

  // Static

  /**
   * Using default instance to create temp directory
   */
  public static Path createDirectory(String subDir) {
    return instance.getDir(subDir);
  }

  /**
   * Using default instance to create temp file
   */
  public static Path createFile(String prefix) {
    return instance.createFile(null, prefix);
  }

}
